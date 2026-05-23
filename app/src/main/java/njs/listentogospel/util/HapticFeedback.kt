package njs.listentogospel.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

enum class AppHaptic {
    Selection,
    Play,
    Stop,
    ChapterChange,
}

object HapticFeedback {

    fun perform(view: View, kind: AppHaptic) {
        if (!view.isAttachedToWindow) return
        performVibration(view.context.applicationContext, kind)
        performViewHaptic(view, kind)
        playSoundEffect(view, kind)
    }

    fun perform(context: Context, kind: AppHaptic) {
        performVibration(context.applicationContext, kind)
        val view = (context as? Activity)?.window?.decorView
        if (view != null && view.isAttachedToWindow) {
            performViewHaptic(view, kind)
            playSoundEffect(view, kind)
        } else {
            playTone(context, kind)
        }
    }

    private fun performViewHaptic(view: View, kind: AppHaptic) {
        var flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            flags = flags or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        }
        view.performHapticFeedback(hapticConstant(kind), flags)
    }

    private fun playSoundEffect(view: View, kind: AppHaptic) {
        val sound = when (kind) {
            AppHaptic.Selection -> SoundEffectConstants.CLICK
            AppHaptic.Play -> SoundEffectConstants.CLICK
            AppHaptic.Stop -> SoundEffectConstants.NAVIGATION_DOWN
            AppHaptic.ChapterChange -> SoundEffectConstants.NAVIGATION_RIGHT
        }
        view.playSoundEffect(sound)
    }

    private fun playTone(context: Context, kind: AppHaptic) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        val toneType = when (kind) {
            AppHaptic.Selection -> ToneGenerator.TONE_PROP_BEEP2
            AppHaptic.Play -> ToneGenerator.TONE_PROP_ACK
            AppHaptic.Stop -> ToneGenerator.TONE_PROP_NACK
            AppHaptic.ChapterChange -> ToneGenerator.TONE_PROP_PROMPT
        }

        var generator: ToneGenerator? = null
        try {
            generator = ToneGenerator(AudioManager.STREAM_SYSTEM, 70)
            generator.startTone(toneType, 45)
        } catch (_: Exception) {
            // Ignore if tone playback is unavailable on the device.
        } finally {
            generator?.release()
        }
    }

    private fun performVibration(context: Context, kind: AppHaptic) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effectId = when (kind) {
                AppHaptic.Selection -> VibrationEffect.EFFECT_CLICK
                AppHaptic.Play -> VibrationEffect.EFFECT_TICK
                AppHaptic.Stop -> VibrationEffect.EFFECT_HEAVY_CLICK
                AppHaptic.ChapterChange -> VibrationEffect.EFFECT_DOUBLE_CLICK
            }
            val effect = runCatching {
                VibrationEffect.createPredefined(effectId)
            }.getOrElse {
                VibrationEffect.createOneShot(
                    oneShotDurationMs(kind),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(oneShotDurationMs(kind))
        }
    }

    private fun oneShotDurationMs(kind: AppHaptic): Long {
        return when (kind) {
            AppHaptic.Selection -> 25L
            AppHaptic.Play -> 30L
            AppHaptic.Stop -> 45L
            AppHaptic.ChapterChange -> 35L
        }
    }

    private fun hapticConstant(kind: AppHaptic): Int {
        return when (kind) {
            AppHaptic.Selection -> HapticFeedbackConstants.CLOCK_TICK
            AppHaptic.Play -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            AppHaptic.Stop -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            AppHaptic.ChapterChange -> HapticFeedbackConstants.CONTEXT_CLICK
        }
    }

    private fun vibrator(context: Context): Vibrator? {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        return vibrator?.takeIf { it.hasVibrator() }
    }
}
