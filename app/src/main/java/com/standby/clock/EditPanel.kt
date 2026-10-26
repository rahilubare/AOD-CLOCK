package com.standby.clock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val Sheet = Color(0xF21C1C1E)
private val Chip = Color(0xFF2C2C2E)
private val Muted = Color(0xFF8E8E93)

/**
 * Side sheet for long-press editing. Slides in from the right and leaves the same way.
 * The clock stays visible and un-blurred on the left, so colour changes preview live.
 * Drawn inside the screen (not a Dialog window).
 */
@Composable
fun EditPanel(visible: Boolean, view: Int, onDismiss: () -> Unit, onPickPhotos: (() -> Unit)?) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
            Box(
                Modifier.fillMaxSize().background(Color(0x55000000))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(animationSpec = spring(dampingRatio = 1f, stiffness = 380f)) { it },
            exit = slideOutHorizontally(animationSpec = spring(dampingRatio = 1f, stiffness = 380f)) { it },
        ) {
            SheetContent(view, onDismiss, onPickPhotos)
        }
    }
}

@Composable
private fun SheetContent(view: Int, onDismiss: () -> Unit, onPickPhotos: (() -> Unit)?) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxHeight().fillMaxWidth(0.46f)
            .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
            .background(Sheet)
            .pointerInput(Unit) { detectTapGestures(onTap = { }) }
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (view) { 0 -> "Widgets"; 1 -> "Photos"; else -> "Clock" },
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.02).em,
            )
            TextButton(onClick = onDismiss) { Text("Done", fontSize = 16.sp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            when (view) {
                2 -> Section("Colour") {
                    Palettes.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { p -> Swatch(p, Palettes.indexOf(p)) }
                        }
                    }
                    Text(Palettes[UiState.palette.coerceIn(0, Palettes.lastIndex)].name, color = Muted, fontSize = 14.sp)
                }
                0 -> {
                    Section("Stacks") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            Text("Left", Modifier.width(52.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 13.sp)
                            Text("Right", Modifier.width(52.dp), textAlign = TextAlign.Center, color = Muted, fontSize = 13.sp)
                        }
                        WidgetCatalog.all.forEach { w ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(w.title, Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
                                Checkbox(checked = w.id in UiState.left, onCheckedChange = { UiState.toggle(ctx, true, w.id) })
                                Checkbox(checked = w.id in UiState.right, onCheckedChange = { UiState.toggle(ctx, false, w.id) })
                            }
                        }
                    }
                    Section("Reminders") {
                        OutlinedTextField(
                            value = UiState.notes, onValueChange = { UiState.setNotes(ctx, it) },
                            placeholder = { Text("One per line") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2,
                        )
                    }
                }
                else -> Section("Photo frame") {
                    if (onPickPhotos != null) {
                        PillButton("Choose photos") { onPickPhotos(); onDismiss() }
                    } else {
                        Text("Open the app and tap Choose photos.", color = Muted, fontSize = 14.sp)
                    }
                    PillButton("Remove all photos") { PhotoStore.clear(ctx) }
                }
            }

            Section("Timer") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 5, 10, 15, 30).forEach { min ->
                        PillButton("$min min") { TimerState.start(min); onDismiss() }
                    }
                }
            }

            Section("Notifications") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hide text until tapped", Modifier.weight(1f), color = Color.White, fontSize = 16.sp)
                    Switch(checked = UiState.hideNotif, onCheckedChange = { UiState.setHideNotif(ctx, it) })
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun Swatch(p: Palette, index: Int) {
    val selected = index == UiState.palette
    val scale by animateFloatAsState(if (selected) 1.1f else 1f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "swatch")
    val ctx = LocalContext.current
    Box(
        Modifier.size(54.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(p.dark, p.light)))
            .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape)
            .clickable { UiState.setPalette(ctx, index) }
    )
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Chip).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) { Text(text, color = Color.White, fontSize = 15.sp) }
}
