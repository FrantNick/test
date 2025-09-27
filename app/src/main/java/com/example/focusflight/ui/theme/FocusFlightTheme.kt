package com.example.focusflight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    secondary = Color(0xFFF3C969),
    background = Color(0xFF08121E),
    surface = Color(0xFF0D1B2A),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE5EEF8),
    onSurface = Color(0xFFE5EEF8)
)

@Composable
fun FocusFlightTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
