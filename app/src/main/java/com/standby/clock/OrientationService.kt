package com.standby.clock

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
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * The "main feature": watches the accelerometer and opens StandBy automatically when the
 * phone is stood up in landscape (and, by default, is charging).
 *
 * Android blocks background apps from opening screens, so this needs the user to grant
 * "Display over other apps". A 1x1 transparent overlay window is kept while running, which
 * counts as a visible overlay (required on newer Android versions).
 */
class OrientationService : Service(), SensorEventListener {

    private lateinit var sm: SensorManager
    private lateinit var pm: PowerManager
    private lateinit var bm: BatteryManager

    private val gravity = FloatArray(3)
    private var firstEvent = true
    private var landscapeSince = 0L
    private var notLandscapeSince = 0L
    private var armed = true
    private var sensorOn = false
    private var overlay: View? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { updateSensor() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        startAsForeground()
        addOverlay()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        updateSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateSensor()   // picks up a changed "only when charging" setting
        return START_STICKY
    }

    override fun onDestroy() {
        stopSensor()
        syncWakeLock()
        try { unregisterReceiver(receiver) } catch (e: IllegalArgumentException) { }
        removeOverlay()
        super.onDestroy()
    }

    /* ---------------- sensor control ---------------- */

    private fun charging() = bm.isCharging

    private fun updateSensor() {
        // "Only while charging" ON  -> watch only on the charger (screen on or off).
        // "Only while charging" OFF -> always watch (screen-off watching costs more battery).
        val want = !Prefs.needCharging(this) || charging()
        if (want && !sensorOn) {
            val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            firstEvent = true
            landscapeSince = 0L
            notLandscapeSince = 0L
            sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
            sensorOn = true
        } else if (!want && sensorOn) {
            stopSensor()
        }
        syncWakeLock()
    }

    /** With the screen off the CPU can sleep and stop sensor events; hold a wake lock (charging only). */
    private fun syncWakeLock() {
        val need = sensorOn && !pm.isInteractive
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

    private fun stopSensor() {
        if (sensorOn) sm.unregisterListener(this)
        sensorOn = false
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(e: SensorEvent) {
        if (firstEvent) {
            for (k in 0..2) gravity[k] = e.values[k]
            firstEvent = false
            return
        }
        // low-pass filter = gravity; what's left over = hand motion
        var motion = 0f
        for (k in 0..2) {
            gravity[k] = 0.8f * gravity[k] + 0.2f * e.values[k]
            motion += abs(e.values[k] - gravity[k])
        }
        val now = SystemClock.elapsedRealtime()
        val gx = abs(gravity[0])
        val gz = abs(gravity[2])
        // phone on its side (short edge down), upright or leaned back up to ~45 degrees
        val landscape = gx > LANDSCAPE_G && gx > gz

        if (landscape) {
            notLandscapeSince = 0L
            // must be resting (on a stand), not held in a moving hand
            if (motion > MAX_MOTION || landscapeSince == 0L) landscapeSince = now
            if (armed && now - landscapeSince >= HOLD_MS && shouldAct()) {
                armed = false
                launchStandby()
            }
        } else {
            landscapeSince = 0L
            if (notLandscapeSince == 0L) notLandscapeSince = now
            if (now - notLandscapeSince >= LEAVE_MS) {
                armed = true                       // re-arm once the phone has left landscape
                StandbyActivity.finishIfAuto()     // pick up the phone -> StandBy closes
            }
        }
    }

    private fun shouldAct(): Boolean =
        Prefs.autoEnabled(this) &&
            (!Prefs.needCharging(this) || charging()) &&
            !StandbyActivity.isOpen

    private fun launchStandby() {
        val i = Intent(this, StandbyActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            .putExtra("auto", true)
        try { startActivity(i) } catch (e: Exception) { /* blocked: check overlay permission */ }
    }

    /* ---------------- foreground notification + overlay ---------------- */

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("auto", "Landscape auto-start", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "auto")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("StandBy Clock is watching")
            .setContentText("Opens when the phone is placed in landscape")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

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
        private const val LANDSCAPE_G = 6.5f   // m/s^2 of gravity along the short edge
        private const val MAX_MOTION = 0.45f   // above this = being held / moved
        private const val HOLD_MS = 2500L      // must stay still in landscape this long
        private const val LEAVE_MS = 1500L     // must leave landscape this long to close/re-arm
    }
}
