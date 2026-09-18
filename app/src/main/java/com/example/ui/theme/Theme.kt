package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VetTealLight,
    onPrimary = Color(0xFF003731),
    primaryContainer = VetTealDark,
    onPrimaryContainer = VetTealContainer,
    secondary = VetBlueLight,
    onSecondary = Color(0xFF00344F),
    secondaryContainer = VetBlueDark,
    onSecondaryContainer = VetBlueContainer,
    tertiary = VetAmber,
    onTertiary = Color.White,
    tertiaryContainer = OnVetAmberContainer,
    onTertiaryContainer = VetAmberContainer,
    background = NeutralDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = NeutralDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = VetTeal,
    onPrimary = Color.White,
    primaryContainer = VetTealContainer,
    onPrimaryContainer = OnVetTealContainer,
    secondary = VetBlue,
    onSecondary = Color.White,
    secondaryContainer = VetBlueContainer,
    onSecondaryContainer = OnVetBlueContainer,
    tertiary = VetAmber,
    onTertiary = Color.White,
    tertiaryContainer = VetAmberContainer,
    onTertiaryContainer = OnVetAmberContainer,
    background = NeutralLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = NeutralLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = NeutralLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = NeutralLightOutline,
    outlineVariant = NeutralLightOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional veterinary green/blue branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
