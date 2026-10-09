package com.standby.clock

import androidx.compose.ui.graphics.Color

data class Palette(val name: String, val dark: Color, val light: Color)

val Palettes = listOf(
    Palette("Blue", Color(0xFF0A5FBF), Color(0xFF4DA3F0)),
    Palette("Pink/Teal", Color(0xFFE8628F), Color(0xFF12B5CB)),
    Palette("Green", Color(0xFF2E9E5B), Color(0xFF6EDB8B)),
    Palette("Orange", Color(0xFFD9611A), Color(0xFFFFB347)),
    Palette("Purple", Color(0xFF6A3FC8), Color(0xFFB794F6)),
    Palette("Red", Color(0xFFC0262D), Color(0xFFFF6B6B)),
    Palette("White", Color(0xFF9E9E9E), Color(0xFFFFFFFF)),
)
