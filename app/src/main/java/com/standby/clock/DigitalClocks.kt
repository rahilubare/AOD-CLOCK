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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Line box hugging the digits without clipping the glyphs. */
private fun tight(fontSp: TextUnit, weight: FontWeight, family: FontFamily?, tracking: Float) =
    TextStyle(
        fontSize = fontSp, fontWeight = weight, fontFamily = family,
        letterSpacing = tracking.em, fontFeatureSettings = "tnum",
    )

/** Float: giant rounded digits that overlap in two translucent shades, with a two-dot colon. */
@Composable
fun BigOverlapClock(dark: Color, light: Color, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val digits = h + m
    val boldFont = remember { FontFamily(Typeface.create("sans-serif", Typeface.BOLD)) }
    val adv = rememberAdvances(boldFont, FontWeight.Black)

    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        val d = LocalDensity.current
        val n = digits.length
        val overlapEm = 0.08f
        val dotEm = 0.09f
        val totalEm = n * adv.digit - (n - 1) * overlapEm + dotEm
        // widest allowed by width, tallest allowed by height with safe margin to prevent clipping
        val fontPx = minOf(constraints.maxWidth * 0.90f / totalEm, constraints.maxHeight * 0.74f / 0.72f)
        val fontSp = with(d) { fontPx.toSp() }
        val overlap = with(d) { (fontPx * overlapEm).toDp() }
        val dot = with(d) { (fontPx * dotEm).toDp() }
        val gap = with(d) { (fontPx * 0.18f).toDp() }

        Row(horizontalArrangement = Arrangement.spacedBy(-overlap), verticalAlignment = Alignment.CenterVertically) {
            digits.forEachIndexed { i, ch ->
                if (i == h.length) {
                    Column(
                        Modifier.zIndex(1f).padding(horizontal = with(d) { (fontPx * 0.02f).toDp() }),
                        verticalArrangement = Arrangement.spacedBy(gap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        repeat(2) { Box(Modifier.size(dot).background(Color.White.copy(alpha = 0.85f), CircleShape)) }
                    }
                }
                Text(
                    ch.toString(),
                    color = (if (i % 2 == 0) dark else light).copy(alpha = 0.92f),
                    style = tight(fontSp, FontWeight.Black, boldFont, -0.02f),
                    softWrap = false,
                )
            }
        }
    }
}

/** Digital: tall condensed digits sized to fill the width, the date quietly underneath. */
@Composable
fun TallDigital(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Color(0xFF7BA7FF)) {
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val condensed = remember { FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)) }
    val adv = rememberAdvances(condensed, FontWeight.Bold)
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }

    BoxWithConstraints(modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        val d = LocalDensity.current
        val totalEm = (h.length + m.length) * adv.digit + adv.colon
        val fontPx = minOf(constraints.maxWidth * 0.95f / totalEm, constraints.maxHeight * 0.68f / 0.72f)
        val fontSp = with(d) { fontPx.toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$h:$m", color = color, style = tight(fontSp, FontWeight.Bold, condensed, -0.01f), softWrap = false)
            Spacer(Modifier.height(with(d) { (fontPx * 0.05f).toDp() }))
            Text(
                now.format(dateFmt), color = Color.White.copy(alpha = 0.6f),
                fontSize = with(d) { (fontPx * 0.10f).toSp() }, fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Big StandBy Digital: solid bold digits on left, date + weather + alarm stacked on right (iOS StandBy style). */
@Composable
fun StandbyDigitalClock(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Color(0xFF7BA7FF)) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val nextAlarm by rememberNextAlarm()
    val w = WeatherStore.data

    val dayStr = remember(now) {
        now.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).uppercase() + " " + now.dayOfMonth
    }
    val alarmStr = remember(nextAlarm) {
        nextAlarm?.let { formatAlarm(ctx, it) }
    }
    val weatherStr = remember(w) {
        if (w != null) "${w.temp}°" else "72°"
    }

    val boldFont = remember { FontFamily(Typeface.create("sans-serif", Typeface.BOLD)) }

    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        val d = LocalDensity.current
        val totalH = constraints.maxHeight.toFloat()
        val totalW = constraints.maxWidth.toFloat()

        val timeFontPx = minOf(totalH * 0.76f, (totalW * 0.64f) / 2.3f)
        val timeFontSp = with(d) { timeFontPx.toSp() }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Big digits + colon
            Row(
                modifier = Modifier.weight(0.70f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = h,
                    color = color,
                    style = TextStyle(
                        fontSize = timeFontSp,
                        fontWeight = FontWeight.Black,
                        fontFamily = boldFont,
                        letterSpacing = (-0.03).em,
                        fontFeatureSettings = "tnum",
                    ),
                    softWrap = false,
                )
                // Colon dots
                Column(
                    modifier = Modifier.padding(horizontal = with(d) { (timeFontPx * 0.035f).toDp() }),
                    verticalArrangement = Arrangement.spacedBy(with(d) { (timeFontPx * 0.20f).toDp() }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val dotSize = with(d) { (timeFontPx * 0.10f).toDp() }
                    Box(Modifier.size(dotSize).background(color, CircleShape))
                    Box(Modifier.size(dotSize).background(color, CircleShape))
                }
                Text(
                    text = m,
                    color = color,
                    style = TextStyle(
                        fontSize = timeFontSp,
                        fontWeight = FontWeight.Black,
                        fontFamily = boldFont,
                        letterSpacing = (-0.03).em,
                        fontFeatureSettings = "tnum",
                    ),
                    softWrap = false,
                )
            }

            // Right: Complications stack (Date, Weather, Alarm)
            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .padding(vertical = with(d) { (totalH * 0.12f).toDp() }, horizontal = 8.dp),
                verticalArrangement = if (alarmStr != null) Arrangement.SpaceBetween else Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                // Top: WED 23
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dayStr,
                        color = color,
                        fontSize = with(d) { (totalH * 0.09f).toSp() },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.02.em,
                    )
                    Text(
                        text = weatherStr,
                        color = Color.White,
                        fontSize = with(d) { (totalH * 0.10f).toSp() },
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Bottom: Alarm (ONLY IF real alarm is set!)
                if (alarmStr != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "⏰",
                            fontSize = with(d) { (totalH * 0.07f).toSp() },
                        )
                        Text(
                            text = alarmStr,
                            color = Color.White,
                            fontSize = with(d) { (totalH * 0.08f).toSp() },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
