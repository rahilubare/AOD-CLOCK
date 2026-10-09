package com.standby.clock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/** "Tue Jul" over a giant day-of-month. */
@Composable
fun BigDate(modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(60_000)
    val loc = Locale.getDefault()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = LocalDensity.current
        val fontPx = minOf(constraints.maxHeight * 0.85f, constraints.maxWidth * 0.6f)
        val small = with(d) { (fontPx * 0.13f).toSp() }
        val big = with(d) { fontPx.toSp() }
        Column(horizontalAlignment = Alignment.Start) {
            Row {
                Text(now.dayOfWeek.getDisplayName(DateTextStyle.SHORT, loc),
                    color = Color(0xFFFF453A), fontSize = small, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                Text(now.month.getDisplayName(DateTextStyle.SHORT, loc),
                    color = Color.White, fontSize = small, fontWeight = FontWeight.SemiBold)
            }
            Text(
                now.dayOfMonth.toString(), color = Color.White, softWrap = false,
                style = TextStyle(
                    fontSize = big, lineHeight = big, fontWeight = FontWeight.Bold,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
            )
        }
    }
}
