package com.standby.clock

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/** Digital photo frame: auto-advances every 15 s; swipe up/down for next/previous photo. */
@Composable
fun PhotosView(modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    val photos = remember(PhotoStore.revision) { PhotoStore.load(ctx) }

    if (photos.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "No photos yet\nLong-press and choose photos", color = Color(0xFF8E8E93),
                fontSize = 22.sp, textAlign = TextAlign.Center,
            )
        }
        return
    }

    var index by remember { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    val safe = index % photos.size
    LaunchedEffect(photos, index) {
        delay(15_000)
        index = (index + 1) % photos.size
    }
    val now by rememberNow(60_000)
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)

    Box(
        modifier.pointerInput(photos.size) {
            detectVerticalDragGestures(
                onDragStart = { drag = 0f },
                onDragEnd = {
                    if (drag < -80f) index = (safe + 1) % photos.size
                    else if (drag > 80f) index = (safe - 1 + photos.size) % photos.size
                },
                onVerticalDrag = { _, dy -> drag += dy },
            )
        }
    ) {
        Crossfade(targetState = safe, animationSpec = tween(900), label = "photo") { i ->
            AsyncImage(
                model = photos[i], contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            "$h:$m", color = Color.White,
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.SemiBold, shadow = Shadow(Color.Black, blurRadius = 12f)),
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 20.dp),
        )
    }
}
