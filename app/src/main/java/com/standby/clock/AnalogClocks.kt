package com.standby.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

/** Round face with 12 numerals — used in the widget stack. */
@Composable
fun ClassicAnalog(modifier: Modifier = Modifier, accent: Color = Color(0xFF58AEFF)) {
    val now by rememberNow(40)
    val paint = remember { textPaint() }
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f * 0.92f

        for (i in 0 until 60) {
            val major = i % 5 == 0
            drawLine(
                if (major) Color.White else Color(0xFF5A5A5E),
                polar(c, i * 6f, r),
                polar(c, i * 6f, r * if (major) 0.88f else 0.94f),
                strokeWidth = r * if (major) 0.018f else 0.008f,
                cap = StrokeCap.Round,
            )
        }
        paint.textSize = r * 0.16f
        for (n in 1..12) {
            val p = polar(c, n * 30f, r * 0.70f)
            label(n.toString(), p.x, p.y, paint)
        }

        val sec = now.second + now.nano / 1_000_000_000f
        val min = now.minute + sec / 60f
        val hr = now.hour % 12 + min / 60f
        val hrW = r * 0.045f
        val minW = r * 0.032f
        drawLine(Color.White, polar(c, hr * 30f, -r * 0.08f), polar(c, hr * 30f, r * 0.48f), strokeWidth = hrW, cap = StrokeCap.Round)
        drawLine(Color.White, polar(c, min * 6f, -r * 0.10f), polar(c, min * 6f, r * 0.78f), strokeWidth = minW, cap = StrokeCap.Round)
        drawLine(accent, polar(c, sec * 6f, -r * 0.16f), polar(c, sec * 6f, r * 0.86f), strokeWidth = r * 0.012f, cap = StrokeCap.Round)
        drawCircle(accent, r * 0.035f, c)
        drawCircle(Color.Black, r * 0.014f, c)
    }
}

@Composable
fun MinimalAnalog(modifier: Modifier = Modifier, accent: Color = Color(0xFF58AEFF)) {
    EdgeToEdgeAnalogClock(modifier, accent)
}

/**
 * iOS StandBy analog: ticks run to the rectangular edges, slim rounded hands,
 * next alarm on the left, weekday + date on the right.
 */
