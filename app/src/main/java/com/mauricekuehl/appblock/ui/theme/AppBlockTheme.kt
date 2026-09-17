package com.mauricekuehl.appblock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF21573A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBEEBCC),
    onPrimaryContainer = Color(0xFF062A18),
    secondary = Color(0xFF4E6354),
    secondaryContainer = Color(0xFFD1E8D5),
    background = Color(0xFFF7F9F5),
    surface = Color(0xFFF7F9F5),
    surfaceVariant = Color(0xFFE1E5DF),
    outline = Color(0xFF727972),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3D3B0),
    onPrimary = Color(0xFF0B3822),
    primaryContainer = Color(0xFF294F37),
    onPrimaryContainer = Color(0xFFBEEBCC),
    secondary = Color(0xFFB5CCBA),
    secondaryContainer = Color(0xFF374B3D),
    background = Color(0xFF101510),
    surface = Color(0xFF101510),
    surfaceVariant = Color(0xFF414842),
)

@Composable
fun AppBlockTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
