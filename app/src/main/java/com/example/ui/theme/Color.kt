package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Paper Palette (Книжный минимализм — светлая бумага)
val PaperCreamBackground = Color(0xFFF8F4EC)
val PaperIvorySurface = Color(0xFFFFFDF8)
val PaperWarmVariant = Color(0xFFEFE8DA)
val InkSepiaPrimaryText = Color(0xFF1E1B18)
val InkMutedSecondaryText = Color(0xFF5C5349)
val LiteraryBurgundy = Color(0xFF7C2D32)
val LiteraryBurgundyContainer = Color(0xFFF3DFE0)
val LiteraryBurgundyOnContainer = Color(0xFF471216)
val AgedBrass = Color(0xFF8C622B)
val AgedBrassContainer = Color(0xFFF3E7D2)
val AgedBrassOnContainer = Color(0xFF442E0E)
val PaperOutline = Color(0xFFC5B9A5)
val PaperDivider = Color(0xFFDFD6C5)
val CrimsonErrorLight = Color(0xFF9E2A2B)

// Dark Paper Palette (Книжный минимализм — тёмная бумага)
val DarkInkBackground = Color(0xFF141311)
val DarkCharcoalSurface = Color(0xFF1C1A17)
val DarkCharcoalVariant = Color(0xFF272420)
val IvoryPrimaryText = Color(0xFFEAE3D5)
val WarmTaupeSecondaryText = Color(0xFFA99F91)
val TerracottaRuby = Color(0xFFD4757B)
val TerracottaRubyOnPrimary = Color(0xFF2B0E11)
val TerracottaRubyContainer = Color(0xFF4C2226)
val TerracottaRubyOnContainer = Color(0xFFF7D8DA)
val MutedBrass = Color(0xFFD1A35B)
val MutedBrassOnSecondary = Color(0xFF291D08)
val MutedBrassContainer = Color(0xFF46361D)
val MutedBrassOnContainer = Color(0xFFF4E1C0)
val DarkPaperOutline = Color(0xFF544D44)
val DarkPaperDivider = Color(0xFF34302A)
val CrimsonErrorDark = Color(0xFFE57373)

// Semantic NPC Emotion Accents (softened to harmonize with paper surfaces)
data class EmotionVisualPalette(
    val containerLight: Color,
    val contentLight: Color,
    val containerDark: Color,
    val contentDark: Color
)

object EmotionColors {
    val Joy = EmotionVisualPalette(
        containerLight = Color(0xFFF6E8C8),
        contentLight = Color(0xFF6B4708),
        containerDark = Color(0xFF3E3018),
        contentDark = Color(0xFFECC67A)
    )
    val Embarrassment = EmotionVisualPalette(
        containerLight = Color(0xFFF7E1E3),
        contentLight = Color(0xFF7D2D3B),
        containerDark = Color(0xFF45242B),
        contentDark = Color(0xFFF0A7B3)
    )
    val Anger = EmotionVisualPalette(
        containerLight = Color(0xFFF4D8D5),
        contentLight = Color(0xFF8C1D18),
        containerDark = Color(0xFF4A201E),
        contentDark = Color(0xFFF2968F)
    )
    val Fear = EmotionVisualPalette(
        containerLight = Color(0xFFE1E5F0),
        contentLight = Color(0xFF35446B),
        containerDark = Color(0xFF262D3F),
        contentDark = Color(0xFFAAB8DE)
    )
    val Suspicion = EmotionVisualPalette(
        containerLight = Color(0xFFE6E6D8),
        contentLight = Color(0xFF4E4F2F),
        containerDark = Color(0xFF2F3022),
        contentDark = Color(0xFFC8CA9B)
    )
    val Sadness = EmotionVisualPalette(
        containerLight = Color(0xFFDFEAEE),
        contentLight = Color(0xFF2D4F5C),
        containerDark = Color(0xFF203038),
        contentDark = Color(0xFF9AC5D6)
    )
    val Surprise = EmotionVisualPalette(
        containerLight = Color(0xFFF2E2D5),
        contentLight = Color(0xFF7A411B),
        containerDark = Color(0xFF3F2B1D),
        contentDark = Color(0xFFE8B48A)
    )
    val Calm = EmotionVisualPalette(
        containerLight = Color(0xFFE1EAE2),
        contentLight = Color(0xFF2D5337),
        containerDark = Color(0xFF233226),
        contentDark = Color(0xFFA3CAA9)
    )
    val Neutral = EmotionVisualPalette(
        containerLight = Color(0xFFE9E2D4),
        contentLight = Color(0xFF524A40),
        containerDark = Color(0xFF2D2A26),
        contentDark = Color(0xFFC4BAA9)
    )
}
