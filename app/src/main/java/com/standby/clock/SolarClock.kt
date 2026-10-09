package com.standby.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Set this to your latitude (degrees north). Time zone is assumed to be centred on your longitude. */
const val SOLAR_LATITUDE = 20.0

private class Stop(val elev: Double, val top: Color, val bottom: Color)

private val Stops = listOf(
    Stop(-18.0, Color(0xFF03060F), Color(0xFF0B1230)),
    Stop(-6.0, Color(0xFF1B1E4B), Color(0xFF6B4A7A)),
    Stop(0.0, Color(0xFF3A4F8F), Color(0xFFF39A5B)),
    Stop(8.0, Color(0xFF3F78C9), Color(0xFFF7C77E)),
    Stop(30.0, Color(0xFF2E6FD6), Color(0xFF8CC4FF)),
)

private fun skyFor(e: Double): Pair<Color, Color> {
    if (e <= Stops.first().elev) return Stops.first().top to Stops.first().bottom
    for (i in 0 until Stops.size - 1) {
        val a = Stops[i]; val b = Stops[i + 1]
        if (e <= b.elev) {
            val t = ((e - a.elev) / (b.elev - a.elev)).toFloat()
            return lerp(a.top, b.top, t) to lerp(a.bottom, b.bottom, t)
        }
    }
    return Stops.last().top to Stops.last().bottom
}

private fun sunElevationDeg(now: ZonedDateTime): Double {
    val decl = Math.toRadians(-23.44 * cos(2.0 * PI / 365.0 * (now.dayOfYear + 10)))
    val hours = now.hour + now.minute / 60.0 + now.second / 3600.0
    val ha = Math.toRadians(15.0 * (hours - 12.0))
    val lat = Math.toRadians(SOLAR_LATITUDE)
    return Math.toDegrees(asin(sin(lat) * sin(decl) + cos(lat) * cos(decl) * cos(ha)))
}

/** Sky gradient follows the sun's elevation; the sun travels across the screen. */
@Composable
fun SolarClock(modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(1000)
    val is24 = rememberIs24h()
    val (h, m) = now.hourMinute(is24)
    val stars = remember {
        val r = java.util.Random(7)
        List(70) { Offset(r.nextFloat(), r.nextFloat() * 0.7f) }
    }
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val elev = sunElevationDeg(now)
            val (c1, c2) = skyFor(elev)
            drawRect(Brush.verticalGradient(listOf(c1, c2)))

            val night = ((-elev - 2.0) / 10.0).coerceIn(0.0, 1.0).toFloat()
            if (night > 0f) stars.forEach {
                drawCircle(Color.White.copy(alpha = night * 0.8f), 2.5f, Offset(it.x * size.width, it.y * size.height))
            }

            val hours = now.hour + now.minute / 60.0 + now.second / 3600.0
            val ha = Math.toRadians(15.0 * (hours - 12.0))
            val x = size.width * (0.5f + 0.42f * sin(ha).toFloat())
            val y = size.height * 0.78f - (elev / 90.0).toFloat().coerceIn(-0.6f, 1f) * size.height * 0.62f
            val sun = if (elev > -4) Color(0xFFFFE08A) else Color(0xFFDCE6FF)
            drawCircle(sun.copy(alpha = 0.25f), size.height * 0.14f, Offset(x, y))
            drawCircle(sun, size.height * 0.06f, Offset(x, y))
        }
        Text(
            "$h:$m", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 24.dp),
        )
    }
}
