package com.standby.clock

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class StandbyWidget(val id: String, val title: String, val content: @Composable (Palette) -> Unit)

/** ADD YOUR OWN WIDGETS HERE: one StandbyWidget(id, title) { palette -> ... } line each. */
object WidgetCatalog {
    val all: List<StandbyWidget> = listOf(
        StandbyWidget("analog", "Analog clock") { _ ->
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                ClassicAnalog(Modifier.fillMaxSize())
            }
        },
        StandbyWidget("digital", "Digital clock") { p -> BigOverlapClock(p.dark, p.light, Modifier.fillMaxSize().padding(8.dp)) },
        StandbyWidget("calendar", "Calendar") { _ -> CalendarWidget() },
        StandbyWidget("date", "Date") { _ -> BigDate(Modifier.fillMaxSize().padding(16.dp)) },
        StandbyWidget("battery", "Battery") { p -> BatteryWidget(p) },
        StandbyWidget("notes", "Reminders") { _ -> NotesWidget() },
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
    val now by rememberNow(60_000)
    MonthGrid(now.toLocalDate(), Modifier.fillMaxSize().padding(20.dp))
}

@Composable
private fun BatteryWidget(p: Palette) {
    val ctx = LocalContext.current
    var pct by remember { mutableIntStateOf(0) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                pct = level * 100 / scale
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
            delay(15_000)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(24.dp)) {
            val s = size.minDimension * 0.85f
            val tl = Offset(center.x - s / 2f, center.y - s / 2f)
            val stroke = Stroke(width = s * 0.07f, cap = StrokeCap.Round)
            drawArc(Color(0x33FFFFFF), -90f, 360f, false, tl, Size(s, s), style = stroke)
            drawArc(p.light, -90f, 360f * pct / 100f, false, tl, Size(s, s), style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Bold)
            Text(if (charging) "Charging" else "Battery", color = Color(0xFF8E8E93), fontSize = 18.sp)
        }
    }
}

@Composable
private fun NotesWidget() {
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
