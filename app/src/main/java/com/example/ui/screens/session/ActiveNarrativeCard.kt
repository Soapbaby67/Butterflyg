package com.example.ui.screens.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CampaignTurn
import com.example.data.model.TurnChoice
import com.example.ui.theme.UniverseVisualPalette
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ActiveNarrativeWindow(
    turn: CampaignTurn,
    accentColor: Color,
    secondaryColor: Color,
    isSpeakingThisTurn: Boolean,
    isTtsAvailable: Boolean,
    isBusy: Boolean,
    onToggleTts: () -> Unit,
    onRerollScene: () -> Unit,
    onEditLastAction: () -> Unit
) {
    val narratorBubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 20.dp
    )
    val playerBubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (turn.turnNumber > 1L && turn.playerAction.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.24f),
                    shape = playerBubbleShape,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                    modifier = Modifier.widthIn(max = 520.dp)
                ) {
                    Text(
                        text = turn.playerAction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = narratorBubbleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.36f)),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("narrative_window")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.16f),
                        contentColor = accentColor,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Мастер • Глава ${turn.turnNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (isTtsAvailable) {
                        Surface(
                            color = if (isSpeakingThisTurn) {
                                accentColor.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSpeakingThisTurn) accentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onToggleTts)
                                .testTag("tts_speak_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThisTurn) {
                                        Icons.Outlined.StopCircle
                                    } else {
                                        Icons.Outlined.RecordVoiceOver
                                    },
                                    contentDescription = if (isSpeakingThisTurn) {
                                        "Остановить озвучку"
                                    } else {
                                        "Озвучить сцену"
                                    },
                                    tint = if (isSpeakingThisTurn) accentColor else secondaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSpeakingThisTurn) "Стоп" else "Озвучить",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSpeakingThisTurn) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Повествование",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor
                        )
                    }
                }

                EditorialNarrativeText(
                    narrative = turn.narrative,
                    dropCapColor = accentColor
                )

                // Reroll & Edit Last Action bar for non-opening turns
                if (turn.turnNumber > 1L) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onRerollScene,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("reroll_scene_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Переиграть сцену",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onEditLastAction,
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("edit_last_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Изменить свой ход",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditorialNarrativeText(
    narrative: String,
    dropCapColor: Color
) {
    val trimmed = narrative.trim()
    val bodyStyle = MaterialTheme.typography.bodyLarge
    if (trimmed.length >= 2) {
        val annotated = remember(trimmed, dropCapColor, bodyStyle.fontSize) {
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = (bodyStyle.fontSize.value * 1.65f).sp,
                        fontWeight = FontWeight.Bold,
                        color = dropCapColor
                    )
                ) {
                    append(trimmed.substring(0, 1))
                }
                append(trimmed.substring(1))
            }
        }
        SelectionContainer {
            Text(
                text = annotated,
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Text(
            text = trimmed,
            style = bodyStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun GeneratingNarrativeWindow(
    pendingAction: String,
    streamingNarrativeFlow: StateFlow<String>,
    accentColor: Color
) {
    val narratorBubbleShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 20.dp
    )
    val playerBubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("generating_turn_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.24f),
                shape = playerBubbleShape,
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Text(
                    text = pendingAction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shape = narratorBubbleShape,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                // Isolated collector so high-frequency streaming chunks recompose ONLY this text leaf
                StreamingNarrativeBox(
                    streamingNarrativeFlow = streamingNarrativeFlow,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun StreamingNarrativeBox(
    streamingNarrativeFlow: StateFlow<String>,
    accentColor: Color
) {
    val streamingNarrative by streamingNarrativeFlow.collectAsStateWithLifecycle()
    if (streamingNarrative.isNotBlank()) {
        Text(
            text = streamingNarrative,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("streaming_narrative_text")
        )
    } else {
        Text(
            text = "Мастер плетёт нити событий и оценивает последствия…",
            style = MaterialTheme.typography.bodySmall,
            color = accentColor
        )
    }
}

@Composable
internal fun ChoiceActionCard(
    choice: TurnChoice,
    isRisky: Boolean,
    palette: UniverseVisualPalette,
    onClick: () -> Unit
) {
    val (approachColor, approachIcon) = resolveApproachVisuals(choice.approach, palette)

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (isRisky) 1.3.dp else 1.dp,
            color = if (isRisky) {
                palette.choiceForceColor.copy(alpha = 0.52f)
            } else {
                approachColor.copy(alpha = 0.42f)
            }
        ),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("choice_button_${choice.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .minimumInteractiveComponentSize()
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(approachColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (choice.approach.isNotBlank()) {
                        Surface(
                            color = approachColor.copy(alpha = 0.16f),
                            contentColor = approachColor,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = approachIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = choice.approach,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (isRisky) {
                        Surface(
                            color = palette.choiceForceColor.copy(alpha = 0.16f),
                            contentColor = palette.choiceForceColor,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("risky_choice_badge_${choice.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Casino,
                                    contentDescription = "Рискованный выбор с развилками",
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Риск • развилки",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = choice.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun resolveApproachVisuals(
    approach: String,
    palette: UniverseVisualPalette
): Pair<Color, ImageVector> {
    val lower = approach.lowercase()
    return when {
        lower.contains("роман") || lower.contains("флирт") || lower.contains("нежн") ->
            palette.choiceDiplomacyColor to Icons.Outlined.FavoriteBorder
        lower.contains("забот") || lower.contains("поддерж") || lower.contains("эмоц") ->
            palette.healthHigh to Icons.Outlined.Spa
        lower.contains("быт") || lower.contains("рутин") || lower.contains("уют") ->
            palette.accentSecondary to Icons.Outlined.LocalCafe
        lower.contains("дерз") || lower.contains("вызов") || lower.contains("риск") ->
            palette.choiceForceColor to Icons.Outlined.Whatshot
        lower.contains("сил") || lower.contains("атак") || lower.contains("бой") ->
            palette.choiceForceColor to Icons.Outlined.Bolt
        lower.contains("диплом") || lower.contains("перегов") || lower.contains("убеж") ->
            palette.choiceDiplomacyColor to Icons.Outlined.Forum
        lower.contains("скрыт") || lower.contains("тайн") || lower.contains("обман") ||
            lower.contains("тень") ->
            palette.choiceStealthColor to Icons.Outlined.VisibilityOff
        lower.contains("нестандарт") || lower.contains("хитр") || lower.contains("маг") ->
            palette.choiceWildcardColor to Icons.Outlined.AutoAwesome
        else ->
            palette.accentPrimary to Icons.Outlined.Explore
    }
}
