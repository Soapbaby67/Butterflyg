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

private val EmeraldForestColorScheme = darkColorScheme(
    primary = Color(0xFF56D6A2),
    onPrimary = Color(0xFF072318),
    primaryContainer = Color(0xFF154433),
    onPrimaryContainer = Color(0xFFCFF7E5),
    secondary = Color(0xFFD9B462),
    onSecondary = Color(0xFF241B08),
    secondaryContainer = Color(0xFF3D3117),
    onSecondaryContainer = Color(0xFFF6E6C2),
    background = Color(0xFF0B1914),
    onBackground = Color(0xFFE9F2EC),
    surface = Color(0xFF12261E),
    onSurface = Color(0xFFE9F2EC),
    surfaceVariant = Color(0xFF1C362B),
    onSurfaceVariant = Color(0xFFA7C2B4),
    outline = Color(0xFF3C5F4F),
    outlineVariant = Color(0xFF243E32),
    error = Color(0xFFFF868E),
    onError = Color(0xFF2E080D)
)

private val CrimsonVelvetColorScheme = darkColorScheme(
    primary = Color(0xFFEB6B84),
    onPrimary = Color(0xFF2E0812),
    primaryContainer = Color(0xFF511928),
    onPrimaryContainer = Color(0xFFFFD8DF),
    secondary = Color(0xFFD4C1B8),
    onSecondary = Color(0xFF261D19),
    secondaryContainer = Color(0xFF3D2E28),
    onSecondaryContainer = Color(0xFFF3E7E1),
    background = Color(0xFF180C12),
    onBackground = Color(0xFFF6ECEF),
    surface = Color(0xFF24121A),
    onSurface = Color(0xFFF6ECEF),
    surfaceVariant = Color(0xFF351B27),
    onSurfaceVariant = Color(0xFFCBB1BA),
    outline = Color(0xFF633949),
    outlineVariant = Color(0xFF42222F),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF300808)
)

private val SepiaParchmentColorScheme = lightColorScheme(
    primary = Color(0xFF7D4424),
    onPrimary = Color(0xFFFDF7EC),
    primaryContainer = Color(0xFFEBD2B5),
    onPrimaryContainer = Color(0xFF3B1D0C),
    secondary = Color(0xFF9F7434),
    onSecondary = Color(0xFFFDF7EC),
    secondaryContainer = Color(0xFFF1E1C2),
    onSecondaryContainer = Color(0xFF3D2B0D),
    background = Color(0xFFF4E9D5),
    onBackground = Color(0xFF2D2116),
    surface = Color(0xFFF8F0E0),
    onSurface = Color(0xFF2D2116),
    surfaceVariant = Color(0xFFEADCC2),
    onSurfaceVariant = Color(0xFF62503D),
    outline = Color(0xFFB59E7D),
    outlineVariant = Color(0xFFD9C7AA),
    error = Color(0xFFA63030),
    onError = Color.White
)

private val SakuraMistColorScheme = lightColorScheme(
    primary = Color(0xFFA8426D),
    onPrimary = Color(0xFFFFF8FA),
    primaryContainer = Color(0xFFF7D6E4),
    onPrimaryContainer = Color(0xFF46132A),
    secondary = Color(0xFF5F5B8C),
    onSecondary = Color(0xFFFDFBFF),
    secondaryContainer = Color(0xFFE3E0F5),
    onSecondaryContainer = Color(0xFF242142),
    background = Color(0xFFFBF3F6),
    onBackground = Color(0xFF281D26),
    surface = Color(0xFFFFFAFC),
    onSurface = Color(0xFF281D26),
    surfaceVariant = Color(0xFFF2E2EA),
    onSurfaceVariant = Color(0xFF6A5563),
    outline = Color(0xFFC9AFC0),
    outlineVariant = Color(0xFFE6D3DF),
    error = Color(0xFFB33247),
    onError = Color.White
)

private val DeepCosmosColorScheme = darkColorScheme(
    primary = Color(0xFF82BAFF),
    onPrimary = Color(0xFF071E3D),
    primaryContainer = Color(0xFF18345E),
    onPrimaryContainer = Color(0xFFD7E8FF),
    secondary = Color(0xFFF2B86B),
    onSecondary = Color(0xFF291A05),
    secondaryContainer = Color(0xFF473215),
    onSecondaryContainer = Color(0xFFFCE7C8),
    background = Color(0xFF070B1A),
    onBackground = Color(0xFFEBF0FA),
    surface = Color(0xFF101730),
    onSurface = Color(0xFFEBF0FA),
    surfaceVariant = Color(0xFF1B2547),
    onSurfaceVariant = Color(0xFFA8B6D6),
    outline = Color(0xFF3E4E7D),
    outlineVariant = Color(0xFF243054),
    error = Color(0xFFFF8394),
    onError = Color(0xFF2E0810)
)

private val CubicBiomeColorScheme = darkColorScheme(
    primary = Color(0xFF7CE06E),
    onPrimary = Color(0xFF0E260B),
    primaryContainer = Color(0xFF214A1C),
    onPrimaryContainer = Color(0xFFD8FAD2),
    secondary = Color(0xFFF4B649),
    onSecondary = Color(0xFF291B04),
    secondaryContainer = Color(0xFF4A3512),
    onSecondaryContainer = Color(0xFFFDE7BD),
    background = Color(0xFF151C16),
    onBackground = Color(0xFFEDF2EA),
    surface = Color(0xFF1F2820),
    onSurface = Color(0xFFEDF2EA),
    surfaceVariant = Color(0xFF2C382D),
    onSurfaceVariant = Color(0xFFB1C0AE),
    outline = Color(0xFF536652),
    outlineVariant = Color(0xFF344234),
    error = Color(0xFFFF7D7D),
    onError = Color(0xFF2E0808)
)

private val OledObsidianColorScheme = darkColorScheme(
    primary = Color(0xFFE8BC74),
    onPrimary = Color(0xFF211605),
    primaryContainer = Color(0xFF2E2311),
    onPrimaryContainer = Color(0xFFFCE8C7),
    secondary = Color(0xFF9CB4CC),
    onSecondary = Color(0xFF0E1B26),
    secondaryContainer = Color(0xFF1D2936),
    onSecondaryContainer = Color(0xFFDAE6F2),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2ECE1),
    surface = Color(0xFF0C0C0E),
    onSurface = Color(0xFFF2ECE1),
    surfaceVariant = Color(0xFF17171B),
    onSurfaceVariant = Color(0xFFA9A39A),
    outline = Color(0xFF3A3A42),
    outlineVariant = Color(0xFF222228),
    error = Color(0xFFFF858C),
    onError = Color(0xFF2E080B)
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
    applyFontToWholeUi: Boolean = true,
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
            PaperThemeMode.EMERALD_FOREST -> EmeraldForestColorScheme
            PaperThemeMode.CRIMSON_VELVET -> CrimsonVelvetColorScheme
            PaperThemeMode.SEPIA_PARCHMENT -> SepiaParchmentColorScheme
            PaperThemeMode.SAKURA_MIST -> SakuraMistColorScheme
            PaperThemeMode.DEEP_COSMOS -> DeepCosmosColorScheme
            PaperThemeMode.CUBIC_BIOME -> CubicBiomeColorScheme
            PaperThemeMode.OLED_OBSIDIAN -> OledObsidianColorScheme
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

        val typography = remember(fontStyle, fontScale, applyFontToWholeUi) {
            buildChroniclesTypography(
                fontStyle = fontStyle,
                fontScale = fontScale,
                applyToWholeUi = applyFontToWholeUi
            )
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
