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

val LiterataFontFamily = FontFamily(
    Font(R.font.literata, FontWeight.Normal)
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal)
)

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

fun readingFontFamilyFor(style: ReadingFontStyle): FontFamily = when (style) {
    ReadingFontStyle.LITERATA -> LiterataFontFamily
    ReadingFontStyle.PLAYFAIR -> PlayfairFontFamily
    ReadingFontStyle.NUNITO -> NunitoFontFamily
    ReadingFontStyle.INTER -> InterFontFamily
    ReadingFontStyle.JETBRAINS_MONO -> JetBrainsMonoFontFamily
}

fun buildChroniclesTypography(
    fontStyle: ReadingFontStyle = ReadingFontStyle.LITERATA,
    fontScale: ReadingFontScale = ReadingFontScale.NORMAL
): Typography {
    val readingFamily = readingFontFamilyFor(fontStyle)
    val uiFamily = if (fontStyle == ReadingFontStyle.JETBRAINS_MONO) {
        JetBrainsMonoFontFamily
    } else {
        InterFontFamily
    }
    val scale = fontScale.multiplier
    val monoDelta = if (fontStyle == ReadingFontStyle.JETBRAINS_MONO) -1.0f else 0f

    return Typography(
        displaySmall = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * scale.coerceAtMost(1.15f)).sp,
            lineHeight = (22f * scale.coerceAtMost(1.15f)).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = readingFamily,
            fontWeight = FontWeight.Normal,
            fontSize = ((16f + monoDelta) * scale).sp,
            lineHeight = (26f * scale).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = ((14f + monoDelta * 0.5f) * scale.coerceAtMost(1.15f)).sp,
            lineHeight = (20f * scale.coerceAtMost(1.15f)).sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        )
    )
}

val Typography = buildChroniclesTypography()
