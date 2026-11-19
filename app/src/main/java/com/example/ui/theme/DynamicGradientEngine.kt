package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.data.model.EmotionCategory
import com.example.data.repository.PaperThemeMode

enum class AppVisualLocation {
    AUTH,
    SHELF,
    WIZARD,
    GAME,
    PRESENTATION
}

@Immutable
data class DynamicThemeContext(
    val location: AppVisualLocation = AppVisualLocation.SHELF,
    val universe: String = "",
    val weatherMood: WeatherMood = WeatherMood.DAY,
    val activeEmotion: EmotionCategory = EmotionCategory.NEUTRAL
)

@Immutable
data class DynamicGradientSpec(
    val topColor: Color,
    val middleColor: Color,
    val bottomColor: Color,
    val glowColor: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val isDarkSurface: Boolean
)

val LocalDynamicGradientSpec = staticCompositionLocalOf {
    resolveDynamicGradientSpec(
        themeMode = PaperThemeMode.DYNAMIC,
        isSystemDark = true,
        context = DynamicThemeContext()
    )
}

fun resolveDynamicGradientSpec(
    themeMode: PaperThemeMode,
    isSystemDark: Boolean,
    context: DynamicThemeContext
): DynamicGradientSpec {
    return when (themeMode) {
        PaperThemeMode.LIGHT -> DynamicGradientSpec(
            topColor = Color(0xFFFAF6EE),
            middleColor = Color(0xFFF4ECE0),
            bottomColor = Color(0xFFEBE0CD),
            glowColor = AgedBrass.copy(alpha = 0.16f),
            accentPrimary = LiteraryBurgundy,
            accentSecondary = AgedBrass,
            isDarkSurface = false
        )

        PaperThemeMode.DARK -> DynamicGradientSpec(
            topColor = Color(0xFF151311),
            middleColor = Color(0xFF1D1A16),
            bottomColor = Color(0xFF13110F),
            glowColor = MutedBrass.copy(alpha = 0.14f),
            accentPrimary = TerracottaRuby,
            accentSecondary = MutedBrass,
            isDarkSurface = true
        )

        PaperThemeMode.SYSTEM -> if (isSystemDark) {
            resolveDynamicGradientSpec(PaperThemeMode.DARK, true, context)
        } else {
            resolveDynamicGradientSpec(PaperThemeMode.LIGHT, false, context)
        }

        PaperThemeMode.NEON -> DynamicGradientSpec(
            topColor = Color(0xFF0A0E1B),
            middleColor = Color(0xFF141830),
            bottomColor = Color(0xFF0C1C2B),
            glowColor = Color(0xFF39D9EE),
            accentPrimary = Color(0xFF4BE1F2),
            accentSecondary = Color(0xFFF26D9A),
            isDarkSurface = true
        )

        PaperThemeMode.EMERALD_FOREST -> DynamicGradientSpec(
            topColor = Color(0xFF0B1914),
            middleColor = Color(0xFF12271F),
            bottomColor = Color(0xFF0A1410),
            glowColor = Color(0xFF39B98A),
            accentPrimary = Color(0xFF56D6A2),
            accentSecondary = Color(0xFFD9B462),
            isDarkSurface = true
        )

        PaperThemeMode.CRIMSON_VELVET -> DynamicGradientSpec(
            topColor = Color(0xFF180C12),
            middleColor = Color(0xFF28121B),
            bottomColor = Color(0xFF14090E),
            glowColor = Color(0xFFD84A67),
            accentPrimary = Color(0xFFEB6B84),
            accentSecondary = Color(0xFFD4C1B8),
            isDarkSurface = true
        )

        PaperThemeMode.SEPIA_PARCHMENT -> DynamicGradientSpec(
            topColor = Color(0xFFF4E9D5),
            middleColor = Color(0xFFECDDC2),
            bottomColor = Color(0xFFE2CEAB),
            glowColor = Color(0xFF9C6635).copy(alpha = 0.18f),
            accentPrimary = Color(0xFF7D4424),
            accentSecondary = Color(0xFF9F7434),
            isDarkSurface = false
        )

        PaperThemeMode.SAKURA_MIST -> DynamicGradientSpec(
            topColor = Color(0xFFFBF3F6),
            middleColor = Color(0xFFF5E6EC),
            bottomColor = Color(0xFFEBDAE3),
            glowColor = Color(0xFFC9688E).copy(alpha = 0.16f),
            accentPrimary = Color(0xFFA8426D),
            accentSecondary = Color(0xFF5F5B8C),
            isDarkSurface = false
        )

        PaperThemeMode.DEEP_COSMOS -> DynamicGradientSpec(
            topColor = Color(0xFF070B1A),
            middleColor = Color(0xFF101834),
            bottomColor = Color(0xFF0A0F24),
            glowColor = Color(0xFF6FA9FF),
            accentPrimary = Color(0xFF82BAFF),
            accentSecondary = Color(0xFFF2B86B),
            isDarkSurface = true
        )

        PaperThemeMode.CUBIC_BIOME -> DynamicGradientSpec(
            topColor = Color(0xFF151C16),
            middleColor = Color(0xFF1F2A1E),
            bottomColor = Color(0xFF181715),
            glowColor = Color(0xFF62C95A),
            accentPrimary = Color(0xFF7CE06E),
            accentSecondary = Color(0xFFF4B649),
            isDarkSurface = true
        )

        PaperThemeMode.OLED_OBSIDIAN -> DynamicGradientSpec(
            topColor = Color(0xFF000000),
            middleColor = Color(0xFF060608),
            bottomColor = Color(0xFF000000),
            glowColor = Color(0xFFE6B66A).copy(alpha = 0.14f),
            accentPrimary = Color(0xFFE8BC74),
            accentSecondary = Color(0xFF9CB4CC),
            isDarkSurface = true
        )

        PaperThemeMode.DYNAMIC -> resolveDynamicContextGradient(context)
    }
}

