package com.standby.clock

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.abs

class BatteryInfo(val pct: Int, val charging: Boolean, val remainingMs: Long)

fun readBattery(ctx: Context): BatteryInfo {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    var remaining = -1L
    if (Build.VERSION.SDK_INT >= 28 && charging) {
        remaining = (ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).computeChargeTimeRemaining()
    }
    val pct = if (level >= 0 && scale > 0) (level * 100f / scale).toInt().coerceIn(0, 100) else 0
    return BatteryInfo(pct, charging, remaining)
}

@Composable
fun rememberBatteryInfo(): State<BatteryInfo> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(readBattery(ctx)) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state.value = readBattery(ctx) }
        }
        ContextCompat.registerReceiver(ctx, r, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(r) } catch (_: Exception) { } }
    }
    return state
}

/** Next alarm from the system clock app. Null if none is set. */
@Composable
fun rememberNextAlarm(): State<Long?> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(getValidNextAlarm(ctx)) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state.value = getValidNextAlarm(ctx) }
        }
        val f = IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_EXPORTED)
        onDispose { try { ctx.unregisterReceiver(r) } catch (_: Exception) { } }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            state.value = getValidNextAlarm(ctx)
        }
    }
    return state
}

fun getValidNextAlarm(ctx: Context): Long? {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = System.currentTimeMillis()
    val info = am.nextAlarmClock
    if (info != null) {
        val t = info.triggerTime
        val ours = TimerState.endAt > 0L && abs(t - TimerState.endAt) < 8_000L
        val ourPkg = try { info.showIntent?.creatorPackage == ctx.packageName } catch (_: Exception) { false }
        if (!ours && !ourPkg && t > now + 2_000L && t < now + 14 * 86_400_000L) return t
    }
    val formatted = try {
        Settings.System.getString(ctx.contentResolver, Settings.System.NEXT_ALARM_FORMATTED)
    } catch (_: Exception) { null }
    if (!formatted.isNullOrBlank() && info != null) {
        val t = info.triggerTime
        if (t > now && t < now + 14 * 86_400_000L) return t
    }
    return null
}

fun formatAlarm(ctx: Context, t: Long): String {
    val time = DateFormat.getTimeFormat(ctx).format(Date(t))
    val soon = t - System.currentTimeMillis() < 20 * 3_600_000L
    return if (soon) time else DateFormat.format("EEE", t).toString() + " " + time
}

private fun formatDuration(ms: Long): String {
    val m = ms / 60_000L
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

@Composable
fun StatusLine(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bat by rememberBatteryInfo()
    val alarm by rememberNextAlarm()
    val parts = buildList {
        alarm?.let { add("Alarm " + formatAlarm(ctx, it)) }
        add(
            when {
                bat.charging && bat.remainingMs > 0 -> "Charging, full in " + formatDuration(bat.remainingMs)
                bat.charging -> "Charging " + bat.pct + "%"
                else -> bat.pct.toString() + "%"
            }
        )
    }
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
        parts.forEachIndexed { i, p ->
            if (i > 0) Spacer(Modifier.width(28.dp))
            Text(p, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}
