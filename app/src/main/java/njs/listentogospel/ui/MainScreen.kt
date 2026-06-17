package njs.listentogospel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import njs.listentogospel.ui.LocalAppAccessibilityEnabled
import njs.listentogospel.ui.TrackAccessibilityState
import njs.listentogospel.ui.hapticClick
import njs.listentogospel.util.AccessibilitySupport
import njs.listentogospel.ui.components.ChapterList
import njs.listentogospel.ui.components.GospelHeaderGlassBar
import njs.listentogospel.ui.components.GospelPicker
import njs.listentogospel.ui.components.PlaybackGlassMenu
import njs.listentogospel.ui.components.SleepTimerSheet
import njs.listentogospel.ui.components.TodayGospelButton
import njs.listentogospel.model.BibleChapter
import njs.listentogospel.ui.theme.AppControlLayout
import njs.listentogospel.util.AppHaptic
import njs.listentogospel.viewmodel.BiblePlayerViewModel
import njs.listentogospel.viewmodel.SleepTimerOption
import njs.listentogospel.viewmodel.UiState

@Composable
fun MainScreen(viewModel: BiblePlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val density = LocalDensity.current

    val bottomContentPadding = with(density) {
        AppControlLayout.controlsOverlayBottomReserve.roundToPx()
    }

    TrackAccessibilityState {
        val useTalkBack = LocalAppAccessibilityEnabled.current
        AnnounceResumeProgressOnce(uiState = uiState, viewModel = viewModel)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
        Text(
            text = "복음서듣기",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppControlLayout.topContentInset)
                .then(
                    if (useTalkBack) Modifier.semantics { heading() } else Modifier
                ),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        TodayGospelButton(
            todayChapter = uiState.todayGospelChapter,
            onClick = { viewModel.playTodayGospel() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppControlLayout.headerSectionSpacing)
                .padding(horizontal = AppControlLayout.floatingBarHorizontalInset)
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
            onSleepTimerTap = hapticClick(haptic, AppHaptic.Selection) {
                showSleepTimerSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppControlLayout.floatingBarHorizontalInset)
                .padding(bottom = AppControlLayout.headerBottomPadding)
        )

        uiState.playbackMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .then(
                        if (useTalkBack) {
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        } else {
                            Modifier
                        }
                    ),
                color = Color(0xFFFF9500),
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
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
                chapterClickHaptic = { chapter -> chapterRowHaptic(uiState, chapter) },
                onChapterClick = viewModel::toggleChapterPlayback,
                modifier = Modifier.fillMaxSize()
            )

            PlaybackGlassMenu(
                chapter = uiState.playbackTargetChapter,
                isPlaying = uiState.isPlaying,
                onPlayStop = hapticClick(
                    haptic,
                    if (uiState.isPlaying) AppHaptic.Stop else AppHaptic.Play
                ) {
                    viewModel.onPlaybackButtonTap()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = AppControlLayout.floatingBarHorizontalInset)
                    .padding(vertical = AppControlLayout.floatingBarVerticalInset)
            )
        }

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
}

@Composable
private fun AnnounceResumeProgressOnce(
    uiState: UiState,
    viewModel: BiblePlayerViewModel
) {
    val view = LocalView.current
    val accessibilityEnabled = LocalAppAccessibilityEnabled.current

    LaunchedEffect(
        accessibilityEnabled,
        uiState.isPlaying,
        uiState.currentChapter,
        uiState.durationMs
    ) {
        if (!accessibilityEnabled) return@LaunchedEffect
        if (!uiState.isPlaying || uiState.durationMs <= 0) return@LaunchedEffect
        if (!viewModel.consumeResumeProgressAnnouncement()) return@LaunchedEffect
        view.announceForAccessibility(
            AccessibilitySupport.resumeProgressAnnouncement(
                positionMs = uiState.positionMs,
                durationMs = uiState.durationMs
            )
        )
    }
}

private fun chapterRowHaptic(uiState: UiState, chapter: BibleChapter): AppHaptic {
    return when {
        uiState.isPlaying && uiState.currentChapter == chapter -> AppHaptic.Stop
        !uiState.isPlaying && uiState.resumeBookmark?.chapter == chapter -> AppHaptic.Play
        else -> AppHaptic.Play
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
