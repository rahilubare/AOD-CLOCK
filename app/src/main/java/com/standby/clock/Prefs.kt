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
    fun autoEnabled(c: Context): Boolean = sp(c).getBoolean("auto", false)
    fun setAuto(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()
    fun needCharging(c: Context): Boolean = sp(c).getBoolean("needCharging", true)
    fun setNeedCharging(c: Context, v: Boolean) = sp(c).edit().putBoolean("needCharging", v).apply()
}

/** Shared, persisted UI settings (palette, widget stacks, reminders, privacy). */
object UiState {
    private var loaded = false

    var palette by mutableIntStateOf(0); private set
    var left by mutableStateOf(listOf("analog", "digital")); private set
    var right by mutableStateOf(listOf("calendar", "date", "battery")); private set
    var notes by mutableStateOf(""); private set
    var hideNotif by mutableStateOf(true); private set

    fun init(c: Context) {
        if (loaded) return
        loaded = true
        val sp = Prefs.sp(c)
        palette = sp.getInt("palette", 0)
        left = sp.getString("left", null)?.split(',')?.filter { it.isNotBlank() } ?: left
        right = sp.getString("right", null)?.split(',')?.filter { it.isNotBlank() } ?: right
        notes = sp.getString("notes", "") ?: ""
        hideNotif = sp.getBoolean("hideNotif", true)
    }

    fun setPalette(c: Context, i: Int) { palette = i; Prefs.sp(c).edit().putInt("palette", i).apply() }
    fun setNotes(c: Context, s: String) { notes = s; Prefs.sp(c).edit().putString("notes", s).apply() }
    fun setHideNotif(c: Context, b: Boolean) { hideNotif = b; Prefs.sp(c).edit().putBoolean("hideNotif", b).apply() }
    fun toggle(c: Context, leftSide: Boolean, id: String) {
        val cur = if (leftSide) left else right
        val next = if (id in cur) cur - id else cur + id
        if (leftSide) left = next else right = next
        Prefs.sp(c).edit().putString(if (leftSide) "left" else "right", next.joinToString(",")).apply()
    }
}
