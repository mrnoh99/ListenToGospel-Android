package njs.listentogospel.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

fun Modifier.hideFromAccessibilityTree(): Modifier = clearAndSetSemantics { }

fun Modifier.mergedButtonSemantics(
    label: String,
    hint: String? = null,
    stateDescription: String? = null
): Modifier = composed {
    semantics(mergeDescendants = true) {
        contentDescription = buildString {
            append(label)
            hint?.let { append(", $it") }
        }
        stateDescription?.let { this.stateDescription = it }
        role = Role.Button
    }
}
