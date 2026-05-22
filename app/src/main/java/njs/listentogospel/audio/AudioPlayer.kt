package njs.listentogospel.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
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

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    fun play(chapter: BibleChapter, startMs: Int = 0) {
        abandonAudioFocus()
        releasePlayer()

        pendingChapter = chapter
        pendingStartMs = startMs

        when (requestAudioFocus()) {
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
            else -> startPendingPlayback()
        }
    }

    fun stop() {
        pendingChapter = null
        val posMs = mediaPlayer?.currentPosition ?: _state.value.positionMs
        releasePlayer()
        _state.update {
            it.copy(
                isPlaying = false,
                currentChapter = null,
                positionMs = posMs
            )
        }
        stopForegroundService()
        abandonAudioFocus()
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

    private fun startPendingPlayback() {
        val chapter = pendingChapter ?: return
        val startMs = pendingStartMs
        pendingChapter = null

        try {
            val audioFile = resolveChapterAudioFile(chapter)
            startForegroundService(chapter)
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                setVolume(1f, 1f)
                setOnCompletionListener { handleCompletion() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    failPlayback("오디오 재생 중 오류가 발생했습니다.")
                    true
                }
                prepare()
                if (startMs > 0) seekTo(startMs)
                start()
            }
            _state.update {
                it.copy(
                    currentChapter = chapter,
                    isPlaying = true,
                    durationMs = mediaPlayer!!.duration,
                    positionMs = startMs,
                    chapterJustCompleted = false,
                    playbackError = null
                )
            }
            startPositionPolling()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ${chapter.assetPath}", e)
            stopForegroundService()
            failPlayback(
                "오디오 파일을 찾을 수 없습니다. 프로젝트 루트에서 ./copy_audio_assets.sh 실행 후 앱을 다시 빌드하세요."
            )
        }
    }

    private fun resolveChapterAudioFile(chapter: BibleChapter): File {
        val cacheFile = File(context.cacheDir, chapter.assetPath.replace("/", "_"))
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            return cacheFile
        }

        context.assets.open(chapter.assetPath).use { input ->
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        }
        return cacheFile
    }

    private fun failPlayback(message: String) {
        releasePlayer()
        abandonAudioFocus()
        _state.update {
            it.copy(
                isPlaying = false,
                currentChapter = null,
                playbackError = message
            )
        }
    }

    private fun handleCompletion() {
        releasePlayer()
        _state.update { it.copy(isPlaying = false, chapterJustCompleted = true) }
        stopForegroundService()
        abandonAudioFocus()
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (_state.value.isPlaying) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _state.update { it.copy(positionMs = player.currentPosition) }
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
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service start failed", e)
        }
    }

    private fun stopForegroundService() {
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    private fun requestAudioFocus(): Int {
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

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pendingChapter != null) {
                    startPendingPlayback()
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
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}
