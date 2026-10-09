package com.standby.clock

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Long-press editor. Drawn inside the screen (not as a Dialog window) so it also works
 * inside the screensaver service. view: 0 widgets, 1 photos, 2 clock.
 */
@Composable
fun EditPanel(view: Int, onDismiss: () -> Unit, onPickPhotos: (() -> Unit)?) {
    val ctx = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.widthIn(max = 640.dp).fillMaxHeight(0.92f)
                .pointerInput(Unit) { detectTapGestures(onTap = { }) },
            shape = RoundedCornerShape(24.dp), color = Color(0xFF1C1C1E),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (view) { 0 -> "Widgets"; 1 -> "Photos"; else -> "Clock" },
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    )
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (view) {
                        2 -> {
                            Label("Colours")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Palettes.forEachIndexed { i, p ->
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(p.dark, p.light)))
                                            .border(if (i == UiState.palette) 3.dp else 0.dp, Color.White, CircleShape)
                                            .clickable { UiState.setPalette(ctx, i) }
                                    )
                                }
                            }
                        }
                        0 -> {
                            Label("Widget stacks - swipe up/down inside each side")
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(1f))
                                Text("Left", Modifier.width(48.dp), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                                Text("Right", Modifier.width(48.dp), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                            }
                            WidgetCatalog.all.forEach { w ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(w.title, Modifier.weight(1f), color = Color.White)
                                    Checkbox(checked = w.id in UiState.left, onCheckedChange = { UiState.toggle(ctx, true, w.id) })
                                    Checkbox(checked = w.id in UiState.right, onCheckedChange = { UiState.toggle(ctx, false, w.id) })
                                }
                            }
                            OutlinedTextField(
                                value = UiState.notes, onValueChange = { UiState.setNotes(ctx, it) },
                                label = { Text("Reminders (one per line)") },
                                modifier = Modifier.fillMaxWidth(), minLines = 2,
                            )
                        }
                        else -> {
                            if (onPickPhotos != null) {
                                Button(onClick = { onPickPhotos(); onDismiss() }) { Text("Choose photos") }
                            } else {
                                Text("Open the StandBy Clock app and tap Choose photos.", color = Color.Gray)
                            }
                            TextButton(onClick = { PhotoStore.clear(ctx) }) { Text("Clear photos") }
                        }
                    }

                    Label("Timer (fills the screen)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 10, 15, 30).forEach { min ->
                            OutlinedButton(
                                onClick = { TimerState.start(min); onDismiss() },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) { Text("${min}m") }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Hide notification text until tapped", Modifier.weight(1f), color = Color.White)
                        Switch(checked = UiState.hideNotif, onCheckedChange = { UiState.setHideNotif(ctx, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(t: String) = Text(t, color = Color(0xFF8E8E93), fontSize = 13.sp)