private fun resolveDynamicContextGradient(context: DynamicThemeContext): DynamicGradientSpec {
    return when (context.location) {
        AppVisualLocation.AUTH -> DynamicGradientSpec(
            topColor = Color(0xFF1E1322),
            middleColor = Color(0xFF281926),
            bottomColor = Color(0xFF141824),
            glowColor = Color(0xFFE38E6B),
            accentPrimary = Color(0xFFE89B7C),
            accentSecondary = Color(0xFFE0B86C),
            isDarkSurface = true
        )

        AppVisualLocation.SHELF -> DynamicGradientSpec(
            topColor = Color(0xFF171926),
            middleColor = Color(0xFF241D28),
            bottomColor = Color(0xFF191513),
            glowColor = Color(0xFFD89A56),
            accentPrimary = Color(0xFFE58C8A),
            accentSecondary = Color(0xFFDFB268),
            isDarkSurface = true
        )

        AppVisualLocation.WIZARD -> DynamicGradientSpec(
            topColor = Color(0xFF121A2C),
            middleColor = Color(0xFF212138),
            bottomColor = Color(0xFF281C24),
            glowColor = Color(0xFFDF9E58),
            accentPrimary = Color(0xFF7EB8F0),
            accentSecondary = Color(0xFFE5AC65),
            isDarkSurface = true
        )

        AppVisualLocation.PRESENTATION -> DynamicGradientSpec(
            topColor = Color(0xFF131B26),
            middleColor = Color(0xFF1E2636),
            bottomColor = Color(0xFF151923),
            glowColor = Color(0xFF72B0EE),
            accentPrimary = Color(0xFF7EC0F2),
            accentSecondary = Color(0xFFD9AE64),
            isDarkSurface = true
        )

        AppVisualLocation.GAME -> resolveGameMoodGradient(context)
    }
}

