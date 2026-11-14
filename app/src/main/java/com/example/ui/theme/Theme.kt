package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.repository.PaperThemeMode
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle

val LocalIsDarkPaper = staticCompositionLocalOf { false }

private val LightPaperColorScheme = lightColorScheme(
    primary = LiteraryBurgundy,
    onPrimary = PaperIvorySurface,
    primaryContainer = LiteraryBurgundyContainer,
    onPrimaryContainer = LiteraryBurgundyOnContainer,
    secondary = AgedBrass,
    onSecondary = PaperIvorySurface,
    secondaryContainer = AgedBrassContainer,
    onSecondaryContainer = AgedBrassOnContainer,
    background = PaperCreamBackground,
    onBackground = InkSepiaPrimaryText,
    surface = PaperIvorySurface,
    onSurface = InkSepiaPrimaryText,
    surfaceVariant = PaperWarmVariant,
    onSurfaceVariant = InkMutedSecondaryText,
    outline = PaperOutline,
    outlineVariant = PaperDivider,
    error = CrimsonErrorLight,
    onError = Color.White
)

private val DarkPaperColorScheme = darkColorScheme(
    primary = TerracottaRuby,
    onPrimary = TerracottaRubyOnPrimary,
    primaryContainer = TerracottaRubyContainer,
    onPrimaryContainer = TerracottaRubyOnContainer,
    secondary = MutedBrass,
    onSecondary = MutedBrassOnSecondary,
    secondaryContainer = MutedBrassContainer,
    onSecondaryContainer = MutedBrassOnContainer,
    background = DarkInkBackground,
    onBackground = IvoryPrimaryText,
    surface = DarkCharcoalSurface,
    onSurface = IvoryPrimaryText,
    surfaceVariant = DarkCharcoalVariant,
    onSurfaceVariant = WarmTaupeSecondaryText,
    outline = DarkPaperOutline,
    outlineVariant = DarkPaperDivider,
    error = CrimsonErrorDark,
    onError = Color(0xFF2B0E11)
)

private val NeonNightColorScheme = darkColorScheme(
    primary = Color(0xFF4BE1F2),
    onPrimary = Color(0xFF062127),
    primaryContainer = Color(0xFF103645),
    onPrimaryContainer = Color(0xFFBDF5FC),
    secondary = Color(0xFFF26D9A),
    onSecondary = Color(0xFF2D0A17),
    secondaryContainer = Color(0xFF441A2C),
    onSecondaryContainer = Color(0xFFFCD6E4),
    background = Color(0xFF0A0E1B),
    onBackground = Color(0xFFEDF2FA),
    surface = Color(0xFF121829),
    onSurface = Color(0xFFEDF2FA),
    surfaceVariant = Color(0xFF1D253B),
    onSurfaceVariant = Color(0xFFA7B3CD),
    outline = Color(0xFF3E4B6E),
    outlineVariant = Color(0xFF27314A),
    error = Color(0xFFFF7B89),
    onError = Color(0xFF2E070D)
)

private val BookishShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ChroniclesTheme(
    themeMode: PaperThemeMode = PaperThemeMode.DYNAMIC,
    dynamicContext: DynamicThemeContext = DynamicThemeContext(),
    fontStyle: ReadingFontStyle = ReadingFontStyle.LITERATA,
    fontScale: ReadingFontScale = ReadingFontScale.NORMAL,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val rawSpec = remember(themeMode, systemDark, dynamicContext) {
        resolveDynamicGradientSpec(
            themeMode = themeMode,
            isSystemDark = systemDark,
            context = dynamicContext
        )
    }

    ProvideDynamicGradientSpec(spec = rawSpec) {
        val animatedSpec = LocalDynamicGradientSpec.current
        val colorScheme = when (themeMode) {
            PaperThemeMode.LIGHT -> LightPaperColorScheme
            PaperThemeMode.DARK -> DarkPaperColorScheme
            PaperThemeMode.SYSTEM -> if (systemDark) DarkPaperColorScheme else LightPaperColorScheme
            PaperThemeMode.NEON -> NeonNightColorScheme
            PaperThemeMode.DYNAMIC -> darkColorScheme(
                primary = animatedSpec.accentPrimary,
                onPrimary = Color(0xFF191214),
                primaryContainer = animatedSpec.accentPrimary.copy(alpha = 0.20f),
                onPrimaryContainer = Color(0xFFF7EFE6),
                secondary = animatedSpec.accentSecondary,
                onSecondary = Color(0xFF1C160C),
                secondaryContainer = animatedSpec.accentSecondary.copy(alpha = 0.22f),
                onSecondaryContainer = Color(0xFFF6E9D2),
                background = animatedSpec.topColor,
                onBackground = Color(0xFFF3ECE2),
                surface = animatedSpec.middleColor.copy(alpha = 0.92f),
                onSurface = Color(0xFFF3ECE2),
                surfaceVariant = animatedSpec.bottomColor.copy(alpha = 0.90f),
                onSurfaceVariant = Color(0xFFC6BDB0),
                outline = Color.White.copy(alpha = 0.22f),
                outlineVariant = Color.White.copy(alpha = 0.12f),
                error = CrimsonErrorDark,
                onError = Color(0xFF2B0E11)
            )
        }

        val typography = remember(fontStyle, fontScale) {
            buildChroniclesTypography(fontStyle = fontStyle, fontScale = fontScale)
        }

        CompositionLocalProvider(LocalIsDarkPaper provides animatedSpec.isDarkSurface) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = BookishShapes,
                content = content
            )
        }
    }
}
