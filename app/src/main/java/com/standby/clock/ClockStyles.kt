package com.standby.clock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class ClockStyle(val name: String, val content: @androidx.compose.runtime.Composable (Palette) -> Unit)

object ClockStyles {
    val all: List<ClockStyle> = listOf(
        ClockStyle("Analog") { p -> MinimalAnalog(Modifier.fillMaxSize(), accent = p.light) },
        ClockStyle("Digital") { p -> StandbyDigitalClock(color = p.light) },
        ClockStyle("Word") { p -> WordClock(p) },
        ClockStyle("Solar") { _ -> SolarClock() },
        ClockStyle("World") { p -> WorldClock(p) },
        ClockStyle("Float") { p -> BigOverlapClock(p.dark, p.light) },
        ClockStyle("Classic") { p -> ClassicAnalog(Modifier.fillMaxSize(), accent = p.light) },
        ClockStyle("Tall") { p -> TallDigital(color = p.light) },
        ClockStyle("Dual") { p -> DualClock(p) },
        ClockStyle("Seconds") { p -> SecondsClock(p) },
    )
}
