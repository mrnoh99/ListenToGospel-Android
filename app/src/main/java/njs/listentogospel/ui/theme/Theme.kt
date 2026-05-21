package njs.listentogospel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    secondary = AccentBlue,
    onSecondary = Color(0xFFFFFFFF),
    background = SystemBackground,
    onBackground = LabelPrimary,
    surface = SystemBackground,
    onSurface = LabelPrimary,
    surfaceVariant = SecondarySystemBackground,
    onSurfaceVariant = LabelPrimary,
    tertiary = LabelSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
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
    val colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
