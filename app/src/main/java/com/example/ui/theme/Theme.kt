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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = EcoSagePrimaryDark,
    onPrimary = Color(0xFF071F11),
    secondary = EcoSagePrimaryDark,
    secondaryContainer = EcoDarkSurfaceVariant,
    onSecondaryContainer = EcoSagePrimaryDark,
    tertiary = EcoAmberAccent,
    tertiaryContainer = Color(0xFF5C3E00),
    background = EcoDarkBackground,
    surface = EcoDarkSurface,
    surfaceVariant = EcoDarkSurfaceVariant,
    onBackground = EcoDarkOnSurfaceText,
    onSurface = EcoDarkOnSurfaceText,
    onSurfaceVariant = EcoDarkOnSurfaceVariant,
    outline = EcoDarkBorderOutline,
    error = AlertRed,
    errorContainer = Color(0xFF5C0E0E),
    onErrorContainer = Color(0xFFFEE2E2)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EcoSagePrimary,
    onPrimary = Color.White,
    secondary = EcoSageDark,
    secondaryContainer = EcoSageLight,
    onSecondaryContainer = EcoSageDark,
    tertiary = EcoAmberAccent,
    tertiaryContainer = EcoAmberLight,
    background = EcoWarmBackground,
    surface = Color.White,
    surfaceVariant = EcoNeutralVariantBg,
    onBackground = EcoOnSurfaceText,
    onSurface = EcoOnSurfaceText,
    outline = EcoBorderOutline,
    error = AlertRed,
    errorContainer = AlertRedBg,
    onErrorContainer = AlertRedText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so our custom design is strictly applied
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
