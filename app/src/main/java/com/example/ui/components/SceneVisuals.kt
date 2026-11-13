package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.EmotionCategory
import com.example.ui.theme.NpcArchetype
import com.example.ui.theme.NpcPortraitSpec
import com.example.ui.theme.UniverseVisualPalette
import com.example.ui.theme.WeatherMood
import com.example.ui.theme.resolveNpcPortraitSpec
import com.example.ui.theme.resolveSceneBackdropRes

@Composable
fun SceneAtmosphereBanner(
    universe: String,
    location: String,
    timeLabel: String,
    weatherMood: WeatherMood,
    palette: UniverseVisualPalette,
    modifier: Modifier = Modifier
) {
    val backdropRes = resolveSceneBackdropRes(
        universe = universe,
        location = location,
        weatherMood = weatherMood
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(142.dp)
            .testTag("scene_atmosphere_banner")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = backdropRes,
                animationSpec = tween(durationMillis = 550),
                label = "scene_backdrop_crossfade"
            ) { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Фон сцены: ${location.ifBlank { universe }}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Universe-tinted cinematic color grading + legibility scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.bannerScrimTop,
                                palette.atmosphericTint.copy(alpha = 0.42f),
                                palette.bannerScrimBottom
                            )
                        )
                    )
            )

            WeatherCanvasOverlay(
                weatherMood = weatherMood,
                accentColor = palette.accentPrimary,
                modifier = Modifier.fillMaxSize()
            )

            // Top row: universe genre tag + weather mood pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, palette.accentPrimary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = palette.archetype.displayTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF6EFE4),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = weatherIconFor(weatherMood),
                            contentDescription = weatherMood.russianLabel,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = weatherMood.russianLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF6EFE4)
                        )
                    }
                }
            }

            // Bottom row: Location title + Time pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = location.ifBlank { "Неизвестная локация" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFFBF4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (timeLabel.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFDCD3C4),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE7DFD1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCanvasOverlay(
    weatherMood: WeatherMood,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather_phase"
    )

    Canvas(modifier = modifier) {
        when (weatherMood) {
            WeatherMood.RAIN, WeatherMood.STORM -> drawRainStreaks(
                phase = phase,
                heavy = weatherMood == WeatherMood.STORM
            )
            WeatherMood.FOG -> drawFogLayers(phase = phase)
            WeatherMood.SNOW -> drawSnowParticles(phase = phase)
            WeatherMood.INDOOR_CANDLE, WeatherMood.DAWN -> drawWarmEmbers(
                phase = phase,
                color = accentColor
            )
            WeatherMood.NEON_NIGHT -> drawNeonShimmer(
                phase = phase,
                accentColor = accentColor
            )
            WeatherMood.NIGHT -> drawNightStars(phase = phase)
            WeatherMood.DAY -> Unit
        }
    }
}

