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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Orange = Color(0xFFFF9F0A)
private val Cities = listOf(40.7 to -74.0, 51.5 to -0.1, 35.7 to 139.7, -33.9 to 151.2, 25.2 to 55.3)
private const val LAT_SPAN = 138f   // map covers lat 80 .. -58

/** Dotted world map with a live day/night shadow, your time zone pinned in orange. */
@Composable
fun WorldClock(p: Palette, modifier: Modifier = Modifier.fillMaxSize()) {
    val now by rememberNow(1000)
    val is24 = rememberIs24h()
    val zone = remember { ZoneId.systemDefault() }
    val city = remember { zone.id.substringAfterLast('/').replace('_', ' ') }
    val local = now.withZoneSameInstant(zone)
    val (h, m) = local.hourMinute(is24)
    val points = remember { WorldMap.points }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val utc = now.withZoneSameInstant(ZoneOffset.UTC)
            val mapW = minOf(size.width * 0.96f, size.height * 0.96f * 360f / LAT_SPAN)
            val mapH = mapW * LAT_SPAN / 360f
            val left = (size.width - mapW) / 2f
            val top = (size.height - mapH) / 2f - size.height * 0.03f
            fun px(lon: Float) = left + (lon + 180f) / 360f * mapW
            fun py(lat: Float) = top + (80f - lat) / LAT_SPAN * mapH

            val dec = Math.toRadians(-23.44 * cos(2.0 * PI / 365.0 * (utc.dayOfYear + 10)))
            val sunLon = (720 - (utc.hour * 60 + utc.minute)) / 4f
            val sd = sin(dec); val cd = cos(dec)

            val day = ArrayList<Offset>(); val night = ArrayList<Offset>()
            var i = 0
            while (i < points.size) {
                val lon = points[i]; val lat = points[i + 1]
                val latR = Math.toRadians(lat.toDouble())
                val alt = sin(latR) * sd + cos(latR) * cd * cos(Math.toRadians((lon - sunLon).toDouble()))
                (if (alt > 0.0) day else night).add(Offset(px(lon), py(lat)))
                i += 2
            }
            val dotW = mapW / 360f * WorldMap.STEP * 0.62f
            drawPoints(night, PointMode.Points, Color.White.copy(alpha = 0.2f), strokeWidth = dotW, cap = StrokeCap.Round)
            drawPoints(day, PointMode.Points, Color.White.copy(alpha = 0.85f), strokeWidth = dotW, cap = StrokeCap.Round)

            Cities.forEach { (lat, lon) -> drawCircle(Orange, dotW * 0.9f, Offset(px(lon.toFloat()), py(lat.toFloat()))) }
            val pin = Offset(px(local.offset.totalSeconds / 3600f * 15f), py(20f))
            drawCircle(Orange, dotW * 1.2f, pin)
            drawCircle(Orange.copy(alpha = 0.6f), dotW * 2.4f, pin, style = Stroke(width = dotW * 0.3f))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 28.dp)) {
            Text(city, color = Orange, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("$h:$m", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Medium)
        }
    }
}
