package com.standby.clock

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Date

class EventItem(val title: String, val begin: Long, val allDay: Boolean)

private fun queryEvents(ctx: Context): List<EventItem> {
    val now = System.currentTimeMillis()
    val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(b, now)
    ContentUris.appendId(b, now + 36 * 3_600_000L)
    val out = ArrayList<EventItem>()
    try {
        ctx.contentResolver.query(
            b.build(),
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
            null, null, CalendarContract.Instances.BEGIN + " ASC",
        )?.use { c ->
            while (c.moveToNext() && out.size < 4) {
                out.add(EventItem(c.getString(0) ?: "(No title)", c.getLong(1), c.getInt(2) == 1))
            }
        }
    } catch (e: SecurityException) { /* permission revoked */ }
    return out
}

/** Next events from the phone's calendar. Needs the calendar permission (asked from the app's main screen). */
@Composable
fun UpNextWidget() {
    val ctx = LocalContext.current
    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var allowed by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            allowed = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
            events = if (allowed) withContext(Dispatchers.IO) { queryEvents(ctx) } else emptyList()
            delay(60_000L)
        }
    }
    if (UiState.hidePrivate) {
        LockedPlaceholder("Calendar hidden on the lock screen\nTap to unlock")
        return
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Up next", color = Color(0xFFFF9F0A), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        when {
            !allowed -> Text("Allow calendar access in the app to see events", color = Color(0xFF8E8E93), fontSize = 18.sp)
            events.isEmpty() -> Text("Nothing planned", color = Color(0xFF8E8E93), fontSize = 20.sp)
            else -> events.forEach { e ->
                val time = if (e.allDay) "All day" else DateFormat.getTimeFormat(ctx).format(Date(e.begin))
                Column(Modifier.padding(vertical = 5.dp)) {
                    Text(e.title, color = Color.White, fontSize = 22.sp, maxLines = 1)
                    Text(time, color = Color(0xFF8E8E93), fontSize = 15.sp)
                }
            }
        }
    }
}