private fun DrawScope.drawRainStreaks(phase: Float, heavy: Boolean) {
    val count = if (heavy) 24 else 16
    val dropColor = Color(0xFFD7E6F5).copy(alpha = if (heavy) 0.34f else 0.24f)
    for (i in 0 until count) {
        val seedX = ((i * 73 + 19) % 100) / 100f
        val seedOffset = ((i * 37) % 100) / 100f
        val progress = (phase + seedOffset) % 1f
        val startX = seedX * size.width - progress * 22f
        val startY = progress * (size.height + 40f) - 30f
        val len = if (heavy) 22f else 16f
        drawLine(
            color = dropColor,
            start = Offset(startX, startY),
            end = Offset(startX - 5f, startY + len),
            strokeWidth = if (heavy) 2.2f else 1.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawFogLayers(phase: Float) {
    val drift = kotlin.math.sin(phase * 2f * Math.PI).toFloat() * 18f
    drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = size.width * 0.42f,
        center = Offset(size.width * 0.28f + drift, size.height * 0.52f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.07f),
        radius = size.width * 0.38f,
        center = Offset(size.width * 0.76f - drift, size.height * 0.65f)
    )
}

private fun DrawScope.drawSnowParticles(phase: Float) {
    val flakeColor = Color.White.copy(alpha = 0.45f)
    for (i in 0 until 18) {
        val seedX = ((i * 61 + 11) % 100) / 100f
        val seedOffset = ((i * 29) % 100) / 100f
        val progress = (phase + seedOffset) % 1f
        val sway = kotlin.math.sin((progress + i) * 6.28f).toFloat() * 10f
        drawCircle(
            color = flakeColor,
            radius = if (i % 3 == 0) 3.2f else 2.2f,
            center = Offset(seedX * size.width + sway, progress * size.height)
        )
    }
}

private fun DrawScope.drawWarmEmbers(phase: Float, color: Color) {
    val emberColor = color.copy(alpha = 0.42f)
    for (i in 0 until 12) {
        val seedX = ((i * 83 + 17) % 100) / 100f
        val seedOffset = ((i * 41) % 100) / 100f
        val progress = (phase + seedOffset) % 1f
        val y = size.height - progress * size.height
        val x = seedX * size.width + kotlin.math.sin(progress * 6.28f + i).toFloat() * 8f
        drawCircle(
            color = emberColor,
            radius = if (i % 2 == 0) 2.8f else 2f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawNeonShimmer(phase: Float, accentColor: Color) {
    val pulse = (0.14f + 0.10f * kotlin.math.sin(phase * 6.28f).toFloat()).coerceIn(0.05f, 0.28f)
    drawLine(
        color = accentColor.copy(alpha = pulse),
        start = Offset(0f, size.height * 0.24f),
        end = Offset(size.width, size.height * 0.24f),
        strokeWidth = 2f
    )
    drawRainStreaks(phase = phase, heavy = false)
}

private fun DrawScope.drawNightStars(phase: Float) {
    for (i in 0 until 14) {
        val x = (((i * 67 + 23) % 100) / 100f) * size.width
        val y = (((i * 43 + 7) % 55) / 100f) * size.height
        val alpha = 0.18f + 0.22f * kotlin.math.abs(kotlin.math.sin((phase + i * 0.13f) * 3.14f)).toFloat()
        drawCircle(
            color = Color(0xFFF6F1DE).copy(alpha = alpha),
            radius = if (i % 4 == 0) 2.5f else 1.6f,
            center = Offset(x, y)
        )
    }
}

@Composable
fun NpcPortraitAvatar(
    name: String,
    role: String,
    universe: String,
    explicitArchetype: String,
    emotionCategory: String,
    intensity: Long,
    palette: UniverseVisualPalette,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val category = EmotionCategory.fromKey(emotionCategory)
    val (emotionContainer, emotionContent) = emotionColorsFor(category)
    val animatedRingColor by animateColorAsState(
        targetValue = emotionContent,
        animationSpec = tween(durationMillis = 350),
        label = "npc_ring_color"
    )
    val portraitSpec = resolveNpcPortraitSpec(
        name = name,
        role = role,
        universe = universe,
        explicitArchetype = explicitArchetype,
        palette = palette
    )

    val ringAlpha = (0.48f + (intensity.coerceIn(1L, 5L) * 0.10f)).coerceIn(0.45f, 0.98f)

    Box(
        modifier = modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            animatedRingColor.copy(alpha = ringAlpha),
                            palette.accentPrimary.copy(alpha = 0.72f),
                            animatedRingColor.copy(alpha = ringAlpha)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            when (portraitSpec) {
                is NpcPortraitSpec.PhotoPortrait -> {
                    Image(
                        painter = painterResource(id = portraitSpec.resId),
                        contentDescription = "Портрет NPC: $name",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        portraitSpec.tintOverlay,
                                        animatedRingColor.copy(alpha = 0.22f)
                                    )
                                )
                            )
                    )
                }

                is NpcPortraitSpec.SilhouetteEmblem -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        portraitSpec.backgroundStart,
                                        portraitSpec.backgroundEnd
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val silhouetteIcon = when (portraitSpec.archetype) {
                            NpcArchetype.MERCHANT -> Icons.Outlined.Storefront
                            NpcArchetype.CREATURE -> Icons.Outlined.Pets
                            else -> Icons.Outlined.Person
                        }
                        Icon(
                            imageVector = silhouetteIcon,
                            contentDescription = "Силуэт NPC: $name",
                            tint = Color(0xFFF3ECE0).copy(alpha = 0.90f),
                            modifier = Modifier.size(size * 0.52f)
                        )
                    }
                }
            }
        }

        // Corner emotion indicator badge
        Surface(
            color = emotionContainer,
            contentColor = emotionContent,
            shape = CircleShape,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = emotionIconFor(category),
                    contentDescription = category.russianDefault,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

fun weatherIconFor(mood: WeatherMood): ImageVector = when (mood) {
    WeatherMood.RAIN -> Icons.Outlined.WaterDrop
    WeatherMood.STORM -> Icons.Outlined.ElectricBolt
    WeatherMood.FOG -> Icons.Outlined.Cloud
    WeatherMood.NIGHT -> Icons.Outlined.Nightlight
    WeatherMood.DAWN -> Icons.Outlined.WbTwilight
    WeatherMood.DAY -> Icons.Outlined.WbSunny
    WeatherMood.SNOW -> Icons.Outlined.AcUnit
    WeatherMood.INDOOR_CANDLE -> Icons.Outlined.LocalFireDepartment
    WeatherMood.NEON_NIGHT -> Icons.Outlined.Flare
}