@Composable
fun EdgeToEdgeAnalogClock(modifier: Modifier = Modifier.fillMaxSize(), accent: Color = Color(0xFF58AEFF)) {
    val ctx = LocalContext.current
    val now by rememberNow(40)
    val nextAlarm by rememberNextAlarm()
    val battery by rememberBatteryInfo()
    val numPaint = remember { textPaint(bold = true) }
    val accentPaint = remember { textPaint(bold = true) }
    val whitePaint = remember { textPaint(bold = true) }

    val accentInt = android.graphics.Color.argb(
        255,
        (accent.red * 255).toInt(),
        (accent.green * 255).toInt(),
        (accent.blue * 255).toInt(),
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = center
        val marginX = (w * 0.03f).coerceIn(12f, 36f)
        val marginY = (h * 0.04f).coerceIn(10f, 28f)
        val hw = (w / 2f - marginX).coerceAtLeast(10f)
        val hh = (h / 2f - marginY).coerceAtLeast(10f)
        val minDim = min(w, h)
        val innerR = min(hw, hh)

        fun rayEnd(deg: Float): Pair<Offset, Float> {
            val rad = Math.toRadians(deg.toDouble())
            val dx = sin(rad).toFloat()
            val dy = -cos(rad).toFloat()
            val tx = if (abs(dx) > 1e-4f) hw / abs(dx) else Float.MAX_VALUE
            val ty = if (abs(dy) > 1e-4f) hh / abs(dy) else Float.MAX_VALUE
            val t = min(tx, ty)
            return Offset(c.x + dx * t, c.y + dy * t) to t
        }

        val numSize = innerR * 0.28f
        numPaint.textSize = numSize
        numPaint.color = android.graphics.Color.WHITE
        val numInset = numSize * 0.52f
        label("12", c.x, c.y - hh + numInset, numPaint)
        label("6", c.x, c.y + hh - numInset * 0.88f, numPaint)
        val x9 = c.x - hw + numInset * 0.78f
        val x3 = c.x + hw - numInset * 0.78f
        label("9", x9, c.y, numPaint)
        label("3", x3, c.y, numPaint)

        val tickMajor = (minDim * 0.0065f).coerceIn(2f, 4.5f)
        val tickMinor = (minDim * 0.0032f).coerceIn(1.1f, 2.6f)

        for (i in 0 until 60) {
            val deg = i * 6f
            val rad = Math.toRadians(deg.toDouble())
            val dx = sin(rad).toFloat()
            val dy = -cos(rad).toFloat()
            val (pOuter, tOuter) = rayEnd(deg)

            if (i % 15 == 0) {
                // Keep ticks off the numerals; only the outer stubs at 3 and 9.
                when (i) {
                    15 -> drawLine(Color.White, Offset(x3 + numSize * 0.42f, c.y), pOuter, tickMajor, StrokeCap.Round)
                    45 -> drawLine(Color.White, pOuter, Offset(x9 - numSize * 0.42f, c.y), tickMajor, StrokeCap.Round)
                }
                continue
            }

            val isHour = i % 5 == 0
            val inner = if (isHour) innerR * 0.58f else (tOuter - minOf(tOuter * 0.09f, innerR * 0.12f))
            val pInner = Offset(c.x + dx * inner, c.y + dy * inner)
            drawLine(
                if (isHour) Color.White else Color(0xFF636366),
                pInner, pOuter,
                if (isHour) tickMajor else tickMinor,
                StrokeCap.Round,
            )
        }

        val compSize = innerR * 0.13f
        accentPaint.textSize = compSize
        accentPaint.color = accentInt
        whitePaint.textSize = compSize
        whitePaint.color = android.graphics.Color.WHITE

        val alarmStr = nextAlarm?.let { formatAlarm(ctx, it) }
        val leftText = alarmStr ?: "${battery.pct}%"
        accentPaint.textAlign = Paint.Align.LEFT
        val leftStart = x9 + numSize * 0.55f
        val iconR = compSize * 0.38f
        val iconC = Offset(leftStart + iconR, c.y)
        if (alarmStr != null) {
            drawAlarmBadge(iconC, iconR, accent)
        } else {
            drawBolt(iconC, iconR * 1.1f, if (battery.charging) accent else Color(0xFF8E8E93))
        }
        label(leftText, leftStart + iconR * 2.4f, c.y, accentPaint)

        val dayName = now.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault()).uppercase()
        val dayNum = now.dayOfMonth.toString()
        accentPaint.textAlign = Paint.Align.LEFT
        whitePaint.textAlign = Paint.Align.LEFT
        val gap = minDim * 0.018f
        val dateW = accentPaint.measureText(dayName) + gap + whitePaint.measureText(dayNum)
        val dateX = x3 - numSize * 0.55f - dateW
        label(dayName, dateX, c.y, accentPaint)
        label(dayNum, dateX + accentPaint.measureText(dayName) + gap, c.y, whitePaint)

        val sec = now.second + now.nano / 1_000_000_000f
        val min = now.minute + sec / 60f
        val hr = now.hour % 12 + min / 60f
        val hrLen = innerR * 0.50f
        val minLen = innerR * 0.78f
        val hrW = (minDim * 0.028f).coerceIn(7f, 14f)
        val minW = (minDim * 0.020f).coerceIn(5f, 10f)
        val secW = (minDim * 0.006f).coerceIn(1.8f, 3.2f)

        drawLine(Color.White, polar(c, hr * 30f, -innerR * 0.08f), polar(c, hr * 30f, hrLen), hrW, StrokeCap.Round)
        drawLine(Color.White, polar(c, min * 6f, -innerR * 0.10f), polar(c, min * 6f, minLen), minW, StrokeCap.Round)
        drawLine(accent, polar(c, sec * 6f, -innerR * 0.18f), polar(c, sec * 6f, innerR * 0.92f), secW, StrokeCap.Round)
        val hub = (minDim * 0.016f).coerceIn(4.5f, 8f)
        drawCircle(accent, hub, c)
        drawCircle(Color.Black, hub * 0.42f, c)
    }
}

private fun DrawScope.drawAlarmBadge(center: Offset, r: Float, color: Color) {
    val stroke = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    drawCircle(color, r, center, style = stroke)
    drawLine(color, center, Offset(center.x, center.y - r * 0.55f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
    drawLine(color, center, Offset(center.x + r * 0.42f, center.y), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
}

private fun DrawScope.drawBolt(center: Offset, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x + r * 0.12f, center.y - r)
        lineTo(center.x - r * 0.35f, center.y + r * 0.08f)
        lineTo(center.x + r * 0.05f, center.y + r * 0.08f)
        lineTo(center.x - r * 0.12f, center.y + r)
        lineTo(center.x + r * 0.35f, center.y - r * 0.08f)
        lineTo(center.x - r * 0.05f, center.y - r * 0.08f)
        close()
    }
    drawPath(path, color)
}
