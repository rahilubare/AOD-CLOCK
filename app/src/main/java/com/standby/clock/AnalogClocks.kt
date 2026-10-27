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
import androidx.compose.foundation.layout.fillMaxSize
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import androidx.compose.ui.unit.dp
import kotlin.math.abs
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

/**
 * Edge-to-Edge Dynamic Analog Clock (iOS StandBy style).
 * Automatically calculates screen ratio so tick rays extend dynamically to the rectangular screen borders.
 * Features 12, 3, 6, 9 numerals, Alarm complication on left, Date complication on right, and rounded hands.
 */
@Composable
fun MinimalAnalog(modifier: Modifier = Modifier, accent: Color = Orange) {
    EdgeToEdgeAnalogClock(modifier, accent)
}

@Composable
fun EdgeToEdgeAnalogClock(modifier: Modifier = Modifier.fillMaxSize(), accent: Color = Orange) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val now by rememberNow(40)
    val nextAlarm by rememberNextAlarm()
    val numPaint = remember { textPaint(bold = true) }
    val dayPaint = remember { textPaint(bold = true).apply { color = android.graphics.Color.rgb(255, 69, 58) } }
    val whitePaint = remember { textPaint(bold = true) }
    val alarmPaint = remember { textPaint(bold = true).apply { color = android.graphics.Color.rgb(255, 159, 10) } }

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = center

        // Insets from screen edges to avoid cutouts and rounded bezels
        val marginX = (w * 0.035f).coerceIn(20f, 40f)
        val marginY = (h * 0.045f).coerceIn(16f, 32f)
        val hw = (w / 2f - marginX).coerceAtLeast(10f)
        val hh = (h / 2f - marginY).coerceAtLeast(10f)
        val minDim = minOf(w, h)

        fun rayIntersect(deg: Float): Pair<Offset, Float> {
            val rad = Math.toRadians(deg.toDouble())
            val dx = sin(rad).toFloat()
            val dy = -cos(rad).toFloat()
            val tx = if (abs(dx) > 1e-4f) hw / abs(dx) else Float.MAX_VALUE
            val ty = if (abs(dy) > 1e-4f) hh / abs(dy) else Float.MAX_VALUE
            val tOuter = minOf(tx, ty)
            return Offset(c.x + dx * tOuter, c.y + dy * tOuter) to tOuter
        }

        // 12, 3, 6, 9 Numerals
        val numSize = minDim * 0.16f
        numPaint.textSize = numSize
        val numInset = numSize * 0.55f

        // "12" at top
        label("12", c.x, c.y - hh + numInset, numPaint)
        // "6" at bottom
        label("6", c.x, c.y + hh - numInset * 0.85f, numPaint)
        // "9" at left
        val x9 = c.x - hw + numInset * 0.90f
        label("9", x9, c.y, numPaint)
        // "3" at right
        val x3 = c.x + hw - numInset * 0.90f
        label("3", x3, c.y, numPaint)

        // Draw tick marks
        val tickStrokeMajor = (minDim * 0.0075f).coerceIn(2.5f, 5.5f)
        val tickStrokeMinor = (minDim * 0.0035f).coerceIn(1.2f, 3f)

        for (i in 0 until 60) {
            val deg = i * 6f
            val rad = Math.toRadians(deg.toDouble())
            val dx = sin(rad).toFloat()
            val dy = -cos(rad).toFloat()
            val (pOuter, tOuter) = rayIntersect(deg)

            if (i % 15 == 0) {
                // At 0, 15, 30, 45 (12, 3, 6, 9) - ensure rays NEVER intersect the numerals!
                when (i) {
                    15 -> { // 3 o'clock: ray goes from right of "3" to right edge
                        val startX = x3 + numSize * 0.45f
                        drawLine(Color.White, Offset(startX, c.y), pOuter, strokeWidth = tickStrokeMajor)
                    }
                    45 -> { // 9 o'clock: ray goes from left edge to left of "9"
                        val endX = x9 - numSize * 0.45f
                        drawLine(Color.White, pOuter, Offset(endX, c.y), strokeWidth = tickStrokeMajor)
                    }
                }
                continue
            }

            val isMajor = i % 5 == 0
            val tInner = if (isMajor) {
                tOuter * 0.42f
            } else {
                tOuter - minOf(tOuter * 0.11f, 22f)
            }
            val pInner = Offset(c.x + dx * tInner, c.y + dy * tInner)
            val strokeColor = if (isMajor) Color.White else Color(0xFF5A5A5E)
            val strokeWidth = if (isMajor) tickStrokeMajor else tickStrokeMinor

            drawLine(strokeColor, pInner, pOuter, strokeWidth = strokeWidth)
        }

        // Complications on horizontal axis
        val compSize = minDim * 0.075f
        dayPaint.textSize = compSize
        whitePaint.textSize = compSize
        alarmPaint.textSize = compSize

        // Left Complication: Next Alarm (ONLY IF real alarm is set!)
        val alarmStr = nextAlarm?.let { formatAlarm(ctx, it) }
        if (alarmStr != null) {
            alarmPaint.textAlign = Paint.Align.LEFT
            val alarmX = x9 + numSize * 0.60f
            label(alarmStr, alarmX + compSize * 1.4f, c.y, alarmPaint)

            // Draw clean vector alarm bell icon
            val iconX = alarmX + compSize * 0.5f
            val iconR = compSize * 0.40f
            val strokeW = (minDim * 0.005f).coerceIn(2f, 4f)
            drawCircle(Orange, iconR, Offset(iconX, c.y), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))
            drawLine(Orange, Offset(iconX, c.y), Offset(iconX, c.y - iconR * 0.6f), strokeWidth = strokeW)
            drawLine(Orange, Offset(iconX, c.y), Offset(iconX + iconR * 0.5f, c.y), strokeWidth = strokeW)
        }

        // Right Complication: Date (between center and 3)
        val dayName = now.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()
        val dayNum = now.dayOfMonth.toString()
        dayPaint.textAlign = Paint.Align.LEFT
        whitePaint.textAlign = Paint.Align.LEFT
        val dateWidth = dayPaint.measureText(dayName) + minDim * 0.02f + whitePaint.measureText(dayNum)
        val dateStartX = x3 - numSize * 0.60f - dateWidth
        label(dayName, dateStartX, c.y, dayPaint)
        label(dayNum, dateStartX + dayPaint.measureText(dayName) + minDim * 0.02f, c.y, whitePaint)

        // Clock Hands - iOS StandBy style thick rounded capsules
        val sec = now.second + now.nano / 1_000_000_000f
        val min = now.minute + sec / 60f
        val hr = now.hour % 12 + min / 60f

        val hrLen = hh * 0.52f
        val minLen = hh * 0.82f
        val hrWidth = (minDim * 0.046f).coerceIn(12f, 22f)
        val minWidth = (minDim * 0.036f).coerceIn(9f, 18f)
        val secWidth = (minDim * 0.008f).coerceIn(2.2f, 4.5f)

        // Hour Hand (solid rounded pill capsule)
        drawLine(Color.White, c, polar(c, hr * 30f, hrLen), strokeWidth = hrWidth, cap = StrokeCap.Round)
        // Minute Hand (solid rounded pill capsule)
        drawLine(Color.White, c, polar(c, min * 6f, minLen), strokeWidth = minWidth, cap = StrokeCap.Round)
        // Second Hand with Tail
        drawLine(accent, polar(c, sec * 6f, -hh * 0.20f), polar(c, sec * 6f, hh * 0.92f), strokeWidth = secWidth)
        // Center Pivot
        val hubR = (minDim * 0.024f).coerceIn(6f, 12f)
        drawCircle(accent, hubR, c)
        drawCircle(Color.Black, hubR * 0.45f, c)
    }
}
