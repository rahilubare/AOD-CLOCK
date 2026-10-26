package com.standby.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Orange = Color(0xFFFF9F0A)
private val RedDate = Color(0xFFFF453A)

/** Angle in degrees clockwise from 12 o'clock. */
private fun polar(c: Offset, deg: Float, r: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(c.x + r * sin(a).toFloat(), c.y - r * cos(a).toFloat())
}

private fun textPaint(bold: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create("sans-serif-medium", if (bold) Typeface.BOLD else Typeface.NORMAL)
}

private fun DrawScope.label(text: String, x: Float, y: Float, paint: Paint) {
    val fm = paint.fontMetrics
    drawContext.canvas.nativeCanvas.drawText(text, x, y - (fm.ascent + fm.descent) / 2f, paint)
}

/** Round face with all 12 numerals and an orange sweeping second hand. */
@Composable
fun ClassicAnalog(modifier: Modifier = Modifier, accent: Color = Orange) {
    val now by rememberNow(40)
    val paint = remember { textPaint() }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.96f

        for (i in 0 until 60) {
            val major = i % 5 == 0
            drawLine(
                if (major) Color(0xFFBDBDBD) else Color(0xFF6E6E6E),
                polar(c, i * 6f, r), polar(c, i * 6f, r * if (major) 0.91f else 0.945f),
                strokeWidth = r * if (major) 0.02f else 0.012f,
            )
        }
        paint.textSize = r * 0.2f
        for (n in 1..12) {
            val p = polar(c, n * 30f, r * 0.73f)
            label(n.toString(), p.x, p.y, paint)
        }

        val sec = now.second + now.nano / 1_000_000_000f
        val min = now.minute + sec / 60f
        val hr = now.hour % 12 + min / 60f
        drawLine(Color.White, c, polar(c, hr * 30f, r * 0.5f), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
        drawLine(Color.White, c, polar(c, min * 6f, r * 0.8f), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
        drawLine(accent, polar(c, sec * 6f, -r * 0.18f), polar(c, sec * 6f, r * 0.88f), strokeWidth = r * 0.015f)
        drawCircle(accent, r * 0.05f, c)
        drawCircle(Color.Black, r * 0.025f, c)
    }
}

/** Wide landscape dial: elliptical ticks, 12/3/6/9 numerals, date on the right. */
@Composable
fun MinimalAnalog(modifier: Modifier = Modifier, accent: Color = Orange) {
    val now by rememberNow(40)
    val num = remember { textPaint() }
    val dayP = remember { textPaint().apply { textAlign = Paint.Align.LEFT; color = android.graphics.Color.rgb(255, 69, 58) } }
    val dateP = remember { textPaint().apply { textAlign = Paint.Align.LEFT } }
    Canvas(modifier) {
        val c = center
        val rx = size.width / 2f
        val ry = size.height / 2f
        val m = size.minDimension

        fun pt(deg: Float, s: Float): Offset {
            val a = Math.toRadians(deg.toDouble())
            return Offset(c.x + rx * s * sin(a).toFloat(), c.y - ry * s * cos(a).toFloat())
        }

        for (i in 0 until 60) {
            if (i % 15 == 0) continue
            val major = i % 5 == 0
            drawLine(
                Color(if (major) 0xFFCFCFCF else 0xFF777777),
                pt(i * 6f, 0.98f), pt(i * 6f, if (major) 0.74f else 0.93f),
                strokeWidth = m * if (major) 0.012f else 0.007f,
            )
        }
        num.textSize = m * 0.15f
        listOf(0 to "12", 90 to "3", 180 to "6", 270 to "9").forEach { (deg, t) ->
            val p = pt(deg.toFloat(), 0.82f)
            label(t, p.x, p.y, num)
        }

        val sec = now.second + now.nano / 1_000_000_000f
        val min = now.minute + sec / 60f
        val hr = now.hour % 12 + min / 60f
        fun hand(deg: Float, len: Float) = polar(c, deg, m * len)
        drawLine(Color.White, c, hand(hr * 30f, 0.30f), strokeWidth = m * 0.034f, cap = StrokeCap.Round)
        drawLine(Color.White, c, hand(min * 6f, 0.44f), strokeWidth = m * 0.034f, cap = StrokeCap.Round)
        drawLine(accent, polar(c, sec * 6f, -m * 0.08f), hand(sec * 6f, 0.46f), strokeWidth = m * 0.008f)
        drawCircle(accent, m * 0.02f, c)
        drawCircle(Color.Black, m * 0.01f, c)

        // date complication (right of centre)
        dayP.textSize = m * 0.09f; dateP.textSize = m * 0.09f
        val day = now.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()
        val x = c.x + rx * 0.36f
        label(day, x, c.y, dayP)
        label(now.dayOfMonth.toString(), x + dayP.measureText(day) + m * 0.03f, c.y, dateP)
    }
}
