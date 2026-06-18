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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import njs.listentogospel.ui.theme.AppControlLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayGospelButton(
    todayChapter: BibleChapter?,
    liturgicalName: String,
    date: LocalDate,
    onClick: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (todayChapter == null) return

    val today = LocalDate.now()
    val dateLabel = date.format(DateTimeFormatter.ofPattern("M월 d일"))
    val topLabel = if (date == today) "오늘의 복음  $dateLabel" else dateLabel
    val chapterLabel = "${todayChapter.gospel.shortName} ${todayChapter.number}장"
    val a11yCenter = "$topLabel, $liturgicalName, $chapterLabel"

    GlassCapsuleSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppControlLayout.barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous day arrow
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 40.dp)
                    .clickable(onClick = onPrev)
                    .semantics { contentDescription = "이전 날" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "〈",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Center: date + liturgical name + chapter (tapping plays)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onClick)
                    .semantics(mergeDescendants = true) { contentDescription = a11yCenter },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = topLabel,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = liturgicalName,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$chapterLabel ▶",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Next day arrow
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 40.dp)
                    .clickable(onClick = onNext)
                    .semantics { contentDescription = "다음 날" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "〉",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
