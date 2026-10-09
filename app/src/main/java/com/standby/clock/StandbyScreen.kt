package com.standby.clock

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * StandBy UI. Swipe left/right: Widgets | Photos | Clock.
 * Swipe up/down inside a view: clock style / widget in a stack / photo.
 * Long-press: edit. Double-tap: force night mode.
 */
@Composable
fun StandbyScreen(onBrightness: (Float) -> Unit, onPickPhotos: (() -> Unit)? = null) {
    val context = LocalContext.current
    remember { UiState.init(context); true }
    val palette = Palettes[UiState.palette.coerceIn(0, Palettes.lastIndex)]

    val viewPager = rememberPagerState(initialPage = Prefs.page(context).coerceIn(0, 2)) { 3 }
    LaunchedEffect(viewPager) {
        snapshotFlow { viewPager.settledPage }.collect { Prefs.setPage(context, it) }
    }

    val lux by rememberAmbientLux()
    var forceNight by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val night = forceNight || lux < NIGHT_LUX
    LaunchedEffect(night) {
        onBrightness(if (night) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    val minute by rememberNow(60_000)
    val shiftX = ((minute.minute * 7) % 11 - 5).dp
    val shiftY = ((minute.minute * 5) % 9 - 4).dp

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(editing) {
                    if (!editing) detectTapGestures(
                        onLongPress = { editing = true },
                        onDoubleTap = { forceNight = !forceNight },
                    )
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (night) drawRect(Color(0xFFB00000), blendMode = BlendMode.Multiply)
                }
        ) {
            HorizontalPager(state = viewPager, modifier = Modifier.fillMaxSize()) { page ->
                val m = if (page == 1) Modifier.fillMaxSize() else Modifier.fillMaxSize().offset(shiftX, shiftY)
                Box(m, contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> WidgetView(palette)
                        1 -> PhotosView()
                        else -> ClockView(palette)
                    }
                }
            }

            NotificationOverlay(Modifier.align(Alignment.TopCenter))
            TimerOverlay(palette)
            if (editing) EditPanel(viewPager.currentPage, { editing = false }, onPickPhotos)

            PageDots(3, viewPager.currentPage, false, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun ClockView(palette: Palette) {
    val ctx = LocalContext.current
    val styles = ClockStyles.all
    val state = rememberPagerState(initialPage = Prefs.clockStyle(ctx).coerceIn(0, styles.lastIndex)) { styles.size }
    LaunchedEffect(state) {
        snapshotFlow { state.settledPage }.collect { Prefs.setClockStyle(ctx, it) }
    }
    Box(Modifier.fillMaxSize()) {
        VerticalPager(state, Modifier.fillMaxSize()) { i -> styles[i].content(palette) }
        PageDots(styles.size, state.currentPage, true, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
    }
}
