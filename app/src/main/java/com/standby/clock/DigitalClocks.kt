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

/** Big StandBy Digital: bold digits on the left, date + battery + alarm on the right. */
@Composable
fun StandbyDigitalClock(modifier: Modifier = Modifier.fillMaxSize(), color: Color = Color(0xFF7BA7FF)) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val now by rememberNow()
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val nextAlarm by rememberNextAlarm()
    val battery by rememberBatteryInfo()

    val dayStr = remember(now) {
        now.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).uppercase() + " " + now.dayOfMonth
    }
    val alarmStr = remember(nextAlarm) {
        nextAlarm?.let { formatAlarm(ctx, it) }
    }
    val batteryStr = if (battery.charging) "⚡ ${battery.pct}%" else "${battery.pct}%"

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

            Column(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .padding(vertical = with(d) { (totalH * 0.10f).toDp() }, horizontal = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = dayStr,
                        color = color,
                        fontSize = with(d) { (totalH * 0.09f).toSp() },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.02.em,
                    )
                    Text(
                        text = batteryStr,
                        color = Color.White,
                        fontSize = with(d) { (totalH * 0.10f).toSp() },
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (alarmStr != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("⏰", fontSize = with(d) { (totalH * 0.07f).toSp() })
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

@Composable
fun DualClock(palette: Palette, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(1_000)
    val is24 = rememberIs24h()
    val otherZone = remember {
        val here = java.time.ZoneId.systemDefault()
        if (here.id == "UTC" || here.id == "Etc/UTC") java.time.ZoneId.of("America/New_York")
        else java.time.ZoneId.of("UTC")
    }
    val other = now.withZoneSameInstant(otherZone)
    val (lh, lm) = now.hourMinute(is24)
    val (oh, om) = other.hourMinute(is24)
    val hereName = remember { java.time.ZoneId.systemDefault().id.substringAfterLast('/').replace('_', ' ') }
    val otherName = remember(otherZone) { otherZone.id.substringAfterLast('/').replace('_', ' ') }
    BoxWithConstraints(modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
        val d = LocalDensity.current
        val big = with(d) { (constraints.maxHeight * 0.28f).toSp() }
        val small = with(d) { (constraints.maxHeight * 0.07f).toSp() }
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(hereName.uppercase(), color = Color(0xFF8E8E93), fontSize = small, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em)
                Text("$lh:$lm", color = palette.light, fontSize = big, fontWeight = FontWeight.Black, letterSpacing = (-0.03).em)
            }
            Column(horizontalAlignment = Alignment.Start) {
                Text(otherName.uppercase(), color = Color(0xFF8E8E93), fontSize = small, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em)
                Text("$oh:$om", color = Color.White, fontSize = big, fontWeight = FontWeight.Black, letterSpacing = (-0.03).em)
            }
        }
    }
}

@Composable
fun SecondsClock(palette: Palette, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(40)
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val sec = "%02d".format(now.second)
    BoxWithConstraints(modifier.padding(horizontal = 28.dp)) {
        val d = LocalDensity.current
        val timePx = minOf(constraints.maxHeight * 0.42f, constraints.maxWidth * 0.22f)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$h:$m",
                color = palette.light,
                fontSize = with(d) { timePx.toSp() },
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.04).em,
            )
            Text(
                sec,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = with(d) { (timePx * 0.38f).toSp() },
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.12.em,
            )
        }
    }
}
