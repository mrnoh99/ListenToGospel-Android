package njs.listentogospel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import njs.listentogospel.ui.theme.AppControlLayout

@Composable
fun GlassCapsuleSurface(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = AppControlLayout.barHorizontalPadding,
    cornerRadius: Dp = AppControlLayout.barCornerRadius,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val strokeColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 5.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(width = 0.75.dp, color = strokeColor, shape = shape)
            .padding(horizontal = horizontalPadding)
    ) {
        content()
    }
}
