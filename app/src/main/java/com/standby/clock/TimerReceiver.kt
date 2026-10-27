package com.standby.clock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Fires when the timer ends, even if the app was closed: alarm-volume sound + full-screen notification. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channel = NotificationChannel("timer", "Timer", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
            enableVibration(true)
        }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            c, 9, Intent(c, StandbyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(c, "timer")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer finished")
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT      // keep ringing until dismissed
        try { NotificationManagerCompat.from(c).notify(TimerState.NOTIF_ID, n) } catch (e: SecurityException) { }
    }
}
