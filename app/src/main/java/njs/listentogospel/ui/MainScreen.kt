package njs.listentogospel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import njs.listentogospel.ui.components.ChapterList
import njs.listentogospel.ui.components.GospelHeaderGlassBar
import njs.listentogospel.ui.components.GospelPicker
import njs.listentogospel.ui.components.PlaybackGlassMenu
import njs.listentogospel.ui.components.SleepTimerSheet
import njs.listentogospel.ui.theme.AppControlLayout
import njs.listentogospel.viewmodel.BiblePlayerViewModel
import njs.listentogospel.viewmodel.SleepTimerOption

@Composable
fun MainScreen(viewModel: BiblePlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val bottomContentPadding = with(density) {
        AppControlLayout.chapterListGlassPeek.roundToPx()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "복음서듣기",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            GospelPicker(
                selectedGospel = uiState.selectedGospel,
                onSelect = viewModel::selectGospelInGrid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppControlLayout.headerSectionSpacing)
            )

            GospelHeaderGlassBar(
                gospelName = uiState.selectedGospel.koreanName,
                sleepTimerLabel = sleepTimerLabel(
                    option = uiState.sleepTimerOption,
                    remainingSeconds = uiState.sleepTimerRemainingSeconds
                ),
                onSleepTimerTap = { showSleepTimerSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppControlLayout.floatingBarHorizontalInset,
                        vertical = AppControlLayout.headerBottomPadding
                    )
            )

            ChapterList(
                gospel = uiState.selectedGospel,
                selectedChapter = uiState.selectedChapter,
                currentChapter = uiState.currentChapter,
                resumeChapter = uiState.resumeBookmark?.chapter,
                isPlaying = uiState.isPlaying,
                positionMs = uiState.positionMs,
                durationMs = uiState.durationMs,
                topContentPadding = 0,
                bottomContentPadding = bottomContentPadding,
                scrollRequestId = uiState.scrollRequestId,
                scrollTargetChapter = uiState.scrollTargetChapter,
                scrollAlignment = uiState.scrollAlignment,
                onChapterClick = viewModel::toggleChapterPlayback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppControlLayout.chapterListViewportHeight)
            )

            PlaybackGlassMenu(
                chapterTitle = uiState.playbackTargetChapter.title,
                isPlaying = uiState.isPlaying,
                onPlayStop = viewModel::onPlaybackButtonTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppControlLayout.floatingBarHorizontalInset,
                        vertical = AppControlLayout.floatingBarVerticalInset
                    )
            )

            if (uiState.showResumeOffer && uiState.savedSession != null && !uiState.isPlaying) {
                val session = uiState.savedSession!!
                Button(
                    onClick = { viewModel.resumeFromLaunchOffer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "이어서 ${session.gospel.shortName} ${session.chapterNumber}장 재생",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            uiState.playbackMessage?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = Color(0xFFFF9500),
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "by njs 2026",
            modifier = Modifier
                .fillMaxWidth()
                .height(AppControlLayout.footerBarHeight),
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 8.sp,
            textAlign = TextAlign.End
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerSheet(
            selectedOption = uiState.sleepTimerOption,
            onSelect = { option ->
                viewModel.setSleepTimer(option)
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false }
        )
    }
}

private fun sleepTimerLabel(option: SleepTimerOption, remainingSeconds: Int): String {
    return when {
        option == SleepTimerOption.CONTINUOUS || remainingSeconds <= 0 -> "남은시간: ∞"
        else -> {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            "남은시간: ${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }
}
