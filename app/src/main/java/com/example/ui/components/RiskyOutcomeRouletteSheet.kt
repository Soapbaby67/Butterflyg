package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.LocalRpgAudioEngine
import com.example.core.RpgHaptics
import com.example.core.SfxEvent
import com.example.data.model.HeroSkills
import com.example.data.model.InventoryItem
import com.example.data.model.RiskModifierSummary
import com.example.data.model.RiskOutcome
import com.example.data.model.RiskOutcomeTier
import com.example.data.model.RiskyChoiceResolver
import com.example.data.model.TurnChoice
import com.example.ui.theme.UniverseVisualPalette
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskyOutcomeRouletteSheet(
    choice: TurnChoice,
    activeNpcName: String?,
    skills: HeroSkills = HeroSkills(),
    inventoryItems: List<InventoryItem> = emptyList(),
    palette: UniverseVisualPalette,
    haptics: RpgHaptics,
    onDismiss: () -> Unit,
    onOutcomeResolved: (RiskOutcome, RiskModifierSummary) -> Unit
) {
    val evaluation = remember(choice, activeNpcName, skills, inventoryItems) {
        RiskyChoiceResolver.resolveOutcomesWithModifiers(
            choice = choice,
            activeNpcName = activeNpcName,
            skills = skills,
            inventoryItems = inventoryItems
        )
    }
    val outcomes = evaluation.outcomes
    val modifierSummary = evaluation.modifier
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val audio = LocalRpgAudioEngine.current

    var isSpinning by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableIntStateOf(-1) }
    var winningIndex by remember { mutableIntStateOf(-1) }
    var winningOutcome by remember { mutableStateOf<RiskOutcome?>(null) }
    var spinTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(spinTrigger) {
        if (spinTrigger <= 0 || outcomes.isEmpty()) return@LaunchedEffect
        isSpinning = true
        winningIndex = -1
        winningOutcome = null

        val (targetIdx, selectedOutcome) = RiskyChoiceResolver.pickWeightedOutcome(outcomes)
        val totalCycles = 3 * outcomes.size + targetIdx
        var cursor = 0
        for (step in 0..totalCycles) {
            highlightedIndex = cursor % outcomes.size
            audio?.playSfx(SfxEvent.ROULETTE_TICK)
            haptics.tap()
            val progress = step.toFloat() / totalCycles.coerceAtLeast(1)
            val delayMs = when {
                progress < 0.45f -> 58L
                progress < 0.72f -> 95L
                progress < 0.88f -> 145L
                else -> 225L
            }
            delay(delayMs)
            cursor++
        }

        highlightedIndex = targetIdx
        winningIndex = targetIdx
        winningOutcome = selectedOutcome
        isSpinning = false

        val tier = RiskOutcomeTier.fromKey(selectedOutcome.tier)
        when (tier) {
            RiskOutcomeTier.TRIUMPH -> {
                audio?.playSfx(SfxEvent.OUTCOME_TRIUMPH)
                haptics.impactConfirm()
            }
            RiskOutcomeTier.MIXED -> {
                audio?.playSfx(SfxEvent.OUTCOME_MIXED)
                haptics.impactConfirm()
            }
            RiskOutcomeTier.SETBACK -> {
                audio?.playSfx(SfxEvent.OUTCOME_SETBACK)
                haptics.impactConfirm()
            }
            RiskOutcomeTier.TWIST -> {
                audio?.playSfx(SfxEvent.OUTCOME_TWIST)
                haptics.butterflyShift()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSpinning) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = palette.choiceForceColor.copy(alpha = 0.16f),
                    contentColor = palette.choiceForceColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Casino,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Рискованная развилка",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("fate_roulette_title")
                    )
                    Text(
                        text = choice.approach.ifBlank { "Рискованный шаг" },
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.choiceForceColor
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = choice.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Active Skill & Equipment Modifier Banner
            val modifierPositive = modifierSummary.setbackDelta <= 0L
            val modColor = if (modifierPositive) palette.healthHigh else palette.healthLow
            Surface(
                color = modColor.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, modColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fate_modifier_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = modColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Бонус навыка и снаряжения",
                            style = MaterialTheme.typography.labelMedium,
                            color = modColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = modifierSummary.summaryRu,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = when {
                    isSpinning -> "Судьба крутит колесо — выбирается случайный исход…"
                    winningOutcome != null -> "Выпавший исход зафиксирован:"
                    else -> "Вероятности с учётом ваших характеристик и предметов:"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            outcomes.forEachIndexed { index, outcome ->
                val isHighlighted = highlightedIndex == index
                val isWinner = winningIndex == index
                val tier = RiskOutcomeTier.fromKey(outcome.tier)
                val (tierColor, tierIcon) = resolveTierVisuals(tier, palette)
                val scale by animateFloatAsState(
                    targetValue = when {
                        isWinner -> 1.02f
                        isHighlighted -> 1.01f
                        else -> 1f
                    },
                    animationSpec = spring(),
                    label = "outcome_scale_$index"
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isWinner -> tierColor
                        isHighlighted -> palette.accentPrimary
                        winningIndex >= 0 -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        else -> tierColor.copy(alpha = 0.35f)
                    },
                    label = "outcome_border_$index"
                )
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isWinner -> tierColor.copy(alpha = 0.18f)
                        isHighlighted -> palette.accentPrimary.copy(alpha = 0.14f)
                        winningIndex >= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.background
                    },
                    label = "outcome_bg_$index"
                )

                Surface(
                    color = bgColor,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        width = if (isWinner || isHighlighted) 2.dp else 1.dp,
                        color = borderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .testTag("fate_outcome_card_$index")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWinner) Icons.Outlined.CheckCircle else tierIcon,
                                    contentDescription = null,
                                    tint = tierColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${tier.labelRu}: ${outcome.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = tierColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "${outcome.weight}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = outcome.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSpinning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    color = palette.accentPrimary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (winningOutcome == null) {
                    OutlinedButton(
                        onClick = {
                            audio?.playSfx(SfxEvent.UI_TAP)
                            onDismiss()
                        },
                        enabled = !isSpinning,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = { spinTrigger++ },
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.choiceForceColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("spin_fate_roulette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSpinning) "Крутится…" else "Испытать судьбу")
                    }
                } else {
                    val resolved = winningOutcome!!
                    Button(
                        onClick = {
                            audio?.playSfx(SfxEvent.SEND_ACTION)
                            onOutcomeResolved(resolved, modifierSummary)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accentPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_fate_outcome_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Продолжить: ${resolved.label}")
                    }
                }
            }
        }
    }
}

private fun resolveTierVisuals(
    tier: RiskOutcomeTier,
    palette: UniverseVisualPalette
): Pair<Color, ImageVector> = when (tier) {
    RiskOutcomeTier.TRIUMPH -> palette.healthHigh to Icons.Outlined.AutoAwesome
    RiskOutcomeTier.MIXED -> palette.accentSecondary to Icons.Outlined.Bolt
    RiskOutcomeTier.SETBACK -> palette.healthLow to Icons.Outlined.WarningAmber
    RiskOutcomeTier.TWIST -> palette.choiceWildcardColor to Icons.Outlined.Casino
}
