package njs.listentogospel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import njs.listentogospel.ui.hideFromAccessibilityTree
import njs.listentogospel.ui.mergedButtonSemantics
import njs.listentogospel.util.AccessibilitySupport
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import njs.listentogospel.ui.theme.AppControlLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackGlassMenu(
    chapterTitle: String,
    isPlaying: Boolean,
    onPlayStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppControlLayout.barCornerRadius)
    val strokeColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        Color.White.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    Surface(
        onClick = onPlayStop,
        modifier = modifier
            .mergedButtonSemantics(
                label = AccessibilitySupport.playbackButtonLabel(chapterTitle, isPlaying),
                hint = AccessibilitySupport.playbackButtonHint(isPlaying)
            )
            .fillMaxWidth()
            .height(AppControlLayout.playbackBarHeight),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 5.dp,
        border = BorderStroke(0.75.dp, strokeColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppControlLayout.barHorizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(AppControlLayout.playbackBarIconSize)
                        .hideFromAccessibilityTree()
                )
                Text(
                    text = chapterTitle,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .widthIn(max = 240.dp)
                        .hideFromAccessibilityTree(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
