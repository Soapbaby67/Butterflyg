package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ButterflyEvent
import com.example.data.model.Campaign
import com.example.ui.theme.UniverseVisualPalette

internal enum class ButterflyImpactTier(
    val key: String,
    val labelRu: String,
    val branchOffsetFraction: Float
) {
    MINOR("minor", "Лёгкий сдвиг", 0.36f),
    MAJOR("major", "Серьёзный поворот", 0.64f),
    CRITICAL("critical", "Точка невозврата", 0.88f);

    companion object {
        fun fromImpact(raw: String): ButterflyImpactTier = when (raw.trim().lowercase()) {
            "critical" -> CRITICAL
            "major" -> MAJOR
            else -> MINOR
        }
    }
}

/**
 * Pure layout geometry helper for Butterfly Effect branching nodes so unit tests can verify
 * deterministic divergence calculations without needing Compose UI rendering.
 */
object ButterflyBranchGeometry {
    fun computeDivergenceOffset(impactLevel: String): Float =
        ButterflyImpactTier.fromImpact(impactLevel).branchOffsetFraction

    fun tierLabelRu(impactLevel: String): String =
        ButterflyImpactTier.fromImpact(impactLevel).labelRu
}

/**
 * Interactive vector Butterfly Effect branching map rendered on Compose Canvas with zero network traffic.
 */
