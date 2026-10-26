package com.standby.clock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PageDots(count: Int, current: Int, vertical: Boolean, modifier: Modifier = Modifier) {
    val dot: @Composable (Int) -> Unit = { i ->
        Box(Modifier.size(5.dp).background(Color.White.copy(alpha = if (i == current) 0.85f else 0.25f), CircleShape))
    }
    if (vertical) Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) { repeat(count) { dot(it) } }
    else Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) { repeat(count) { dot(it) } }
}

/** 1 right after something changes, fades to 0 after a moment. Keeps chrome out of the way of the clock. */
@Composable
fun rememberHintAlpha(vararg keys: Any?): State<Float> {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(*keys) {
        visible = true
        delay(2600)
        visible = false
    }
    return animateFloatAsState(if (visible) 1f else 0f, tween(450), label = "hint")
}
