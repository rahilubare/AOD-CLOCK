package com.standby.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val Red = Color(0xFFFF453A)
private val Dim = Color(0xFF8E8E93)

@Composable
fun MonthGrid(month: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val rowH = maxHeight / 8
        val fs = with(LocalDensity.current) { (rowH * 0.5f).toSp() }
        val locale = Locale.getDefault()
        val firstDow = remember { WeekFields.of(locale).firstDayOfWeek }
        val first = month.withDayOfMonth(1)
        val offset = (first.dayOfWeek.value - firstDow.value + 7) % 7
        val days = month.lengthOfMonth()
        val inThisMonth = month.month == today.month && month.year == today.year

        Column {
            Row(Modifier.height(rowH), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    month.month.getDisplayName(DateTextStyle.FULL, locale).uppercase() + " " + month.year,
                    color = Red, fontSize = fs, fontWeight = FontWeight.SemiBold,
                )
            }
            Row(Modifier.height(rowH)) {
                for (i in 0..6) {
                    val d = firstDow.plus(i.toLong())
                    Cell(d.getDisplayName(DateTextStyle.NARROW, locale), Dim, fs, rowH, false)
                }
            }
            for (w in 0 until 6) {
                Row(Modifier.height(rowH)) {
                    for (d in 0..6) {
                        val day = w * 7 + d - offset + 1
                        if (day in 1..days) {
                            val date = month.withDayOfMonth(day)
                            val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                            Cell(day.toString(), if (weekend) Dim else Color.White, fs, rowH, inThisMonth && day == today.dayOfMonth)
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Cell(
    text: String,
    color: Color,
    fs: androidx.compose.ui.unit.TextUnit,
    rowH: androidx.compose.ui.unit.Dp,
    today: Boolean,
) {
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
        if (today) Box(Modifier.size(rowH * 0.92f).background(Red, CircleShape))
        Text(
            text,
            color = if (today) Color.White else color,
            fontSize = fs,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
