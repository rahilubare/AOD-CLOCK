package com.standby.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restarts the watcher after a reboot or an app update, if auto-start is switched on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val a = i.action
        if ((a == Intent.ACTION_BOOT_COMPLETED || a == Intent.ACTION_MY_PACKAGE_REPLACED) && Prefs.autoEnabled(c)) {
            try {
                ContextCompat.startForegroundService(c, Intent(c, OrientationService::class.java))
            } catch (e: Exception) { /* blocked by the system: the main screen shows "watcher not running" */ }
        }
    }
}
