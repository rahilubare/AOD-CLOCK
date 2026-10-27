package com.standby.clock

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.abs

class BatteryInfo(val pct: Int, val charging: Boolean, val remainingMs: Long)

fun readBattery(ctx: Context): BatteryInfo {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
    var remaining = -1L
    if (Build.VERSION.SDK_INT >= 28 && charging) {
        remaining = (ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).computeChargeTimeRemaining()
    }
    return BatteryInfo(if (scale > 0) level * 100 / scale else 0, charging, remaining)
}

@Composable
fun rememberBatteryInfo(): State<BatteryInfo> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(BatteryInfo(0, false, -1L)) }
    LaunchedEffect(Unit) {
        while (true) {
            state.value = readBattery(ctx)
            delay(15_000)
        }
    }
    return state
}

/** Next alarm set in the clock app. Needs no permission. The StandBy timer is hidden (it is registered as an alarm). */
@Composable
fun rememberNextAlarm(): State<Long?> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        while (true) {
            val t = am.nextAlarmClock?.triggerTime
            state.value = if (t != null && TimerState.endAt > 0L && abs(t - TimerState.endAt) < 2000L) null else t
            delay(30_000)
        }
    }
    return state
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

/** One quiet line under the clock: next alarm, battery, time to full charge. */
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
