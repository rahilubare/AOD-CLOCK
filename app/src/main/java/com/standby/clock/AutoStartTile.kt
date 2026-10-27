package com.standby.clock

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

/** Quick Settings tile: one tap turns auto-start on or off, and restarts the watcher if the system stopped it. */
class AutoStartTile : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val on = !Prefs.autoEnabled(this)
        if (on && !Settings.canDrawOverlays(this)) { openApp(); return }
        Prefs.setAuto(this, on)
        if (on) {
            try {
                ContextCompat.startForegroundService(this, Intent(this, OrientationService::class.java))
            } catch (e: Exception) { openApp() }
        } else stopService(Intent(this, OrientationService::class.java))
        refresh()
    }

    private fun refresh() {
        val on = Prefs.autoEnabled(this)
        qsTile?.apply {
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "StandBy"
            if (Build.VERSION.SDK_INT >= 29) subtitle = if (on) "On" else "Off"
            updateTile()
        }
    }

    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }
}
