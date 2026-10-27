package com.standby.clock

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Watches the accelerometer and opens StandBy when the phone rests in its stand.
 *
 * Reliability measures:
 *  - launch the screen directly; if Android blocks it, fall back to a full-screen notification
 *  - warn with a notification when a permission has been lost
 *  - screen off: uses the low-power "wake-up" accelerometer in batches (no wake lock) when the phone has one
 *  - pauses on battery saver when not charging
 *  - by default only opens while the phone is locked, so it never covers an app or video
 */
class OrientationService : Service(), SensorEventListener {

    private lateinit var sm: SensorManager
    private lateinit var pm: PowerManager
    private lateinit var bm: BatteryManager
    private lateinit var km: KeyguardManager
    private val handler = Handler(Looper.getMainLooper())

    private val gravity = FloatArray(3)
    private var firstEvent = true
    private var landscapeSince = 0L
    private var notLandscapeSince = 0L
    private var armed = true
    private var mode = 0                 // 0 off, 1 normal, 2 wake-up batched, 3 normal + wake lock
    private var overlay: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var fallback: Runnable? = null

    // cached settings (re-read in onStartCommand)
    private var calib: FloatArray? = null
    private var sens = 1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { updateSensor() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        running = true
        startAsForeground()
        addOverlay()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        loadSettings()
        updateSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        loadSettings()
        addOverlay()
        mode = -1            // force re-registration
        updateSensor()
        if (!Settings.canDrawOverlays(this)) notifyAttention()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        mode = -1
        sm.unregisterListener(this)
        syncWakeLock(false)
        try { unregisterReceiver(receiver) } catch (e: IllegalArgumentException) { }
        removeOverlay()
        super.onDestroy()
    }

    private fun loadSettings() {
        calib = Prefs.calibration(this)
        sens = Prefs.sensitivity(this).coerceIn(0, 2)
    }

    /* ---------------- sensor control ---------------- */

    private fun updateSensor() {
        val screenOn = pm.isInteractive
        val charging = bm.isCharging
        val allowed = Prefs.autoEnabled(this) && (!Prefs.needCharging(this) || charging) && !(pm.isPowerSaveMode && !charging)
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val accWake = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
        val want = when {
            !allowed || acc == null -> 0
            screenOn -> 1
            accWake != null -> 2           // low power: events arrive in 2 s batches, CPU can sleep
            else -> 3                      // keep the CPU awake with partial wake-lock when screen is off
        }
        if (want != mode) {
            sm.unregisterListener(this)
            firstEvent = true
            landscapeSince = 0L
            notLandscapeSince = 0L
            when (want) {
                1, 3 -> sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
                2 -> sm.registerListener(this, accWake, SensorManager.SENSOR_DELAY_NORMAL, 2_000_000)
            }
            mode = want
        }
        syncWakeLock(want == 3)
    }

    private fun syncWakeLock(need: Boolean) {
        if (need && wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "standbyclock:watch").apply {
                setReferenceCounted(false)
                acquire()
            }
        } else if (!need) {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        }
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(e: SensorEvent) {
        if (firstEvent) {
            for (k in 0..2) gravity[k] = e.values[k]
            firstEvent = false
            return
        }
        var motion = 0f
        for (k in 0..2) {
            gravity[k] = 0.8f * gravity[k] + 0.2f * e.values[k]
            motion += abs(e.values[k] - gravity[k])
        }
        val now = e.timestamp / 1_000_000L          // event time in ms (works with batched delivery)
        val inPose = Orientation.isLandscapePose(gravity, calib, sens)

        if (inPose) {
            notLandscapeSince = 0L
            if (motion > Orientation.MAX_MOTION[sens] || landscapeSince == 0L) landscapeSince = now
            // while unlocked (and allowed) wait twice as long: you are probably using the phone
            val unlocked = pm.isInteractive && !km.isKeyguardLocked
            val hold = Orientation.HOLD_MS[sens] * (if (unlocked) 2 else 1)
            if (armed && now - landscapeSince >= hold && shouldAct(unlocked)) {
                armed = false
                launchStandby()
            }
        } else {
            landscapeSince = 0L
            if (notLandscapeSince == 0L) notLandscapeSince = now
            if (now - notLandscapeSince >= LEAVE_MS) {
                armed = true
                StandbyActivity.finishIfAuto()
                NotificationManagerCompat.from(this).cancel(ID_OPEN)
            }
        }
    }

    private fun shouldAct(unlocked: Boolean): Boolean =
        Prefs.autoEnabled(this) &&
            (!Prefs.needCharging(this) || bm.isCharging) &&
            (!unlocked || Prefs.unlockedOk(this)) &&
            !StandbyActivity.isOpen

    /* ---------------- launching ---------------- */

    private fun launchStandby() {
        try {
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "standbyclock:wakeup"
            )
            wl.acquire(3000L)
        } catch (e: Exception) { }

        val i = Intent(this, StandbyActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("auto", true)

        val isLocked = km.isKeyguardLocked || !pm.isInteractive
        if (isLocked) {
            openViaNotification()
        }

        var ok = true
        try { startActivity(i) } catch (e: Exception) { ok = false }
        fallback?.let { handler.removeCallbacks(it) }
        val r = Runnable { if (!StandbyActivity.isOpen) openViaNotification() }
        fallback = r
        handler.postDelayed(r, if (ok) 1000L else 0L)
    }

    private fun channel(id: String, name: String, importance: Int) {
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(id, name, importance))
    }

    private fun openViaNotification() {
        channel("open", "Open StandBy", NotificationManager.IMPORTANCE_HIGH)
        val pi = PendingIntent.getActivity(
            this, 2, Intent(this, StandbyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("auto", true),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(this, "open")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Open StandBy clock")
            .setContentText("Tap to open")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        try { NotificationManagerCompat.from(this).notify(ID_OPEN, n) } catch (e: SecurityException) { }
    }

    /** A permission was lost (for example Android reset it for an unused app): tell the user once in a while. */
    private fun notifyAttention() {
        val sp = Prefs.sp(this)
        val last = sp.getLong("lastWarn", 0L)
        if (System.currentTimeMillis() - last < 6 * 3_600_000L) return
        sp.edit().putLong("lastWarn", System.currentTimeMillis()).apply()
        channel("attention", "Needs attention", NotificationManager.IMPORTANCE_DEFAULT)
        val pi = PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "attention")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("StandBy Clock needs a permission")
            .setContentText("Tap to open the setup guide")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try { NotificationManagerCompat.from(this).notify(ID_ATTENTION, n) } catch (e: SecurityException) { }
    }

    /* ---------------- foreground notification + overlay ---------------- */

    private fun startAsForeground() {
        channel("auto", "Landscape auto-start", NotificationManager.IMPORTANCE_LOW)
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "auto")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("StandBy Clock is watching")
            .setContentText("Opens when the phone rests on its side")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    /** A 1x1 transparent overlay: newer Android only allows background launches while an overlay is visible. */
    private fun addOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        val lp = WindowManager.LayoutParams(
            1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.START or Gravity.TOP }
        try {
            val v = View(this)
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(v, lp)
            overlay = v
        } catch (e: Exception) { /* no permission yet */ }
    }

    private fun removeOverlay() {
        overlay?.let {
            try { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it) } catch (e: Exception) { }
        }
        overlay = null
    }

    companion object {
        /** True while the watcher process is alive (the main screen shows this). */
        @Volatile var running = false
        private const val LEAVE_MS = 1500L
        private const val ID_OPEN = 2
        private const val ID_ATTENTION = 3
    }
}
