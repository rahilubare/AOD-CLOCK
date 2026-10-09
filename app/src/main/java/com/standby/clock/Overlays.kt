package com.standby.clock

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
import kotlinx.coroutines.delay

/* ---------------- Timer (full-screen, "live activity" style) ---------------- */

object TimerState {
    var endAt by mutableLongStateOf(0L); private set
    var totalMs by mutableLongStateOf(1L); private set
    fun start(minutes: Int) {
        totalMs = minutes * 60_000L
        endAt = System.currentTimeMillis() + totalMs
    }
    fun cancel() { endAt = 0L }
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
    DisposableEffect(done) {
        var ring: Ringtone? = null
        if (done) {
            ring = RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ring?.play()
        }
        onDispose { ring?.stop() }
    }
    val secs = ((remaining + 999) / 1000).coerceAtLeast(0)

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { TimerState.cancel() }) },
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
        val hide = UiState.hideNotif && !revealed
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
