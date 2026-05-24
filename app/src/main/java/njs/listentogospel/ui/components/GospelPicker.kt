package njs.listentogospel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import njs.listentogospel.ui.mergedButtonSemantics
import njs.listentogospel.util.AccessibilitySupport
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import njs.listentogospel.model.Gospel
import njs.listentogospel.ui.hapticClickable
import njs.listentogospel.ui.theme.AppControlLayout
import njs.listentogospel.util.AppHaptic

@Composable
fun GospelPicker(
    selectedGospel: Gospel,
    onSelect: (Gospel) -> Unit,
    modifier: Modifier = Modifier
) {
    val gospels = Gospel.values()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppControlLayout.gospelGridSpacing)
    ) {
        for (rowStart in gospels.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppControlLayout.gospelGridSpacing)
            ) {
                for (index in rowStart until minOf(rowStart + 2, gospels.size)) {
                    val gospel = gospels[index]
                    GospelPickerCell(
                        gospel = gospel,
                        isSelected = gospel == selectedGospel,
                        onClick = { onSelect(gospel) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GospelPickerCell(
    gospel: Gospel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppControlLayout.barCornerRadius)
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .mergedButtonSemantics(
                label = AccessibilitySupport.gospelPickerLabel(gospel, isSelected)
            )
            .height(AppControlLayout.barHeight)
            .clip(shape)
            .background(backgroundColor)
            .hapticClickable(kind = AppHaptic.Selection, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gospel.shortName,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
