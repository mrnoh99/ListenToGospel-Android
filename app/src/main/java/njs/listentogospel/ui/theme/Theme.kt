package njs.listentogospel.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AccentBlue.copy(alpha = 0.35f),
    onPrimaryContainer = LabelPrimaryDark,
    secondary = AccentBlue,
    onSecondary = Color(0xFFFFFFFF),
    background = SystemBackgroundDark,
    onBackground = LabelPrimaryDark,
    surface = SystemBackgroundDark,
    onSurface = LabelPrimaryDark,
    surfaceVariant = SecondarySystemBackgroundDark,
    onSurfaceVariant = LabelPrimaryDark,
    tertiary = LabelSecondaryDark
)

@Composable
fun ListenToGospelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
