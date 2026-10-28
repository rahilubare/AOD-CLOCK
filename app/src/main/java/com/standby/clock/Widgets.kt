package com.standby.clock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class StandbyWidget(val id: String, val title: String, val content: @Composable (Palette) -> Unit)

/** ADD YOUR OWN WIDGETS HERE: one StandbyWidget(id, title) { palette -> ... } line each. */
object WidgetCatalog {
    val all: List<StandbyWidget> = listOf(
        StandbyWidget("analog", "Analog clock") { p ->
            MinimalAnalog(Modifier.fillMaxSize(), accent = p.light)
        },
        StandbyWidget("digital", "Digital clock") { p -> BigOverlapClock(p.dark, p.light, Modifier.fillMaxSize()) },
        StandbyWidget("calendar", "Calendar") { _ -> CalendarWidget() },
        StandbyWidget("date", "Date") { _ -> BigDate(Modifier.fillMaxSize().padding(8.dp)) },
        StandbyWidget("battery", "Batteries") { p -> DevicesBatteryWidget(p) },
        StandbyWidget("notes", "Reminders") { _ -> NotesWidget() },
        StandbyWidget("nowplaying", "Now playing") { _ -> NowPlayingWidget() },
        StandbyWidget("upnext", "Calendar events") { _ -> UpNextWidget() },
    )
}

/** Two side-by-side stacks; swipe up/down inside each side. */
@Composable
fun WidgetView(palette: Palette) {
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
        WidgetStack(UiState.left, palette, Modifier.weight(1f).fillMaxHeight())
        Spacer(Modifier.width(24.dp))
        WidgetStack(UiState.right, palette, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
fun WidgetStack(ids: List<String>, palette: Palette, modifier: Modifier = Modifier) {
    val widgets = ids.mapNotNull { id -> WidgetCatalog.all.find { it.id == id } }
    if (widgets.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Empty - long-press to add", color = Color.DarkGray) }
    } else {
        val state = rememberPagerState { widgets.size }
        Box(modifier) {
            VerticalPager(state, Modifier.fillMaxSize()) { i -> widgets[i].content(palette) }
            if (widgets.size > 1) {
                val a by rememberHintAlpha(state.currentPage, state.isScrollInProgress)
                PageDots(widgets.size, state.currentPage, true,
                    Modifier.align(Alignment.CenterEnd).graphicsLayer { alpha = a })
            }
        }
    }
}

@Composable
private fun CalendarWidget() {
    val today by rememberNow(60_000)
    val todayDate = today.toLocalDate()
    val start = 24
    val pager = rememberPagerState(initialPage = start) { 49 }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            val month = todayDate.withDayOfMonth(1).plusMonths((page - start).toLong())
            MonthGrid(month, todayDate, Modifier.fillMaxSize().padding(12.dp))
        }
    }
}

@Composable
private fun NotesWidget() {
    if (UiState.hidePrivate) {
        LockedPlaceholder("Reminders hidden on the lock screen\nTap to unlock")
        return
    }
    val lines = UiState.notes.lines().filter { it.isNotBlank() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Reminders", color = Color(0xFFFF9F0A), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (lines.isEmpty()) {
            Text("Long-press to add reminders", color = Color(0xFF8E8E93), fontSize = 20.sp)
        } else {
            lines.take(6).forEach {
                Text("\u2022 $it", color = Color.White, fontSize = 24.sp, maxLines = 2, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
