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
    val playbackMessage: String? = null
) {
    val playbackTargetChapter: BibleChapter
        get() = currentChapter
            ?: resumeBookmark?.chapter
            ?: savedSession?.let { BibleChapter(it.gospel, it.chapterNumber) }
            ?: selectedChapter
}

class BiblePlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = (application as ListenToGospelApp).audioPlayer
    private val persistence = PlaybackPersistence(application)
    private var sleepTimer: CountDownTimer? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
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
        }

        viewModelScope.launch {
            audioPlayer.state.collectLatest { audioState ->
                _uiState.update { state ->
                    state.copy(
                        currentChapter = audioState.currentChapter,
                        isPlaying = audioState.isPlaying,
                        positionMs = audioState.positionMs,
                        durationMs = audioState.durationMs,
                        playbackMessage = audioState.playbackError ?: state.playbackMessage
                    )
                }
                if (audioState.chapterJustCompleted) {
                    audioPlayer.ackChapterCompleted()
                    playNextChapter()
                }
            }
        }
    }

    fun selectGospelInGrid(gospel: Gospel) {
        _uiState.update { state ->
            val chapter = when {
                state.isPlaying && state.currentChapter?.gospel == gospel -> state.currentChapter!!
                state.resumeBookmark?.chapter?.gospel == gospel -> state.resumeBookmark!!.chapter
                else -> BibleChapter(gospel, 1)
            }
            state.copy(selectedGospel = gospel, selectedChapter = chapter)
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

    fun playChapter(chapter: BibleChapter, startMs: Int = 0) {
        if (_uiState.value.isPlaying) saveCurrentPosition()
        audioPlayer.play(chapter, startMs)
        _uiState.update {
            it.copy(
                selectedGospel = chapter.gospel,
                selectedChapter = chapter,
                resumeBookmark = null,
                showResumeOffer = false,
                savedSession = null,
                playbackMessage = null
            )
        }
    }

    fun playFromSelection() {
        playChapter(_uiState.value.selectedChapter)
    }

    fun stop() {
        val chapter = _uiState.value.currentChapter
        val posMs = audioPlayer.getCurrentPositionMs()
        saveCurrentPosition()
        audioPlayer.stop()
        if (chapter != null) {
            _uiState.update {
                it.copy(resumeBookmark = ResumeBookmark(chapter, posMs))
            }
        }
    }

    fun resumePlaybackAfterStop(): Boolean {
        val bookmark = _uiState.value.resumeBookmark ?: return false
        playChapter(bookmark.chapter, bookmark.positionMs)
        return true
    }

    fun resumeFromLaunchOffer(): Boolean {
        val saved = _uiState.value.savedSession ?: return false
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
            resumeFromLaunchOffer() -> Unit
            else -> playFromSelection()
        }
    }

    fun dismissResumeOffer() {
        _uiState.update { it.copy(showResumeOffer = false, savedSession = null) }
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

    private fun playNextChapter() {
        val current = _uiState.value.currentChapter ?: return
        val next = current.number + 1
        if (next <= current.gospel.chapterCount) {
            playChapter(BibleChapter(current.gospel, next))
        } else {
            persistence.clear()
        }
    }

    private fun saveCurrentPosition() {
        val chapter = _uiState.value.currentChapter ?: return
        val posMs = audioPlayer.getCurrentPositionMs()
        persistence.save(chapter.gospel, chapter.number, posMs / 1000.0)
    }

    override fun onCleared() {
        cancelSleepTimerInternal()
        saveCurrentPosition()
        super.onCleared()
    }
}
