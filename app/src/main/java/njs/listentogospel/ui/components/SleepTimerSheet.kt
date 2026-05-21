package njs.listentogospel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import njs.listentogospel.viewmodel.SleepTimerOption

private val timedOptions = listOf(
    SleepTimerOption.THIRTY,
    SleepTimerOption.SIXTY,
    SleepTimerOption.NINETY,
    SleepTimerOption.ONE_TWENTY
)

@Composable
fun SleepTimerSheet(
    selectedOption: SleepTimerOption,
    onSelect: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "수면 타이머",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "타이머 시간을 정합니다",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                timedOptions.forEach { option ->
                    TextButton(
                        onClick = { onSelect(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = optionTitle(option, selectedOption),
                            color = if (selectedOption == option) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (selectedOption == option) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                TextButton(
                    onClick = { onSelect(SleepTimerOption.CONTINUOUS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = optionTitle(SleepTimerOption.CONTINUOUS, selectedOption),
                        color = if (selectedOption == SleepTimerOption.CONTINUOUS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (selectedOption == SleepTimerOption.CONTINUOUS) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun optionTitle(option: SleepTimerOption, selectedOption: SleepTimerOption): String {
    return if (option == selectedOption) "${option.title} ✓" else option.title
}
