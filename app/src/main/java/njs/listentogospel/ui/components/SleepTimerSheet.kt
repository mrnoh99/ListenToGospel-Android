package njs.listentogospel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import njs.listentogospel.viewmodel.SleepTimerOption

private val timedOptions = listOf(
    SleepTimerOption.THIRTY,
    SleepTimerOption.SIXTY,
    SleepTimerOption.NINETY,
    SleepTimerOption.ONE_TWENTY,
    SleepTimerOption.CONTINUOUS
)

private val sleepTimerTitleFontSize = 18.sp

@Composable
fun SleepTimerSheet(
    selectedOption: SleepTimerOption,
    onSelect: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 220.dp, max = 260.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "수면 타이머",
                    fontSize = sleepTimerTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                timedOptions.forEach { option ->
                    SleepTimerOptionRow(
                        label = optionTitle(option, selectedOption),
                        isSelected = selectedOption == option,
                        fontSize = sleepTimerTitleFontSize,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerOptionRow(
    label: String,
    isSelected: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        fontSize = fontSize,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
}

private fun optionTitle(option: SleepTimerOption, selectedOption: SleepTimerOption): String {
    return if (option == selectedOption) "${option.title} ✓" else option.title
}
