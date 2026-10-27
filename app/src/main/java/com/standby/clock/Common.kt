package com.standby.clock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Asks Android to show the unlock prompt over the lock screen. */
fun requestUnlock(ctx: Context) {
    val a = ctx.findActivity() ?: return
    (a.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(a, null)
}

/** Shown instead of personal content while the lock screen is up. Tap to unlock. */
@Composable
fun LockedPlaceholder(text: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val ctx = LocalContext.current
    Box(modifier.clickable { requestUnlock(ctx) }, contentAlignment = Alignment.Center) {
        Text(text, color = Color(0xFF8E8E93), fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}
