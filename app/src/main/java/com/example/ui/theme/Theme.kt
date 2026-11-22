package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val CareNexLightColorScheme = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalBlueContainer,
    onPrimaryContainer = MedicalBlueDark,
    secondary = CareTeal,
    onSecondary = Color.White,
    secondaryContainer = CareTealContainer,
    onSecondaryContainer = Color(0xFF065F56),
    tertiary = CareTeal,
    onTertiary = Color.White,
    background = CareBackground,
    onBackground = TextPrimary,
    surface = CareSurface,
    onSurface = TextPrimary,
    surfaceVariant = CareSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    error = EmergencyRed,
    onError = Color.White,
    errorContainer = EmergencyRedLight,
    onErrorContainer = Color(0xFF991B1B)
)

private val CareNexDarkColorScheme = darkColorScheme(
    primary = MedicalBlueDarkPrimary,
    onPrimary = Color(0xFF06254D),
    primaryContainer = Color(0xFF0F3B7A),
    onPrimaryContainer = Color(0xFFD6E6FA),
    secondary = CareTealDarkAccent,
    onSecondary = Color(0xFF033832),
    secondaryContainer = Color(0xFF0A4F48),
    onSecondaryContainer = Color(0xFFCCF2ED),
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val CareNexShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CareNexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CareNexDarkColorScheme
        else -> CareNexLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CareNexTypography,
        shapes = CareNexShapes,
        content = content
    )
}
