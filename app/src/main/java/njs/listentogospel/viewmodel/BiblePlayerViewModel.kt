package njs.listentogospel.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import njs.listentogospel.ListenToGospelApp
import njs.listentogospel.data.PlaybackPersistence
import njs.listentogospel.data.SavedSession
import njs.listentogospel.model.BibleChapter
import njs.listentogospel.model.Gospel
import njs.listentogospel.model.LiturgicalCalendar
import njs.listentogospel.model.Lectionary
import njs.listentogospel.util.AppHaptic
import njs.listentogospel.util.HapticFeedback
import java.time.LocalDate

enum class ChapterScrollAlignment {
    TOP,
    CENTER
}

enum class SleepTimerOption(val title: String, val minutes: Int?) {
    THIRTY("30분", 30),
    SIXTY("60분", 60),
    NINETY("90분", 90),
    ONE_TWENTY("120분", 120),
    CONTINUOUS("계속", null)
}

data class ResumeBookmark(
    val chapter: BibleChapter,
    val positionMs: Int
)

data class UiState(
    val selectedGospel: Gospel = Gospel.MATTHEW,
    val selectedChapter: BibleChapter = BibleChapter(Gospel.MATTHEW, 1),
    val currentChapter: BibleChapter? = null,
    val resumeBookmark: ResumeBookmark? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.CONTINUOUS,
    val sleepTimerRemainingSeconds: Int = 0,
    val savedSession: SavedSession? = null,
    val showResumeOffer: Boolean = false,
    val playbackMessage: String? = null,
    val scrollRequestId: Long = 0,
    val scrollTargetChapter: BibleChapter? = null,
    val scrollAlignment: ChapterScrollAlignment = ChapterScrollAlignment.TOP,
    val viewedDate: LocalDate = LocalDate.now(),
    val todayGospelChapter: BibleChapter? = null,
    val todayLiturgicalName: String = ""
) {
    val playbackTargetChapter: BibleChapter
        get() = currentChapter
            ?: resumeBookmark?.chapter
            ?: savedSession
                ?.takeIf { showResumeOffer }
                ?.let { BibleChapter(it.gospel, it.chapterNumber) }
            ?: selectedChapter
}

class BiblePlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = (application as ListenToGospelApp).audioPlayer
    private val persistence = PlaybackPersistence(application)
    private var sleepTimer: CountDownTimer? = null
    private var launchResumeOfferDismissed = false
    private var pendingResumeProgressAnnouncement = false

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        updateViewedDate(LocalDate.now())
        restoreSessionOnLaunch()

        viewModelScope.launch {
            audioPlayer.state.collectLatest { audioState ->
                _uiState.update { state ->
                    state.copy(
                        currentChapter = audioState.currentChapter,
                        isPlaying = audioState.isPlaying,
                        positionMs = audioState.positionMs,
                        durationMs = audioState.durationMs,
                        playbackMessage = when {
                            audioState.playbackError != null -> audioState.playbackError
                            audioState.isPlaying -> null
                            else -> state.playbackMessage
                        }
                    )
                }
                if (audioState.chapterJustCompleted) {
                    val completedChapter = audioState.currentChapter
                    audioPlayer.ackChapterCompleted()
                    if (completedChapter != null) {
                        viewModelScope.launch {
                            playNextChapterAfter(completedChapter)
                        }
                    }
                }
            }
        }
    }

    fun selectGospelInGrid(gospel: Gospel) {
        val state = _uiState.value
        if (gospel == state.selectedGospel) {
            val chapter = when {
                state.isPlaying && state.currentChapter?.gospel == gospel -> state.currentChapter
                else -> stoppedResumeChapter(state, gospel)
            }
            if (chapter != null) {
                _uiState.update { it.copy(selectedChapter = chapter) }
                requestScroll(chapter, ChapterScrollAlignment.CENTER)
                persistFocusedSession()
            }
            return
        }

        _uiState.update { state ->
            val chapter = when {
                state.isPlaying && state.currentChapter?.gospel == gospel -> state.currentChapter
                state.resumeBookmark?.chapter?.gospel == gospel -> state.resumeBookmark.chapter
                else -> BibleChapter(gospel, 1)
            }
            state.copy(selectedGospel = gospel, selectedChapter = chapter)
        }
        requestScroll(BibleChapter(gospel, 1), ChapterScrollAlignment.TOP)
        persistFocusedSession()
    }

    private fun stoppedResumeChapter(state: UiState, gospel: Gospel): BibleChapter? {
        if (state.isPlaying) return null
        val bookmark = state.resumeBookmark ?: return null
        return bookmark.chapter.takeIf { it.gospel == gospel }
    }

    /** Saves gospel, chapter, and playback position for the next app launch. */
    fun persistFocusedSession() {
        val state = _uiState.value
        val chapter = state.currentChapter
            ?: state.resumeBookmark?.chapter
            ?: state.selectedChapter.takeIf { it.gospel == state.selectedGospel }
            ?: return

        val elapsedSeconds = when {
            state.resumeBookmark?.chapter == chapter ->
                state.resumeBookmark.positionMs / 1000.0
            state.currentChapter == chapter && state.isPlaying ->
                audioPlayer.getCurrentPositionMs() / 1000.0
            state.currentChapter == chapter ->
                state.positionMs / 1000.0
            else -> persistence.load()?.let { saved ->
                if (saved.gospel == chapter.gospel && saved.chapterNumber == chapter.number) {
                    saved.elapsedSeconds
                } else {
                    null
                }
            } ?: 0.0
        }

        persistence.save(chapter.gospel, chapter.number, elapsedSeconds)
    }

    private fun restoreSessionOnLaunch() {
        val saved = persistence.load()
        if (saved == null) {
            requestScroll(BibleChapter(Gospel.MATTHEW, 1), ChapterScrollAlignment.TOP)
            return
        }

        val chapterNumber = saved.chapterNumber.coerceIn(1, saved.gospel.chapterCount)
        val chapter = BibleChapter(saved.gospel, chapterNumber)
        val positionMs = (saved.elapsedSeconds * 1000).toInt().coerceAtLeast(0)
        val canResume = saved.elapsedSeconds >= 3

        _uiState.update {
            it.copy(
                selectedGospel = saved.gospel,
                selectedChapter = chapter,
                savedSession = if (canResume) saved else null,
                showResumeOffer = canResume,
                resumeBookmark = if (canResume) ResumeBookmark(chapter, positionMs) else null,
                positionMs = if (canResume) positionMs else 0
            )
        }
        requestScroll(chapter, ChapterScrollAlignment.CENTER)
    }

    private fun refreshLaunchResumeOffer() {
        val state = _uiState.value
        if (launchResumeOfferDismissed || state.isPlaying || state.resumeBookmark != null) {
            _uiState.update { it.copy(showResumeOffer = false) }
            return
        }

        val saved = persistence.load()
        if (saved != null && saved.elapsedSeconds >= 3) {
            _uiState.update {
                it.copy(
                    savedSession = saved,
                    showResumeOffer = true,
                    selectedGospel = saved.gospel,
                    selectedChapter = BibleChapter(saved.gospel, saved.chapterNumber)
                )
            }
        } else {
            _uiState.update { it.copy(showResumeOffer = false, savedSession = null) }
        }
    }

    private fun dismissLaunchResumeOffer() {
        launchResumeOfferDismissed = true
        _uiState.update { it.copy(showResumeOffer = false, savedSession = null) }
    }

    private fun requestScroll(chapter: BibleChapter, alignment: ChapterScrollAlignment) {
        _uiState.update {
            it.copy(
                scrollTargetChapter = chapter,
                scrollAlignment = alignment,
                scrollRequestId = it.scrollRequestId + 1
            )
        }
    }

    fun selectChapter(chapter: BibleChapter) {
        _uiState.update { it.copy(selectedChapter = chapter) }
    }

    fun toggleChapterPlayback(chapter: BibleChapter) {
        val state = _uiState.value
        if (state.isPlaying && state.currentChapter == chapter) {
            stop()
            return
        }
        if (!state.isPlaying && state.resumeBookmark?.chapter == chapter) {
            resumePlaybackAfterStop()
            return
        }
        playChapter(chapter)
    }

    fun canResumeChapter(chapter: BibleChapter): Boolean {
        val state = _uiState.value
        return !state.isPlaying && state.resumeBookmark?.chapter == chapter
    }

    fun consumeResumeProgressAnnouncement(): Boolean {
        if (!pendingResumeProgressAnnouncement) return false
        pendingResumeProgressAnnouncement = false
        return true
    }

    fun playChapter(chapter: BibleChapter, startMs: Int = 0) {
        if (_uiState.value.isPlaying) saveCurrentPosition()
        dismissLaunchResumeOffer()
        pendingResumeProgressAnnouncement = startMs > 0
        _uiState.update {
            it.copy(
                selectedGospel = chapter.gospel,
                selectedChapter = chapter,
                resumeBookmark = null,
                playbackMessage = null
            )
        }
        requestScroll(chapter, ChapterScrollAlignment.CENTER)
        audioPlayer.play(chapter, startMs)
        val result = audioPlayer.state.value
        if (!result.isPlaying && result.playbackError != null) {
            _uiState.update {
                it.copy(playbackMessage = result.playbackError)
            }
        }
    }

    fun playFromSelection() {
        val target = _uiState.value.playbackTargetChapter
        playChapter(target)
    }

    fun playTodayGospel() {
        val chapter = _uiState.value.todayGospelChapter ?: return
        playChapter(chapter)
    }

    fun navigatePrevDay() {
        updateViewedDate(_uiState.value.viewedDate.minusDays(1))
    }

    fun navigateNextDay() {
        updateViewedDate(_uiState.value.viewedDate.plusDays(1))
    }

    private fun updateViewedDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                viewedDate = date,
                todayGospelChapter = Lectionary.getTodayGospelChapter(date),
                todayLiturgicalName = LiturgicalCalendar.liturgicalDayName(date)
            )
        }
    }

    fun stop() {
        val chapter = _uiState.value.currentChapter
        val posMs = audioPlayer.getCurrentPositionMs()
        saveCurrentPosition()
        audioPlayer.stop()
        if (chapter != null) {
            _uiState.update {
                it.copy(
                    resumeBookmark = ResumeBookmark(chapter, posMs),
                    selectedGospel = chapter.gospel,
                    selectedChapter = chapter
                )
            }
            requestScroll(chapter, ChapterScrollAlignment.CENTER)
        }
        refreshLaunchResumeOffer()
        persistFocusedSession()
    }

    fun resumePlaybackAfterStop(): Boolean {
        val bookmark = _uiState.value.resumeBookmark ?: return false
        playChapter(bookmark.chapter, bookmark.positionMs)
        return true
    }

    fun resumeFromLaunchOffer(): Boolean {
        val saved = _uiState.value.savedSession ?: return false
        if (!_uiState.value.showResumeOffer) return false
        playChapter(
            BibleChapter(saved.gospel, saved.chapterNumber),
            (saved.elapsedSeconds * 1000).toInt()
        )
        return true
    }

    fun onPlaybackButtonTap() {
        val state = _uiState.value
        when {
            state.isPlaying -> stop()
            resumePlaybackAfterStop() -> Unit
            state.showResumeOffer && state.savedSession != null -> resumeFromLaunchOffer()
            else -> playFromSelection()
        }
    }

    fun reassertPlaybackIfNeeded() {
        audioPlayer.reassertPlaybackIfNeeded()
    }

    fun dismissResumeOffer() {
        dismissLaunchResumeOffer()
        persistence.clear()
    }

    fun setSleepTimer(option: SleepTimerOption) {
        cancelSleepTimerInternal()
        _uiState.update { it.copy(sleepTimerOption = option) }
        val minutes = option.minutes ?: return
        val totalMs = minutes * 60 * 1000L
        sleepTimer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(remaining: Long) {
                _uiState.update { it.copy(sleepTimerRemainingSeconds = (remaining / 1000).toInt()) }
            }

            override fun onFinish() {
                stop()
                _uiState.update {
                    it.copy(
                        sleepTimerRemainingSeconds = 0,
                        sleepTimerOption = SleepTimerOption.CONTINUOUS
                    )
                }
            }
        }.start()
        _uiState.update { it.copy(sleepTimerRemainingSeconds = minutes * 60) }
    }

    private fun cancelSleepTimerInternal() {
        sleepTimer?.cancel()
        sleepTimer = null
        _uiState.update { it.copy(sleepTimerRemainingSeconds = 0) }
    }

    private fun playNextChapterAfter(completedChapter: BibleChapter) {
        val next = if (completedChapter.number < completedChapter.gospel.chapterCount) {
            completedChapter.number + 1
        } else {
            1
        }
        haptic(AppHaptic.ChapterChange)
        playChapter(BibleChapter(completedChapter.gospel, next))
    }

    private fun saveCurrentPosition() {
        val chapter = _uiState.value.currentChapter ?: return
        val posMs = audioPlayer.getCurrentPositionMs()
        persistence.save(chapter.gospel, chapter.number, posMs / 1000.0)
    }

    override fun onCleared() {
        cancelSleepTimerInternal()
        persistFocusedSession()
        super.onCleared()
    }

    private fun haptic(kind: AppHaptic) {
        HapticFeedback.perform(getApplication(), kind)
    }
}
