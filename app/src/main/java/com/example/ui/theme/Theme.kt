package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryPurple,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryPurple,
    onBackground = TextLight,
    onSurface = TextLight
  )

private val LightColorScheme = DarkColorScheme // Default to Dark, matching immersive UI context

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve customized sci-fi theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
