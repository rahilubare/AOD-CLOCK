package com.standby.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Set this to your latitude (degrees north). Time zone is assumed local. */
const val SOLAR_LATITUDE = 20.0

private fun sunTimes(dayOfYear: Int): Pair<Double, Double> {
    val decl = Math.toRadians(-23.44 * cos(2.0 * PI / 365.0 * (dayOfYear + 10)))
    val lat = Math.toRadians(SOLAR_LATITUDE)
    val cosHa = (-tan(lat) * tan(decl)).coerceIn(-1.0, 1.0)
    val haHours = Math.toDegrees(acos(cosHa)) / 15.0
    return (12.0 - haHours) to (12.0 + haHours)
}

private fun fmtHours(h: Double): String {
    val total = ((h * 60).toInt() + 24 * 60) % (24 * 60)
    val hh = total / 60
    val mm = total % 60
    val h12 = if (hh % 12 == 0) 12 else hh % 12
    return "$h12:${"%02d".format(mm)}"
}

@Composable
fun SolarClock(modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(1_000)
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val (rise, set) = sunTimes(now.dayOfYear)
    BoxWithConstraints(modifier) {
        val big = with(LocalDensity.current) { (minOf(constraints.maxHeight * 0.42f, constraints.maxWidth * 0.28f)).toSp() }
        val cap = with(LocalDensity.current) { (constraints.maxHeight * 0.045f).toSp() }
        Canvas(Modifier.fillMaxSize()) {
            val horizon = size.height * 0.55f
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1F4A), Color(0xFF163A7A)), endY = horizon))
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFFE8A020), Color(0xFFC45A10)), startY = horizon),
                topLeft = Offset(0f, horizon),
            )
            val hours = now.hour + now.minute / 60.0 + now.second / 3600.0
            val span = (set - rise).coerceAtLeast(0.5)
            val t = ((hours - rise) / span).toFloat()
            val sunX = size.width * (0.18f + 0.64f * t.coerceIn(0f, 1f))
            val sunY = horizon
            val r = size.height * 0.16f
            drawCircle(Color(0xFFFFE08A).copy(alpha = 0.18f), r * 1.8f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFC94A).copy(alpha = 0.45f), r * 1.15f, Offset(sunX, sunY))
            drawCircle(Color(0xFFFFD36A), r, Offset(sunX, sunY))
        }
        Text(
            "$h:$m",
            color = Color.White,
            fontSize = big,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.04).em,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            "SUNRISE  ${fmtHours(rise)}",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = cap,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 28.dp, bottom = 20.dp),
        )
        Text(
            "SUNSET  ${fmtHours(set)}",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = cap,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 20.dp),
        )
    }
}
