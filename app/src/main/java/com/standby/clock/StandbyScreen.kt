package com.standby.clock

import android.app.KeyguardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * StandBy UI. Swipe left/right: Widgets | Photos | Clock.
 * Swipe up/down inside a view: clock style / widget in a stack / photo.
 * Long-press: edit. Double-tap: night mode.
 */
@Composable
fun StandbyScreen(onBrightness: (Float) -> Unit, onPickPhotos: (() -> Unit)? = null, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    remember { UiState.init(context); TimerState.restore(context); WeatherStore.init(context); true }
    val palette = Palettes[UiState.palette.coerceIn(0, Palettes.lastIndex)]

    val viewPager = rememberPagerState(initialPage = Prefs.page(context).coerceIn(0, 2)) { 3 }
    LaunchedEffect(viewPager) {
        snapshotFlow { viewPager.settledPage }.collect { Prefs.setPage(context, it) }
    }

    val lux by rememberAmbientLux()
    var forceNight by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val night = forceNight || lux < NIGHT_LUX

    // burn-in care: dim after a minute without touch, and switch the clock off after N hours
    val touch = remember { longArrayOf(System.currentTimeMillis()) }
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        while (true) {
            idle = System.currentTimeMillis() - touch[0] > 60_000L
            UiState.setLocked(km.isKeyguardLocked)
            delay(1000)
        }
    }
    LaunchedEffect(UiState.autoOff) {
        if (UiState.autoOff > 0) {
            delay(UiState.autoOff * 3_600_000L)
            onClose()
        }
    }
    LaunchedEffect(night, idle) {
        onBrightness(
            when {
                night -> 0.02f
                idle -> 0.2f
                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        )
    }

    // small status pill (feedback for gestures that otherwise show nothing)
    var hintText by remember { mutableStateOf("") }
    var hintOn by remember { mutableStateOf(false) }
    LaunchedEffect(hintOn, hintText) { if (hintOn) { delay(2200); hintOn = false } }
    LaunchedEffect(Unit) {
        if (Prefs.bumpOpens(context) <= 3) { hintText = "Swipe to explore, long-press to customise"; hintOn = true }
    }

    val minute by rememberNow(60_000)
    val shiftX = ((minute.minute * 7) % 11 - 5).dp
    val shiftY = ((minute.minute * 5) % 9 - 4).dp

    // the screen steps back while the edit sheet is open (spring, no overshoot)
    val back by animateFloatAsState(if (editing) 0.95f else 1f, spring(dampingRatio = 1f, stiffness = 300f), label = "back")
    val dotsAlpha by rememberHintAlpha(viewPager.currentPage, viewPager.isScrollInProgress)

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            touch[0] = System.currentTimeMillis()
                            idle = false
                        }
                    }
                }
                .pointerInput(editing) {
                    if (!editing) detectTapGestures(
                        onLongPress = { editing = true },
                        onDoubleTap = {
                            forceNight = !forceNight
                            hintText = if (forceNight) "Night mode on" else "Night mode auto"
                            hintOn = true
                        },
                    )
                }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (night) drawRect(Color(0xFFB00000), blendMode = BlendMode.Multiply)
                }
        ) {
            HorizontalPager(
                state = viewPager,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = back; scaleY = back; transformOrigin = TransformOrigin(0f, 0.5f)
                },
            ) { page ->
                val m = if (page == 1) Modifier.fillMaxSize()
                else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).offset(shiftX, shiftY)
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

            AnimatedVisibility(
                hintOn,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)),
                exit = fadeOut(),
            ) {
                Text(
                    hintText, color = Color.White, fontSize = 15.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC2C2C2E))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }

            EditPanel(editing, viewPager.currentPage, { editing = false }, onPickPhotos)

            PageDots(3, viewPager.currentPage, false,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).graphicsLayer { alpha = dotsAlpha })
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
    val a by rememberHintAlpha(state.currentPage, state.isScrollInProgress)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            VerticalPager(state, Modifier.fillMaxSize()) { i -> styles[i].content(palette) }
            PageDots(styles.size, state.currentPage, true,
                Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).graphicsLayer { alpha = a })
        }
        StatusLine()
    }
}
