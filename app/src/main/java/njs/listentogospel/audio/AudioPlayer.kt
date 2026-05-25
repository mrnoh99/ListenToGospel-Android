package njs.listentogospel.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import njs.listentogospel.model.BibleChapter
import njs.listentogospel.service.PlaybackService
import java.io.File

data class AudioState(
    val currentChapter: BibleChapter? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val chapterJustCompleted: Boolean = false,
    val playbackError: String? = null
)

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusGeneration = 0
    private var pendingChapter: BibleChapter? = null
    private var pendingStartMs = 0
    private var playbackJob: Job? = null
    private var playbackGeneration = 0
    private var completionSignaled = false

    @Suppress("DEPRECATION")
    private val legacyFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleAudioFocusChange(focusChange)
    }

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    fun play(chapter: BibleChapter, startMs: Int = 0) {
        val generation = ++playbackGeneration
        playbackJob?.cancel()
        releasePlayer()

        pendingChapter = chapter
        pendingStartMs = startMs

        when (ensureAudioFocus()) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                pendingChapter = null
                _state.update {
                    it.copy(
                        isPlaying = false,
                        playbackError = "오디오 재생 권한을 가져올 수 없습니다. 다른 앱의 소리를 확인해 주세요."
                    )
                }
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        playbackError = null
                    )
                }
            }
            else -> startPendingPlayback(generation)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        pendingChapter = null
        val posMs = mediaPlayer?.currentPosition ?: _state.value.positionMs
        releasePlayer()
        _state.update {
            it.copy(
                isPlaying = false,
                currentChapter = null,
                positionMs = posMs,
                chapterJustCompleted = false
            )
        }
        endPlaybackSession()
    }

    /** Call when playback ends and no further chapter will auto-start (e.g. last chapter of a gospel). */
    fun endPlaybackSession() {
        stopForegroundService()
        abandonAudioFocus()
    }

    /** Stops playback when the user closes the app task (recents swipe or back to exit). */
    fun stopBecauseAppClosed() {
        stop()
    }

    fun getCurrentPositionMs(): Int = mediaPlayer?.currentPosition ?: _state.value.positionMs

    fun reassertPlaybackIfNeeded() {
        if (!_state.value.isPlaying) return
        val player = mediaPlayer ?: return
        if (player.isPlaying) return

        try {
            player.start()
            _state.update { it.copy(isPlaying = true, playbackError = null) }
            startPositionPolling()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reassert playback", e)
        }
    }

    fun ackChapterCompleted() {
        _state.update { it.copy(chapterJustCompleted = false) }
    }

    private fun startPendingPlayback(generation: Int) {
        val chapter = pendingChapter ?: return
        val startMs = pendingStartMs
        pendingChapter = null

        playbackJob = scope.launch {
            try {
                beginPlayback(chapter, startMs, generation)
            } catch (e: CancellationException) {
                throw e
            } catch (first: Exception) {
                if (generation != playbackGeneration) return@launch
                Log.w(TAG, "Playback failed for ${chapter.assetPath}, retrying after cache clear", first)
                invalidateChapterCache(chapter)
                try {
                    beginPlayback(chapter, startMs, generation)
                } catch (e: CancellationException) {
                    throw e
                } catch (second: Exception) {
                    if (generation != playbackGeneration) return@launch
                    Log.e(TAG, "Failed to play ${chapter.assetPath}", second)
                    stopForegroundService()
                    failPlayback(playbackErrorMessage(second))
                }
            }
        }
    }

    private suspend fun beginPlayback(
        chapter: BibleChapter,
        startMs: Int,
        generation: Int
    ) {
        if (generation != playbackGeneration) return
        completionSignaled = false

        val audioFile = withContext(Dispatchers.IO) {
            resolveChapterAudioFile(chapter)
        }
        if (generation != playbackGeneration) return

        startForegroundService(chapter)

        val player = MediaPlayer().apply {
            setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(audioFile.absolutePath)
            setVolume(1f, 1f)
            setOnCompletionListener { scope.launch { handleCompletion() } }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                scope.launch {
                    failPlayback("오디오 재생 중 오류가 발생했습니다.")
                }
                true
            }
        }

        withContext(Dispatchers.IO) {
            player.prepare()
        }
        if (generation != playbackGeneration) {
            player.release()
            return
        }

        val durationMs = player.duration
        val seekMs = safeSeekPositionMs(startMs, durationMs)
        if (seekMs > 0) {
            player.seekTo(seekMs)
        }
        player.start()
        mediaPlayer = player

        _state.update {
            it.copy(
                currentChapter = chapter,
                isPlaying = true,
                durationMs = durationMs,
                positionMs = seekMs,
                chapterJustCompleted = false,
                playbackError = null
            )
        }
        startPositionPolling()
    }

    private fun safeSeekPositionMs(requestedMs: Int, durationMs: Int): Int {
        if (requestedMs <= 0) return 0
        if (durationMs <= 0) return requestedMs
        return requestedMs.coerceIn(0, (durationMs - 250).coerceAtLeast(0))
    }

    private fun playbackErrorMessage(e: Exception): String {
        return if (e is java.io.FileNotFoundException || e.message?.contains("ENOENT") == true) {
            "오디오 파일을 찾을 수 없습니다. 앱을 다시 설치해 주세요."
        } else {
            "오디오 재생을 시작할 수 없습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    private fun chapterCacheFile(chapter: BibleChapter): File =
        File(context.cacheDir, chapter.assetPath.replace("/", "_"))

    private fun invalidateChapterCache(chapter: BibleChapter) {
        chapterCacheFile(chapter).takeIf { it.exists() }?.delete()
    }

    private fun assetByteLength(assetPath: String): Long =
        context.assets.openFd(assetPath).use { it.length }

    private fun resolveChapterAudioFile(chapter: BibleChapter): File {
        val cacheFile = chapterCacheFile(chapter)
        val expectedLength = assetByteLength(chapter.assetPath)

        if (cacheFile.exists() && cacheFile.length() == expectedLength) {
            return cacheFile
        }

        cacheFile.takeIf { it.exists() }?.delete()
        context.assets.open(chapter.assetPath).use { input ->
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }

        if (!cacheFile.exists() || cacheFile.length() != expectedLength) {
            cacheFile.delete()
            throw java.io.IOException(
                "Incomplete audio cache for ${chapter.assetPath} (expected $expectedLength bytes)"
            )
        }
        return cacheFile
    }

    private fun failPlayback(message: String) {
        releasePlayer()
        endPlaybackSession()
        _state.update {
            it.copy(
                isPlaying = false,
                currentChapter = null,
                chapterJustCompleted = false,
                playbackError = message
            )
        }
    }

    private fun handleCompletion() {
        if (completionSignaled) return
        completionSignaled = true
        val completedChapter = _state.value.currentChapter
        releasePlayer()
        _state.update {
            it.copy(
                isPlaying = false,
                chapterJustCompleted = true,
                currentChapter = completedChapter,
                positionMs = it.durationMs
            )
        }
        // Keep audio focus and the foreground service so the next chapter can start immediately.
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (_state.value.isPlaying) {
                val player = mediaPlayer
                if (player != null) {
                    val positionMs = player.currentPosition
                    val durationMs = player.duration
                    _state.update {
                        it.copy(positionMs = positionMs, durationMs = durationMs.coerceAtLeast(0))
                    }
                    if (!completionSignaled &&
                        durationMs > 0 &&
                        positionMs >= durationMs - 400
                    ) {
                        handleCompletion()
                    }
                }
                delay(500)
            }
        }
    }

    private fun releasePlayer() {
        positionJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startForegroundService(chapter: BibleChapter) {
        val intent = Intent(context, PlaybackService::class.java).apply {
            putExtra(PlaybackService.EXTRA_CHAPTER_TITLE, chapter.title)
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service start failed", e)
        }
    }

    private fun stopForegroundService() {
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    private fun ensureAudioFocus(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        return requestAudioFocus()
    }

    private fun requestAudioFocus(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val generation = ++focusGeneration
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).apply {
                setAcceptsDelayedFocusGain(true)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnAudioFocusChangeListener { focusChange ->
                    if (generation != focusGeneration) return@setOnAudioFocusChangeListener
                    handleAudioFocusChange(focusChange)
                }
            }.build()
            audioFocusRequest = req
            return audioManager.requestAudioFocus(req)
        }

        @Suppress("DEPRECATION")
        return audioManager.requestAudioFocus(
            legacyFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pendingChapter != null) {
                    startPendingPlayback(playbackGeneration)
                    return
                }
                mediaPlayer?.start()
                _state.update { it.copy(isPlaying = true) }
                startPositionPolling()
            }
            AudioManager.AUDIOFOCUS_LOSS -> stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.pause()
            }
        }
    }

    private fun abandonAudioFocus() {
        focusGeneration += 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(legacyFocusListener)
        }
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}
