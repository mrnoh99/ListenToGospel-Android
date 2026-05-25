package njs.listentogospel.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import njs.listentogospel.util.AppHaptic
import njs.listentogospel.util.HapticFeedback

@Composable
fun rememberHaptic(): (AppHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { kind -> HapticFeedback.perform(view, kind) } }
}

fun hapticClick(haptic: (AppHaptic) -> Unit, kind: AppHaptic = AppHaptic.Selection, onClick: () -> Unit): () -> Unit {
    return {
        haptic(kind)
        onClick()
    }
}

@Composable
fun Modifier.hapticClickable(
    kind: AppHaptic = AppHaptic.Selection,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val view = LocalView.current
    return clickable(enabled = enabled, onClick = {
        HapticFeedback.perform(view, kind)
        onClick()
    })
}
