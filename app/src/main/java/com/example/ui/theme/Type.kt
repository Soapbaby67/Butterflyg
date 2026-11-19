package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.ReadingFontScale
import com.example.data.repository.ReadingFontStyle

val LiterataFontFamily = FontFamily(Font(R.font.literata, FontWeight.Normal))
val PlayfairFontFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val NunitoFontFamily = FontFamily(Font(R.font.nunito, FontWeight.Normal))
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

// Expressive Cyrillic fonts (pixel, handwritten, calligraphic, chronicle, sci-fi, indie, typewriter)
val MinecraftPixelFontFamily = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
val Arcade16FontFamily = FontFamily(Font(R.font.handjet, FontWeight.Normal))
val AuthorDiaryFontFamily = FontFamily(Font(R.font.caveat, FontWeight.Normal))
val QuillScriptFontFamily = FontFamily(Font(R.font.marck_script, FontWeight.Normal))
val AncientChronicleFontFamily = FontFamily(Font(R.font.ruslan_display, FontWeight.Normal))
val CyberDisplayFontFamily = FontFamily(Font(R.font.tektur, FontWeight.Normal))
val IndieGrungeFontFamily = FontFamily(Font(R.font.underdog, FontWeight.Normal))
val NoirTypewriterFontFamily = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

data class FontMetricsTuning(
    val bodyMultiplier: Float = 1.0f,
    val uiMultiplier: Float = 1.0f,
    val headlineMultiplier: Float = 1.0f,
    val lineHeightBoost: Float = 1.0f,
    val letterSpacingSp: Float = 0.15f
)

fun fontMetricsFor(style: ReadingFontStyle): FontMetricsTuning = when (style) {
    ReadingFontStyle.LITERATA -> FontMetricsTuning()
    ReadingFontStyle.PLAYFAIR -> FontMetricsTuning(
        bodyMultiplier = 1.02f,
        lineHeightBoost = 1.04f
    )
    ReadingFontStyle.NUNITO -> FontMetricsTuning()
    ReadingFontStyle.INTER -> FontMetricsTuning(bodyMultiplier = 0.98f)
    ReadingFontStyle.MINECRAFT_PIXEL -> FontMetricsTuning(
        bodyMultiplier = 0.72f,
        uiMultiplier = 0.74f,
        headlineMultiplier = 0.72f,
        lineHeightBoost = 1.16f,
        letterSpacingSp = 0.45f
    )
    ReadingFontStyle.ARCADE_16 -> FontMetricsTuning(
        bodyMultiplier = 1.16f,
        uiMultiplier = 1.10f,
        headlineMultiplier = 1.12f,
        lineHeightBoost = 1.05f,
        letterSpacingSp = 0.35f
    )
    ReadingFontStyle.AUTHOR_DIARY -> FontMetricsTuning(
        bodyMultiplier = 1.24f,
        uiMultiplier = 1.16f,
        headlineMultiplier = 1.18f,
        lineHeightBoost = 1.08f,
        letterSpacingSp = 0.20f
    )
    ReadingFontStyle.QUILL_SCRIPT -> FontMetricsTuning(
        bodyMultiplier = 1.22f,
        uiMultiplier = 1.12f,
        headlineMultiplier = 1.14f,
        lineHeightBoost = 1.10f,
        letterSpacingSp = 0.25f
    )
    ReadingFontStyle.ANCIENT_CHRONICLE -> FontMetricsTuning(
        bodyMultiplier = 0.96f,
        uiMultiplier = 0.94f,
        headlineMultiplier = 0.96f,
        lineHeightBoost = 1.08f,
        letterSpacingSp = 0.30f
    )
    ReadingFontStyle.CYBER_DISPLAY -> FontMetricsTuning(
        bodyMultiplier = 0.96f,
        uiMultiplier = 0.96f,
        headlineMultiplier = 0.98f,
        lineHeightBoost = 1.04f,
        letterSpacingSp = 0.35f
    )
    ReadingFontStyle.INDIE_GRUNGE -> FontMetricsTuning(
        bodyMultiplier = 1.00f,
        uiMultiplier = 0.98f,
        headlineMultiplier = 1.00f,
        lineHeightBoost = 1.06f,
        letterSpacingSp = 0.20f
    )
    ReadingFontStyle.NOIR_TYPEWRITER -> FontMetricsTuning(
        bodyMultiplier = 0.94f,
        uiMultiplier = 0.94f,
        headlineMultiplier = 0.94f,
        lineHeightBoost = 1.06f,
        letterSpacingSp = 0.10f
    )
    ReadingFontStyle.JETBRAINS_MONO -> FontMetricsTuning(
        bodyMultiplier = 0.92f,
        uiMultiplier = 0.92f,
        headlineMultiplier = 0.94f,
        lineHeightBoost = 1.04f,
        letterSpacingSp = 0.05f
    )
}

