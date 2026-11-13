package com.example.core

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

class RpgHaptics(
    private val enabled: Boolean,
    private val view: View,
    private val hapticFeedback: HapticFeedback
) {
    fun tap() {
        if (!enabled) return
        if (!view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun impactConfirm() {
        if (!enabled) return
        if (!view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun butterflyShift() {
        if (!enabled) return
        if (!view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberRpgHaptics(enabled: Boolean): RpgHaptics {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    return remember(enabled, view, hapticFeedback) {
        RpgHaptics(
            enabled = enabled,
            view = view,
            hapticFeedback = hapticFeedback
        )
    }
}
