package njs.listentogospel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import njs.listentogospel.model.BibleChapter
import njs.listentogospel.model.Gospel
import njs.listentogospel.ui.hapticClickable
import njs.listentogospel.ui.theme.AppControlLayout
import njs.listentogospel.ui.theme.PlayingRowBackground
import njs.listentogospel.util.AppHaptic
import njs.listentogospel.viewmodel.ChapterScrollAlignment

private fun chapterLazyIndex(chapterNumber: Int): Int = chapterNumber + 1

@Composable
fun ChapterList(
    gospel: Gospel,
    selectedChapter: BibleChapter,
    currentChapter: BibleChapter?,
    resumeChapter: BibleChapter?,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    topContentPadding: Int,
    bottomContentPadding: Int,
    scrollRequestId: Long,
    scrollTargetChapter: BibleChapter?,
    scrollAlignment: ChapterScrollAlignment,
    onChapterClick: (BibleChapter) -> Unit,
    chapterClickHaptic: (BibleChapter) -> AppHaptic = { AppHaptic.Selection },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val estimatedRowHeightPx = with(density) { AppControlLayout.chapterRowMinHeight.roundToPx() }

    LaunchedEffect(scrollRequestId) {
        val target = scrollTargetChapter ?: return@LaunchedEffect
        if (target.gospel != gospel) return@LaunchedEffect
        listState.scrollToChapter(target.number, scrollAlignment, estimatedRowHeightPx)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        item(key = "top-spacer") { Spacer(modifier = Modifier.height(topContentPadding.dp)) }

        item(key = "top-edge-spacer-row") {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppControlLayout.chapterRowMinHeight)
            )
        }

        items(gospel.chapters, key = { it.number }) { chapter ->
            val isCurrentlyPlaying = isPlaying && currentChapter == chapter
            val canResume = !isPlaying && resumeChapter == chapter
            val isActive = isCurrentlyPlaying || canResume
            val isSelected = chapter == selectedChapter

            ChapterRow(
                chapter = chapter,
                isActive = isActive,
                isCurrentlyPlaying = isCurrentlyPlaying,
                canResume = canResume,
                isSelected = isSelected,
                positionMs = if (isActive) positionMs else 0,
                durationMs = if (isActive) durationMs else 0,
                hapticKind = chapterClickHaptic(chapter),
                onClick = { onChapterClick(chapter) }
            )
        }

        item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(bottomContentPadding.dp)) }
    }
}

private const val scrollCenterTolerancePx = 8

private suspend fun LazyListState.scrollToChapter(
    chapterNumber: Int,
    alignment: ChapterScrollAlignment,
    estimatedRowHeightPx: Int
) {
    val index = chapterLazyIndex(chapterNumber)
    snapshotFlow { layoutInfo.viewportSize.height }
        .filter { it > 0 }
        .first()

    when (alignment) {
        ChapterScrollAlignment.TOP -> {
            animateScrollToItem(index = index, scrollOffset = 0)
        }
        ChapterScrollAlignment.CENTER -> {
            val layout = layoutInfo
            val viewportHeight = layout.viewportSize.height
            val visibleItem = layout.visibleItemsInfo.firstOrNull { it.index == index }
            val itemHeight = visibleItem?.size ?: estimatedRowHeightPx
            val desiredTop = ((viewportHeight - itemHeight) / 2).coerceAtLeast(0)

            if (visibleItem != null) {
                val adjustment = visibleItem.offset - desiredTop
                if (kotlin.math.abs(adjustment) > scrollCenterTolerancePx) {
                    animateScrollToItem(index = index, scrollOffset = -desiredTop)
                }
            } else {
                animateScrollToItem(index = index, scrollOffset = -desiredTop)
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: BibleChapter,
    isActive: Boolean,
    isCurrentlyPlaying: Boolean,
    canResume: Boolean,
    isSelected: Boolean,
    positionMs: Int,
    durationMs: Int,
    hapticKind: AppHaptic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(AppControlLayout.chapterRowMinHeight)
            .background(if (isActive) PlayingRowBackground else MaterialTheme.colorScheme.background)
            .hapticClickable(kind = hapticKind, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )

            if (isActive && durationMs > 0) {
                Text(
                    text = "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            when {
                isCurrentlyPlaying -> {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                    )
                }
                canResume -> {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp)
                    )
                }
                isSelected -> {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                    )
                }
            }
        }

        if (isActive) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
            )
        }
    }
}

private fun formatPlaybackTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
