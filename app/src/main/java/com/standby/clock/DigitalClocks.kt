package com.standby.clock

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

private fun tight(fontSp: androidx.compose.ui.unit.TextUnit, weight: FontWeight, family: FontFamily? = null) =
    TextStyle(
        fontSize = fontSp, lineHeight = fontSp, fontWeight = weight,
        fontFamily = family,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

/** iOS-style giant digits that overlap in two translucent shades, with a two-dot colon. */
@Composable
fun BigOverlapClock(dark: Color, light: Color, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val digits = h + m

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val d = LocalDensity.current
        val fontPx = minOf(constraints.maxHeight * 1.05f, constraints.maxWidth / (digits.length * 0.52f))
        val fontSp = with(d) { fontPx.toSp() }
        val overlap = with(d) { (fontPx * 0.11f).toDp() }
        val dot = with(d) { (fontPx * 0.13f).toDp() }
        val gap = with(d) { (fontPx * 0.26f).toDp() }

        Row(horizontalArrangement = Arrangement.spacedBy(-overlap), verticalAlignment = Alignment.CenterVertically) {
            digits.forEachIndexed { i, ch ->
                if (i == h.length) {
                    Column(Modifier.zIndex(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(2) { Box(Modifier.size(dot).background(Color.White.copy(alpha = 0.8f), CircleShape)) }
                    }
                }
                Text(
                    ch.toString(),
                    color = (if (i % 2 == 0) dark else light).copy(alpha = 0.9f),
                    style = tight(fontSp, FontWeight.Black),
                    softWrap = false,
                )
            }
        }
    }
}

/** Tall condensed digits on the left, weekday + date on the right. */
@Composable
fun TallDigital(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Color(0xFF7BA7FF)) {
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val condensed = remember { FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)) }
    val blue = color

    BoxWithConstraints(modifier.padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        val d = LocalDensity.current
        val fontPx = minOf(constraints.maxHeight * 1.1f, constraints.maxWidth * 0.3f)
        val fontSp = with(d) { fontPx.toSp() }
        val small = with(d) { (fontPx * 0.12f).toSp() }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$h:$m", color = blue, style = tight(fontSp, FontWeight.Bold, condensed), softWrap = false)
            Spacer(Modifier.width(28.dp))
            Column {
                Row {
                    Text(
                        now.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase(),
                        color = blue, fontSize = small, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(now.dayOfMonth.toString(), color = Color.White, fontSize = small, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
