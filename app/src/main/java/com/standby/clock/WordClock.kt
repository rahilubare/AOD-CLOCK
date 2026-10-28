package com.standby.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth

private val Hours = arrayOf(
    "TWELVE", "ONE", "TWO", "THREE", "FOUR", "FIVE",
    "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN",
)

private data class Spoken(val lines: List<String>, val ten: Boolean, val twenty: Boolean, val half: Boolean, val to: Boolean)

private fun spoken(hour: Int, minute: Int): Spoken {
    val rounded = ((minute + 2) / 5) * 5
    val jump = if (rounded >= 60) 1 else 0
    val m = if (rounded >= 60) 0 else rounded
    val h = Hours[(hour + jump) % 12]
    val next = Hours[(hour + jump + 1) % 12]
    return when (m) {
        0 -> Spoken(listOf("IT'S", h, "O'CLOCK"), false, false, false, false)
        5 -> Spoken(listOf("IT'S FIVE", "PAST", h), false, false, false, false)
        10 -> Spoken(listOf("IT'S TEN", "PAST", h), true, false, false, false)
        15 -> Spoken(listOf("IT'S", "QUARTER PAST", h), false, false, false, false)
        20 -> Spoken(listOf("IT'S TWENTY", "PAST", h), false, true, false, false)
        25 -> Spoken(listOf("IT'S TWENTY FIVE", "PAST", h), false, true, false, false)
        30 -> Spoken(listOf("IT'S", "HALF PAST", h), false, false, true, false)
        35 -> Spoken(listOf("IT'S TWENTY FIVE", "TO", next), false, true, false, true)
        40 -> Spoken(listOf("IT'S TWENTY", "TO", next), false, true, false, true)
        45 -> Spoken(listOf("IT'S", "QUARTER TO", next), false, false, false, true)
        50 -> Spoken(listOf("IT'S TEN", "TO", next), true, false, false, true)
        else -> Spoken(listOf("IT'S FIVE", "TO", next), false, false, false, true)
    }
}

@Composable
fun WordClock(palette: Palette, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(1_000)
    val spoken = spoken(now.hour, now.minute)
    BoxWithConstraints(modifier.padding(horizontal = 36.dp, vertical = 24.dp)) {
        val d = LocalDensity.current
        val line = with(d) { (constraints.maxHeight * 0.16f).toSp() }
        val chipSp = with(d) { (constraints.maxHeight * 0.045f).toSp() }
        Column(Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                spoken.lines.forEach { t ->
                    Text(
                        t,
                        color = palette.light,
                        fontSize = line,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.03).em,
                        lineHeight = line,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val chip: @Composable (String, Boolean) -> Unit = { label, on ->
                    Text(
                        label,
                        color = if (on) palette.light else Color(0xFF3A3A3C),
                        fontSize = chipSp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.em,
                    )
                }
                chip("TEN", spoken.ten)
                Text("  ·  ", color = Color(0xFF3A3A3C), fontSize = chipSp)
                chip("TWENTY", spoken.twenty)
                Text("  ·  ", color = Color(0xFF3A3A3C), fontSize = chipSp)
                chip("HALF", spoken.half)
                Text("  ·  ", color = Color(0xFF3A3A3C), fontSize = chipSp)
                chip("TO", spoken.to)
            }
        }
    }
}
