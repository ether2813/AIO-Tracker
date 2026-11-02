package com.aio.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aio.tracker.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F2DF),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF8A6A12),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF2B1F00),
    tertiary = Color(0xFF3D5F90),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5E3FF),
    onTertiaryContainer = Color(0xFF001B3C),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F2),
    surfaceContainer = Color(0xFFE9EFEC),
    surfaceContainerHigh = Color(0xFFE3EAE7),
    surfaceContainerHighest = Color(0xFFDEE4E1),
    inverseSurface = Color(0xFF2B3230),
    inverseOnSurface = Color(0xFFECF2EF),
    inversePrimary = Color(0xFF7ED8C1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7ED8C1),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF9AF5DD),
    secondary = Color(0xFFF0C048),
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFA7C8FF),
    onTertiary = Color(0xFF05305F),
    tertiaryContainer = Color(0xFF224777),
    onTertiaryContainer = Color(0xFFD5E3FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4946),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303634),
    inverseSurface = Color(0xFFDEE4E1),
    inverseOnSurface = Color(0xFF2B3230),
    inversePrimary = Color(0xFF0B6E5F),
)

/** Extra brand colors that Material's scheme doesn't cover. */
@Immutable
data class ExtendedColors(
    val income: Color,
    val expense: Color,
    val gold: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val chart: List<Color>,
    val notePalette: List<Color>,
    val isDark: Boolean,
) {
    val heroBrush: Brush get() = Brush.linearGradient(listOf(heroStart, heroEnd))
}

private val chartColors = listOf(
    Color(0xFF14A38B), Color(0xFFE8B84A), Color(0xFF5B8DEF), Color(0xFFEF6F6C),
    Color(0xFF9B7BEA), Color(0xFF2EC4D6), Color(0xFFF28CC0), Color(0xFFF59E42),
    Color(0xFF7FB069), Color(0xFF8D99AE),
)

private val LightExtended = ExtendedColors(
    income = Color(0xFF1E9E6A),
    expense = Color(0xFFD9434A),
    gold = Color(0xFFC9971F),
    warning = Color(0xFFE08A00),
    heroStart = Color(0xFF0F8A76),
    heroEnd = Color(0xFF053B34),
    chart = chartColors,
    notePalette = listOf(
        Color.Unspecified, Color(0xFFFFF4C2), Color(0xFFD9F5E8), Color(0xFFDCEBFF),
        Color(0xFFFFE0E0), Color(0xFFEDE3FF), Color(0xFFFFE8CC),
    ),
    isDark = false,
)

private val DarkExtended = ExtendedColors(
    income = Color(0xFF5DD6A0),
    expense = Color(0xFFFF8A84),
    gold = Color(0xFFF0C048),
    warning = Color(0xFFFFB547),
    heroStart = Color(0xFF0E7C6A),
    heroEnd = Color(0xFF042C27),
    chart = chartColors,
    notePalette = listOf(
        Color.Unspecified, Color(0xFF4A4220), Color(0xFF1E3D33), Color(0xFF203247),
        Color(0xFF4A2627), Color(0xFF34294A), Color(0xFF4A3520),
    ),
    isDark = true,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
)

val NumberStyle = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AioTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val ext = if (darkTheme) DarkExtended else LightExtended
    val extended = if (dynamicColor && Build.VERSION.SDK_INT >= 31) {
        ext.copy(
            heroStart = if (darkTheme) scheme.primaryContainer else scheme.primary,
            heroEnd = if (darkTheme) scheme.surfaceContainerLowest else scheme.onPrimaryContainer,
        )
    } else ext
    androidx.compose.runtime.CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

object AppTheme {
    val ext: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
