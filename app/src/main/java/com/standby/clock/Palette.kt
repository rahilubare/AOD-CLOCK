package com.standby.clock

import androidx.compose.ui.graphics.Color

/** dark/light are the two overlapping shades used by Float; light is the main accent everywhere else. */
data class Palette(val name: String, val dark: Color, val light: Color)

val Palettes = listOf(
    Palette("Blue", Color(0xFF0A5FBF), Color(0xFF58AEFF)),
    Palette("Pink and teal", Color(0xFFE8628F), Color(0xFF19C1D6)),
    Palette("Green", Color(0xFF1F9D55), Color(0xFF6EE79A)),
    Palette("Sunset", Color(0xFFE5483B), Color(0xFFFFB04A)),
    Palette("Violet", Color(0xFF6A3FC8), Color(0xFFB99BFF)),
    Palette("Rose", Color(0xFFC2185B), Color(0xFFFF8FB8)),
    Palette("Mint", Color(0xFF0F8F86), Color(0xFF7BF0DC)),
    Palette("Silver", Color(0xFF8E8E93), Color(0xFFF2F2F7)),
)
