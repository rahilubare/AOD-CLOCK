package com.standby.clock

import android.text.format.DateFormat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

/** Ticks aligned to the interval boundary (1000 = every second, 40 = smooth analog). */
@Composable
fun rememberNow(intervalMs: Long = 1000L): State<ZonedDateTime> {
    val state = remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            state.value = ZonedDateTime.now()
            delay(intervalMs - System.currentTimeMillis() % intervalMs)
        }
    }
    return state
}

@Composable
fun rememberIs24h(): Boolean {
    val ctx = LocalContext.current
    return remember { DateFormat.is24HourFormat(ctx) }
}

/** "3" / "07" in 12h mode, "13" / "36" in 24h mode. */
fun ZonedDateTime.hourMinute(is24: Boolean): Pair<String, String> {
    val h = if (is24) "%02d".format(hour) else (if (hour % 12 == 0) 12 else hour % 12).toString()
    return h to "%02d".format(minute)
}
