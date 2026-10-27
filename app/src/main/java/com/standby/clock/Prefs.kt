package com.standby.clock

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Prefs {
    fun sp(c: Context): SharedPreferences = c.getSharedPreferences("standby", Context.MODE_PRIVATE)
    fun page(c: Context): Int = sp(c).getInt("view", 2)           // 0 widgets, 1 photos, 2 clock
    fun setPage(c: Context, v: Int) = sp(c).edit().putInt("view", v).apply()
    fun clockStyle(c: Context): Int = sp(c).getInt("clockStyle", 0)
    fun setClockStyle(c: Context, v: Int) = sp(c).edit().putInt("clockStyle", v).apply()
    fun bumpOpens(c: Context): Int { val n = sp(c).getInt("opens", 0) + 1; sp(c).edit().putInt("opens", n).apply(); return n }
    fun oemDone(c: Context): Boolean = sp(c).getBoolean("oemDone", false)
    fun setOemDone(c: Context, v: Boolean) = sp(c).edit().putBoolean("oemDone", v).apply()
    fun autoEnabled(c: Context): Boolean = sp(c).getBoolean("auto", false)
    fun setAuto(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()
    fun needCharging(c: Context): Boolean = sp(c).getBoolean("needCharging", false)
    fun setNeedCharging(c: Context, v: Boolean) = sp(c).edit().putBoolean("needCharging", v).apply()

    /** 0 relaxed, 1 balanced, 2 strict */
    fun sensitivity(c: Context): Int = sp(c).getInt("sens", 1)
    fun setSensitivity(c: Context, v: Int) = sp(c).edit().putInt("sens", v).apply()
    /** Default false: only open while the phone is locked, so it never covers an app or video you are using. */
    fun unlockedOk(c: Context): Boolean = sp(c).getBoolean("unlockedOk", false)
    fun setUnlockedOk(c: Context, v: Boolean) = sp(c).edit().putBoolean("unlockedOk", v).apply()

    fun calibration(c: Context): FloatArray? {
        val p = sp(c).getString("calib", null)?.split(',') ?: return null
        if (p.size != 3) return null
        val v = p.mapNotNull { it.toFloatOrNull() }
        return if (v.size == 3) v.toFloatArray() else null
    }
    fun setCalibration(c: Context, v: FloatArray?) {
        val e = sp(c).edit()
        if (v == null) e.remove("calib") else e.putString("calib", v.joinToString(","))
        e.apply()
    }
}

/** Shared, persisted UI settings. */
object UiState {
    private var loaded = false

    var palette by mutableIntStateOf(0); private set
    var left by mutableStateOf(listOf("analog", "digital")); private set
    var right by mutableStateOf(listOf("calendar", "date", "weather", "nowplaying", "battery")); private set
    var notes by mutableStateOf(""); private set
    var hideNotif by mutableStateOf(true); private set
    var hideWhenLocked by mutableStateOf(true); private set
    var autoOff by mutableIntStateOf(8); private set          // hours, 0 = never
    var locked by mutableStateOf(false); private set          // lock screen showing right now

    /** Personal content (reminders, photos, calendar, notification text) is hidden while this is true. */
    val hidePrivate: Boolean get() = hideWhenLocked && locked

    fun init(c: Context) {
        if (loaded) return
        loaded = true
        val sp = Prefs.sp(c)
        palette = sp.getInt("palette", 0)
        left = sp.getString("left", null)?.split(',')?.filter { it.isNotBlank() } ?: left
        right = sp.getString("right", null)?.split(',')?.filter { it.isNotBlank() } ?: right
        notes = sp.getString("notes", "") ?: ""
        hideNotif = sp.getBoolean("hideNotif", true)
        hideWhenLocked = sp.getBoolean("hideWhenLocked", true)
        autoOff = sp.getInt("autoOff", 8)
    }

    @JvmName("updateLocked")
    fun setLocked(b: Boolean) { locked = b }
    fun setPalette(c: Context, i: Int) { palette = i; Prefs.sp(c).edit().putInt("palette", i).apply() }
    fun setNotes(c: Context, s: String) { notes = s; Prefs.sp(c).edit().putString("notes", s).apply() }
    fun setHideNotif(c: Context, b: Boolean) { hideNotif = b; Prefs.sp(c).edit().putBoolean("hideNotif", b).apply() }
    fun setHideWhenLocked(c: Context, b: Boolean) { hideWhenLocked = b; Prefs.sp(c).edit().putBoolean("hideWhenLocked", b).apply() }
    fun setAutoOff(c: Context, h: Int) { autoOff = h; Prefs.sp(c).edit().putInt("autoOff", h).apply() }
    fun toggle(c: Context, leftSide: Boolean, id: String) {
        val cur = if (leftSide) left else right
        val next = if (id in cur) cur - id else cur + id
        if (leftSide) left = next else right = next
        Prefs.sp(c).edit().putString(if (leftSide) "left" else "right", next.joinToString(",")).apply()
    }
}
