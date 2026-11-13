package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

private val BookishShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun ChroniclesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPaperColorScheme else LightPaperColorScheme

    CompositionLocalProvider(LocalIsDarkPaper provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BookishShapes,
            content = content
        )
    }
}
