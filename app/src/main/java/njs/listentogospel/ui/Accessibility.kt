package njs.listentogospel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import njs.listentogospel.util.AppHaptic
import njs.listentogospel.util.HapticFeedback

/**
 * True when TalkBack touch exploration should drive the UI (semantic double-tap).
 * False = raw single-finger tap via [directTapClickable] (no accessibility click nodes).
 */
val LocalAppAccessibilityEnabled = compositionLocalOf { false }

fun Modifier.hideFromAccessibilityTree(): Modifier = clearAndSetSemantics { }

private fun buildAccessibilityLabel(label: String, hint: String? = null): String {
    return buildString {
        append(label)
        hint?.let { append(", $it") }
    }
}

/** Single-tap handler that does not register Compose foundation clickable accessibility actions. */
fun Modifier.directTapClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = pointerInput(enabled, onClick) {
    if (!enabled) return@pointerInput
    detectTapGestures(onTap = { onClick() })
}

/**
 * TalkBack on: semantic click. TalkBack off: [directTapClickable] only (true single tap).
 */
@Composable
fun Modifier.accessibleHapticClickable(
    label: String,
    hint: String? = null,
    stateDescription: String? = null,
    actionLabel: String = "재생",
    kind: AppHaptic = AppHaptic.Selection,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val view = LocalView.current
    val useTalkBack = LocalAppAccessibilityEnabled.current
    val performClick = {
        HapticFeedback.perform(view, kind)
        onClick()
    }

    return if (useTalkBack) {
        semantics(mergeDescendants = true) {
            contentDescription = label
            hint?.let { this.stateDescription = it }
            stateDescription?.let { this.stateDescription = it }
            onClick(actionLabel) {
                performClick()
                true
            }
        }
    } else {
        directTapClickable(enabled = enabled, onClick = performClick)
    }
}

@Composable
fun Modifier.accessibleSurfaceLabel(
    label: String,
    hint: String? = null,
    stateDescription: String? = null
): Modifier {
    if (!LocalAppAccessibilityEnabled.current) return this
    return semantics(mergeDescendants = true) {
        contentDescription = buildAccessibilityLabel(label, hint)
        stateDescription?.let { this.stateDescription = it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleClickSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    accessibilityLabel: String? = null,
    accessibilityHint: String? = null,
    content: @Composable () -> Unit
) {
    val useTalkBack = LocalAppAccessibilityEnabled.current
    val labelModifier = if (useTalkBack && accessibilityLabel != null) {
        Modifier.accessibleSurfaceLabel(accessibilityLabel, accessibilityHint)
    } else {
        Modifier
    }

    if (useTalkBack) {
        Surface(
            onClick = onClick,
            modifier = modifier.then(labelModifier),
            shape = shape,
            color = color,
            shadowElevation = shadowElevation,
            border = border
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier
                .then(labelModifier)
                .directTapClickable(onClick = onClick),
            shape = shape,
            color = color,
            shadowElevation = shadowElevation,
            border = border
        ) {
            content()
        }
    }
}

/** Kept for call sites; mode is driven from MainActivity via [LocalAppAccessibilityEnabled]. */
@Composable
fun TrackAccessibilityState(content: @Composable () -> Unit) {
    content()
}
