@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.standby.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/** Nunito (SIL Open Font License) - the closest free match to Apple's rounded numerals. */
val RoundedFont = FontFamily(
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

/** Glyph advances as a fraction of font size: digit "8" and ":". Used to fit the clock to the screen. */
class Advances(val digit: Float, val colon: Float)

@Composable
fun rememberAdvances(family: FontFamily?, weight: FontWeight): Advances {
    val measurer: TextMeasurer = rememberTextMeasurer()
    val d = LocalDensity.current
    return remember(family, weight) {
        val em = with(d) { 100.sp.toPx() }
        fun adv(s: String) = measurer.measure(
            s, TextStyle(fontSize = 100.sp, fontWeight = weight, fontFamily = family)
        ).size.width / em
        Advances(adv("8"), adv(":"))
    }
}

