package com.standby.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restarts the landscape watcher after a reboot if auto-start is switched on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED && Prefs.autoEnabled(c)) {
            ContextCompat.startForegroundService(c, Intent(c, OrientationService::class.java))
        }
    }
}
