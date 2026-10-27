package com.standby.clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay

/* ---------------- Timer (full-screen, "live activity" style) ---------------- */

object TimerState {
    const val NOTIF_ID = 42
    var endAt by mutableLongStateOf(0L); private set
    var totalMs by mutableLongStateOf(1L); private set

    /** Picks up a timer that was running while the app was closed (kept up to 10 min after it ended). */
    fun restore(c: Context) {
        val sp = Prefs.sp(c)
        val end = sp.getLong("timerEnd", 0L)
        if (end > System.currentTimeMillis() - 10 * 60_000L) {
            endAt = end
            totalMs = sp.getLong("timerTotal", 1L).coerceAtLeast(1L)
        } else endAt = 0L
    }

    fun start(c: Context, minutes: Int) {
        totalMs = minutes * 60_000L
        endAt = System.currentTimeMillis() + totalMs
        Prefs.sp(c).edit().putLong("timerEnd", endAt).putLong("timerTotal", totalMs).apply()
        // a system alarm clock entry: fires even if the app is closed and wakes the screen
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(c, 8, Intent(c, StandbyActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(endAt, show), pending(c))
    }

    fun cancel(c: Context) {
        endAt = 0L
        Prefs.sp(c).edit().putLong("timerEnd", 0L).apply()
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(c))
        NotificationManagerCompat.from(c).cancel(NOTIF_ID)
    }

    private fun pending(c: Context) = PendingIntent.getBroadcast(
        c, 7, Intent(c, TimerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

@Composable
fun TimerOverlay(p: Palette) {
    if (TimerState.endAt > 0L) TimerContent(p)
}

@Composable
private fun TimerContent(p: Palette) {
    val ctx = LocalContext.current
    var remaining by remember { mutableLongStateOf(TimerState.endAt - System.currentTimeMillis()) }
    LaunchedEffect(TimerState.endAt) {
        while (true) {
            remaining = TimerState.endAt - System.currentTimeMillis()
            if (remaining <= 0L) break
            delay(200)
        }
    }
    val done = remaining <= 0L
    val alarmVolumeOff = remember(done) {
        (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getStreamVolume(AudioManager.STREAM_ALARM) == 0
    }
    // the notification rings; only ring from here if notifications are blocked
    DisposableEffect(done) {
        var ring: Ringtone? = null
        if (done && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            ring = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ring?.play()
        }
        onDispose { ring?.stop() }
    }
    val secs = ((remaining + 999) / 1000).coerceAtLeast(0)

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { TimerState.cancel(ctx) }) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            val s = size.minDimension * 0.92f
            val tl = Offset(center.x - s / 2f, center.y - s / 2f)
            val stroke = Stroke(width = s * 0.025f, cap = StrokeCap.Round)
            drawArc(Color(0x22FFFFFF), -90f, 360f, false, tl, Size(s, s), style = stroke)
            val frac = if (done) 0f else remaining.toFloat() / TimerState.totalMs
            drawArc(p.light, -90f, 360f * frac, false, tl, Size(s, s), style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (done) "Time's up" else "%d:%02d".format(secs / 60, secs % 60),
                color = p.light, fontSize = 120.sp, fontWeight = FontWeight.Bold,
            )
            Text("Tap to dismiss", color = Color(0xFF8E8E93), fontSize = 18.sp)
            if (alarmVolumeOff) Text("Alarm volume is off, you will not hear this", color = Color(0xFFFF9F0A), fontSize = 15.sp)
        }
    }
}

/* ---------------- Glanceable notifications ---------------- */

data class GlanceNotif(val app: String, val title: String, val text: String)

object NotificationBus {
    var latest by mutableStateOf<GlanceNotif?>(null)
}

@Composable
fun NotificationOverlay(modifier: Modifier = Modifier) {
    val n = NotificationBus.latest
    LaunchedEffect(n) {
        if (n != null) {
            delay(10_000)
            if (NotificationBus.latest === n) NotificationBus.latest = null
        }
    }
    var revealed by remember(n) { mutableStateOf(false) }
    if (n != null) {
        val hide = UiState.hidePrivate || (UiState.hideNotif && !revealed)
        Column(
            modifier.padding(16.dp).widthIn(max = 520.dp)
                .clip(RoundedCornerShape(20.dp)).background(Color(0xFF1C1C1E))
                .pointerInput(n) { detectTapGestures(onTap = { revealed = true }) }
                .padding(16.dp),
        ) {
            Text(n.app, color = Color(0xFF8E8E93), fontSize = 14.sp)
            if (hide) {
                Text("Notification - tap to view", color = Color.White, fontSize = 20.sp)
            } else {
                if (n.title.isNotBlank()) Text(n.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (n.text.isNotBlank()) Text(n.text, color = Color(0xFFD1D1D6), fontSize = 17.sp, maxLines = 2)
            }
        }
    }
}