fun readingFontFamilyFor(style: ReadingFontStyle): FontFamily = when (style) {
    ReadingFontStyle.LITERATA -> LiterataFontFamily
    ReadingFontStyle.PLAYFAIR -> PlayfairFontFamily
    ReadingFontStyle.NUNITO -> NunitoFontFamily
    ReadingFontStyle.INTER -> InterFontFamily
    ReadingFontStyle.MINECRAFT_PIXEL -> MinecraftPixelFontFamily
    ReadingFontStyle.ARCADE_16 -> Arcade16FontFamily
    ReadingFontStyle.AUTHOR_DIARY -> AuthorDiaryFontFamily
    ReadingFontStyle.QUILL_SCRIPT -> QuillScriptFontFamily
    ReadingFontStyle.ANCIENT_CHRONICLE -> AncientChronicleFontFamily
    ReadingFontStyle.CYBER_DISPLAY -> CyberDisplayFontFamily
    ReadingFontStyle.INDIE_GRUNGE -> IndieGrungeFontFamily
    ReadingFontStyle.NOIR_TYPEWRITER -> NoirTypewriterFontFamily
    ReadingFontStyle.JETBRAINS_MONO -> JetBrainsMonoFontFamily
}

fun buildChroniclesTypography(
    fontStyle: ReadingFontStyle = ReadingFontStyle.LITERATA,
    fontScale: ReadingFontScale = ReadingFontScale.NORMAL,
    applyToWholeUi: Boolean = true
): Typography {
    val readingFamily = readingFontFamilyFor(fontStyle)
    val uiFamily = if (applyToWholeUi) readingFamily else InterFontFamily
    val metrics = fontMetricsFor(fontStyle)
    val uiMetrics = if (applyToWholeUi) metrics else FontMetricsTuning()
    val scale = fontScale.multiplier
    val uiScale = scale.coerceAtMost(1.15f)

    return Typography(
        displaySmall = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (30f * metrics.headlineMultiplier).sp,
            lineHeight = (36f * metrics.headlineMultiplier * metrics.lineHeightBoost).sp,
            letterSpacing = (-0.15).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (24f * metrics.headlineMultiplier).sp,
            lineHeight = (32f * metrics.headlineMultiplier * metrics.lineHeightBoost).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (20f * metrics.headlineMultiplier).sp,
            lineHeight = (28f * metrics.headlineMultiplier * metrics.lineHeightBoost).sp
        ),
        titleLarge = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19f * metrics.headlineMultiplier).sp,
            lineHeight = (26f * metrics.headlineMultiplier * metrics.lineHeightBoost).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * uiMetrics.uiMultiplier * uiScale).sp,
            lineHeight = (22f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost * uiScale).sp,
            letterSpacing = uiMetrics.letterSpacingSp.sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * uiMetrics.uiMultiplier).sp,
            lineHeight = (18f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost).sp,
            letterSpacing = uiMetrics.letterSpacingSp.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * metrics.bodyMultiplier * scale).sp,
            lineHeight = (26f * metrics.bodyMultiplier * metrics.lineHeightBoost * scale).sp,
            letterSpacing = metrics.letterSpacingSp.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * uiMetrics.uiMultiplier * uiScale).sp,
            lineHeight = (20.5f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost * uiScale).sp,
            letterSpacing = uiMetrics.letterSpacingSp.sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * uiMetrics.uiMultiplier).sp,
            lineHeight = (17f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost).sp,
            letterSpacing = uiMetrics.letterSpacingSp.sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * uiMetrics.uiMultiplier).sp,
            lineHeight = (20f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost).sp,
            letterSpacing = uiMetrics.letterSpacingSp.sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * uiMetrics.uiMultiplier).sp,
            lineHeight = (16f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost).sp,
            letterSpacing = (uiMetrics.letterSpacingSp + 0.1f).sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * uiMetrics.uiMultiplier).sp,
            lineHeight = (14.5f * uiMetrics.uiMultiplier * uiMetrics.lineHeightBoost).sp,
            letterSpacing = (uiMetrics.letterSpacingSp + 0.15f).sp
        )
    )
}

val Typography = buildChroniclesTypography()