@Composable
fun ButterflyBranchMapCard(
    campaign: Campaign,
    palette: UniverseVisualPalette,
    isBusy: Boolean = false,
    onForkAtTurn: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showGraphMode by rememberSaveable { mutableStateOf(true) }
    var expandedIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val sortedEvents = remember(campaign.butterflyEvents) {
        campaign.butterflyEvents.sortedBy { it.turnNumber }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("butterfly_branch_map_root")
    ) {
        // Canon divergence header card
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, palette.accentSecondary.copy(alpha = 0.38f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Отклонение от канона",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${campaign.canonDeviationPercent.coerceIn(0L, 100L)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { campaign.canonDeviationPercent.coerceIn(0L, 100L) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = palette.accentPrimary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                if (campaign.storySummary.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    Text(
                        text = campaign.storySummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // View mode switcher: Visual Branch Graph vs Compact List
        if (sortedEvents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showGraphMode,
                    onClick = { showGraphMode = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Карта развилок") },
                    modifier = Modifier.testTag("butterfly_mode_graph")
                )
                FilterChip(
                    selected = !showGraphMode,
                    onClick = { showGraphMode = false },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ViewList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Список (${sortedEvents.size})") },
                    modifier = Modifier.testTag("butterfly_mode_list")
                )
            }
        }

        if (sortedEvents.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Линия канона пока не нарушена",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Как только герой примет поворотное решение, здесь вырастет ветвь альтернативной реальности, от которой можно будет создать отдельную хронику.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (showGraphMode) {
            // Legend header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ось канона",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ветвь героя • нажмите на узел",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentPrimary
                )
            }

            // Origin root node
            BranchOriginRow(
                universe = campaign.universe,
                canonColor = MaterialTheme.colorScheme.outline,
                accentColor = palette.accentSecondary
            )

            // Branching event rows with continuous Canvas spline
            sortedEvents.forEachIndexed { idx, event ->
                val tier = ButterflyImpactTier.fromImpact(event.impactLevel)
                val prevTier = if (idx > 0) {
                    ButterflyImpactTier.fromImpact(sortedEvents[idx - 1].impactLevel)
                } else {
                    null
                }
                val isLast = idx == sortedEvents.lastIndex
                val isExpanded = expandedIndex == idx || (expandedIndex == null && isLast)

                ButterflyGraphNodeRow(
                    event = event,
                    tier = tier,
                    previousOffsetFraction = prevTier?.branchOffsetFraction ?: 0.14f,
                    isLast = isLast,
                    isExpanded = isExpanded,
                    palette = palette,
                    isBusy = isBusy,
                    onToggleExpand = {
                        expandedIndex = if (isExpanded) -1 else idx
                    },
                    onForkAtTurn = onForkAtTurn
                )
            }
        } else {
            // Compact list fallback view
            sortedEvents.reversed().forEach { event ->
                val tier = ButterflyImpactTier.fromImpact(event.impactLevel)
                val tierColor = colorForImpactTier(tier, palette)
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, tierColor.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ход ${event.turnNumber} • ${tier.labelRu}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tierColor
                            )
                            if (onForkAtTurn != null) {
                                TextButton(
                                    onClick = { onForkAtTurn(event.turnNumber) },
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CallSplit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ветка", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = event.consequence,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchOriginRow(
    universe: String,
    canonColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(68.dp)
                .fillMaxHeight()
                .height(34.dp)
        ) {
            val spineX = size.width * 0.18f
            val centerY = size.height * 0.5f
            drawLine(
                color = canonColor.copy(alpha = 0.55f),
                start = Offset(spineX, centerY),
                end = Offset(spineX, size.height),
                strokeWidth = 2.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawCircle(
                color = accentColor,
                radius = 5.5.dp.toPx(),
                center = Offset(spineX, centerY)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Точка входа • Ход 1",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
            Text(
                text = universe.ifBlank { "Исходный канон мира" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ButterflyGraphNodeRow(
    event: ButterflyEvent,
    tier: ButterflyImpactTier,
    previousOffsetFraction: Float,
    isLast: Boolean,
    isExpanded: Boolean,
    palette: UniverseVisualPalette,
    isBusy: Boolean,
    onToggleExpand: () -> Unit,
    onForkAtTurn: ((Long) -> Unit)?
) {
    val tierColor = colorForImpactTier(tier, palette)
    val canonSpineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val surfaceBg = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("butterfly_node_turn_${event.turnNumber}"),
        verticalAlignment = Alignment.Top
    ) {
        // Left Compose Canvas branch gutter
        Canvas(
            modifier = Modifier
                .width(68.dp)
                .fillMaxHeight()
        ) {
            val spineX = size.width * 0.18f
            val prevX = size.width * (0.18f + previousOffsetFraction * 0.68f)
            val nodeX = size.width * (0.18f + tier.branchOffsetFraction * 0.68f)
            val nodeY = 24.dp.toPx().coerceAtMost(size.height * 0.5f)

            // Dashed canon spine
            drawLine(
                color = canonSpineColor,
                start = Offset(spineX, 0f),
                end = Offset(spineX, size.height),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // Smooth player branch curve from previous node to current node
            val incomingPath = Path().apply {
                moveTo(prevX, 0f)
                cubicTo(
                    prevX, nodeY * 0.55f,
                    nodeX, nodeY * 0.45f,
                    nodeX, nodeY
                )
            }
            drawPath(
                path = incomingPath,
                color = tierColor.copy(alpha = 0.85f),
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Continuation downward if not the last node
            if (!isLast) {
                drawLine(
                    color = tierColor.copy(alpha = 0.65f),
                    start = Offset(nodeX, nodeY),
                    end = Offset(nodeX, size.height),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Connector tick from canon spine on major/critical shifts
            if (tier != ButterflyImpactTier.MINOR) {
                drawLine(
                    color = tierColor.copy(alpha = 0.35f),
                    start = Offset(spineX, nodeY),
                    end = Offset(nodeX, nodeY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )
            }

            // Node glyph (distinct shape per impact tier for color-independent legibility)
            when (tier) {
                ButterflyImpactTier.MINOR -> {
                    drawCircle(color = surfaceBg, radius = 6.dp.toPx(), center = Offset(nodeX, nodeY))
                    drawCircle(
                        color = tierColor,
                        radius = 6.dp.toPx(),
                        center = Offset(nodeX, nodeY),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }
                ButterflyImpactTier.MAJOR -> {
                    drawCircle(
                        color = tierColor.copy(alpha = 0.24f),
                        radius = 11.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                    drawCircle(color = tierColor, radius = 6.5.dp.toPx(), center = Offset(nodeX, nodeY))
                }
                ButterflyImpactTier.CRITICAL -> {
                    val r = 8.5.dp.toPx()
                    drawCircle(
                        color = tierColor.copy(alpha = 0.26f),
                        radius = 14.dp.toPx(),
                        center = Offset(nodeX, nodeY)
                    )
                    val diamond = Path().apply {
                        moveTo(nodeX, nodeY - r)
                        lineTo(nodeX + r, nodeY)
                        lineTo(nodeX, nodeY + r)
                        lineTo(nodeX - r, nodeY)
                        close()
                    }
                    drawPath(path = diamond, color = tierColor)
                    drawPath(
                        path = diamond,
                        color = surfaceBg,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
        }

        // Interactive event card on the right
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = if (isExpanded) 1.4.dp else 1.dp,
                    color = if (isExpanded) tierColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onToggleExpand)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Ход ${event.turnNumber} • ${tier.labelRu}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tierColor
                    )
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = event.consequence,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onForkAtTurn != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                TextButton(
                                    onClick = { onForkAtTurn(event.turnNumber) },
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("butterfly_fork_turn_${event.turnNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CallSplit,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Создать ветку от хода ${event.turnNumber}",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun colorForImpactTier(
    tier: ButterflyImpactTier,
    palette: UniverseVisualPalette
): Color = when (tier) {
    ButterflyImpactTier.MINOR -> palette.accentSecondary
    ButterflyImpactTier.MAJOR -> palette.accentPrimary
    ButterflyImpactTier.CRITICAL -> palette.choiceForceColor
}