private fun resolveGameMoodGradient(context: DynamicThemeContext): DynamicGradientSpec {
    val arch = resolveUniverseArchetype(context.universe)
    val basePalette = resolveUniverseVisualPalette(context.universe, isDark = true)

    val (archTop, archMid, archBottom) = when (arch) {
        UniverseArchetype.COZY_ROMANCE -> Triple(
            Color(0xFF281620),
            Color(0xFF361F25),
            Color(0xFF1E1716)
        )
        UniverseArchetype.SPACE_SCIFI -> Triple(
            Color(0xFF0A1322),
            Color(0xFF122138),
            Color(0xFF0A101C)
        )
        UniverseArchetype.CYBERPUNK_SCIFI -> Triple(
            Color(0xFF0B1426),
            Color(0xFF1C1532),
            Color(0xFF0C1F2A)
        )
        UniverseArchetype.WITCHER_FANTASY -> Triple(
            Color(0xFF1B1B1D),
            Color(0xFF29211E),
            Color(0xFF14181D)
        )
        UniverseArchetype.SHINOBI_ANIME -> Triple(
            Color(0xFF16211F),
            Color(0xFF2A2220),
            Color(0xFF15181C)
        )
        UniverseArchetype.TITAN_SURVIVAL -> Triple(
            Color(0xFF1B1E1E),
            Color(0xFF2A2620),
            Color(0xFF16181A)
        )
        UniverseArchetype.ARCANE_MYSTERY -> Triple(
            Color(0xFF171426),
            Color(0xFF1E292A),
            Color(0xFF14121D)
        )
        UniverseArchetype.GENERIC_CHRONICLE -> Triple(
            Color(0xFF1B161A),
            Color(0xFF261E22),
            Color(0xFF151417)
        )
    }

    val weatherShift = when (context.weatherMood) {
        WeatherMood.RAIN -> Color(0xFF162A3E)
        WeatherMood.STORM -> Color(0xFF1B1E32)
        WeatherMood.FOG -> Color(0xFF262D35)
        WeatherMood.SNOW -> Color(0xFF1D2C3C)
        WeatherMood.NIGHT -> Color(0xFF0E1324)
        WeatherMood.DAWN -> Color(0xFF3E2623)
        WeatherMood.INDOOR_CANDLE -> Color(0xFF362319)
        WeatherMood.NEON_NIGHT -> Color(0xFF102A3D)
        WeatherMood.DAY -> archMid
    }

    val emotionTint = when (context.activeEmotion) {
        EmotionCategory.JOY -> Color(0xFFE0A94F)
        EmotionCategory.EMBARRASSMENT -> Color(0xFFE27C92)
        EmotionCategory.ANGER -> Color(0xFFD9534F)
        EmotionCategory.FEAR -> Color(0xFF6B7EB8)
        EmotionCategory.SUSPICION -> Color(0xFF9EA25D)
        EmotionCategory.SADNESS -> Color(0xFF5D92B0)
        EmotionCategory.SURPRISE -> Color(0xFFE58C4D)
        EmotionCategory.CALM -> Color(0xFF5FA67D)
        EmotionCategory.NEUTRAL -> basePalette.accentPrimary
    }

    val blendedTop = lerp(archTop, weatherShift, 0.38f)
    val blendedMid = lerp(archMid, weatherShift, 0.48f)
    val blendedBottom = lerp(archBottom, emotionTint, 0.14f)

    return DynamicGradientSpec(
        topColor = blendedTop,
        middleColor = blendedMid,
        bottomColor = blendedBottom,
        glowColor = emotionTint,
        accentPrimary = lerp(basePalette.accentPrimary, emotionTint, 0.25f),
        accentSecondary = basePalette.accentSecondary,
        isDarkSurface = true
    )
}

@Composable
fun rememberAnimatedDynamicGradient(spec: DynamicGradientSpec): DynamicGradientSpec {
    val animSpec = tween<Color>(durationMillis = 850)
    val top by animateColorAsState(spec.topColor, animSpec, label = "dyn_top")
    val middle by animateColorAsState(spec.middleColor, animSpec, label = "dyn_mid")
    val bottom by animateColorAsState(spec.bottomColor, animSpec, label = "dyn_bottom")
    val glow by animateColorAsState(spec.glowColor, animSpec, label = "dyn_glow")
    val accent1 by animateColorAsState(spec.accentPrimary, animSpec, label = "dyn_acc1")
    val accent2 by animateColorAsState(spec.accentSecondary, animSpec, label = "dyn_acc2")

    return remember(top, middle, bottom, glow, accent1, accent2, spec.isDarkSurface) {
        DynamicGradientSpec(
            topColor = top,
            middleColor = middle,
            bottomColor = bottom,
            glowColor = glow,
            accentPrimary = accent1,
            accentSecondary = accent2,
            isDarkSurface = spec.isDarkSurface
        )
    }
}

@Composable
fun ProvideDynamicGradientSpec(
    spec: DynamicGradientSpec,
    content: @Composable () -> Unit
) {
    val animated = rememberAnimatedDynamicGradient(spec)
    CompositionLocalProvider(
        LocalDynamicGradientSpec provides animated,
        LocalIsDarkPaper provides animated.isDarkSurface
    ) {
        content()
    }
}

@Composable
fun ChroniclesDynamicBackdrop(
    modifier: Modifier = Modifier,
    showAtmosphericGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val spec = LocalDynamicGradientSpec.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(spec.topColor, spec.middleColor, spec.bottomColor)
                )
            )
    ) {
        if (showAtmosphericGlow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            spec.glowColor.copy(alpha = if (spec.isDarkSurface) 0.20f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.84f, size.height * 0.14f),
                        radius = size.maxDimension * 0.56f
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.14f),
                    radius = size.maxDimension * 0.56f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            spec.accentSecondary.copy(alpha = if (spec.isDarkSurface) 0.14f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.82f),
                        radius = size.maxDimension * 0.48f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.82f),
                    radius = size.maxDimension * 0.48f
                )
            }
        }
        content()
    }
}
