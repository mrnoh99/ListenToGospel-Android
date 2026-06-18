package njs.listentogospel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import njs.listentogospel.model.BibleChapter
import njs.listentogospel.ui.accessibleHapticClickable
import njs.listentogospel.ui.theme.AppControlLayout
import njs.listentogospel.util.AccessibilitySupport
import njs.listentogospel.util.AppHaptic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayGospelButton(
    todayChapter: BibleChapter?,
    liturgicalName: String,
    date: LocalDate,
    startVerse: Int,
    onPlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGoToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (todayChapter == null) return

    val today = LocalDate.now()
    val isToday = date == today
    val dateLabel = date.format(
        DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)
    )
    val gospelName = todayChapter.gospel.shortName
    val chapterLabel = "$gospelName ${todayChapter.number}장 ${startVerse}절"
    val infoDescription = "$dateLabel, $liturgicalName, $chapterLabel"

    GlassCapsuleSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppControlLayout.todayGospelBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dateLabel,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NavArrowButton(
                        label = "〈",
                        contentDescription = "이전 날",
                        onClick = onPrev
                    )

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 44.dp)
                            .clickable(enabled = !isToday, onClick = onGoToday)
                            .semantics { contentDescription = "오늘의 복음으로 돌아가기" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "오늘",
                            color = if (isToday) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    NavArrowButton(
                        label = "〉",
                        contentDescription = "다음 날",
                        onClick = onNext
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp)
                    .semantics { contentDescription = infoDescription },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = liturgicalName,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chapterLabel,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 56.dp)
                    .accessibleHapticClickable(
                        label = AccessibilitySupport.playbackButtonLabel(todayChapter, isPlaying = false),
                        kind = AppHaptic.Play,
                        onClick = onPlay
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppControlLayout.todayGospelPlayIconSize)
                )
            }
        }
    }
}

@Composable
private fun NavArrowButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 36.dp)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
