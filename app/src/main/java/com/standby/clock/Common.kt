package com.standby.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PageDots(count: Int, current: Int, vertical: Boolean, modifier: Modifier = Modifier) {
    val dot: @Composable (Int) -> Unit = { i ->
        Box(Modifier.size(5.dp).background(Color.White.copy(alpha = if (i == current) 0.8f else 0.22f), CircleShape))
    }
    if (vertical) Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) { repeat(count) { dot(it) } }
    else Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(count) { dot(it) } }
}
