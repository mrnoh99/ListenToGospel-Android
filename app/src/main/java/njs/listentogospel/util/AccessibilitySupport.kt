package njs.listentogospel.util

import njs.listentogospel.model.BibleChapter
import njs.listentogospel.model.Gospel

object AccessibilitySupport {
    const val sleepTimerButtonLabel = "타이머 버튼"
    const val sleepTimerButtonHint = "수면 타이머를 설정합니다"

    private val sinoKoreanDigits = arrayOf("", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")

    fun spokenSinoKoreanNumber(number: Int): String {
        if (number <= 0) return "영"
        if (number >= 100) return number.toString()
        if (number < 10) return sinoKoreanDigits[number]
        if (number < 20) {
            val ones = number % 10
            return if (ones == 0) "십" else "십${sinoKoreanDigits[ones]}"
        }
        val tens = number / 10
        val ones = number % 10
        val tensWord = "${sinoKoreanDigits[tens]}십"
        return if (ones == 0) tensWord else "$tensWord${sinoKoreanDigits[ones]}"
    }

    fun spokenChapterTitle(chapter: BibleChapter): String {
        return "${chapter.gospel.shortName}복음서 ${spokenSinoKoreanNumber(chapter.number)}장"
    }

    fun spokenChapterTitle(title: String): String {
        val match = Regex("""(\d+)\s*장""").find(title) ?: return title
        val number = match.groupValues[1].toIntOrNull() ?: return title
        val prefix = title.substring(0, match.range.first).trim()
        val gospel = Gospel.entries.find { prefix.startsWith(it.koreanName) || prefix.startsWith(it.shortName) }
        val spokenPrefix = gospel?.let { "${it.shortName}복음서" } ?: prefix
        return "$spokenPrefix ${spokenSinoKoreanNumber(number)}장"
    }

    fun playbackButtonLabel(chapter: BibleChapter, isPlaying: Boolean): String {
        val spoken = spokenChapterTitle(chapter)
        return if (isPlaying) "$spoken 중지 버튼" else "$spoken 재생 버튼"
    }

    fun playbackButtonHint(isPlaying: Boolean): String {
        return if (isPlaying) "재생을 멈춥니다" else "선택한 장을 재생합니다"
    }

    fun gospelPickerLabel(gospel: Gospel, isSelected: Boolean): String {
        return if (isSelected) {
            "${gospel.koreanName}, 선택됨"
        } else {
            "${gospel.koreanName}, 선택"
        }
    }

    /** Spoken once when resuming playback (not on fresh play or row focus). */
    fun resumeProgressAnnouncement(positionMs: Int, durationMs: Int): String {
        val elapsed = spokenDuration((positionMs / 1000).toLong())
        val total = spokenDuration((durationMs / 1000).toLong())
        val percent = ((positionMs.toFloat() / durationMs) * 100).toInt().coerceIn(0, 100)
        return "$elapsed / $total, ${percent}% 재생됨"
    }

    fun spokenDuration(totalSeconds: Long): String {
        if (totalSeconds < 0) return "0초"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}시간 ${minutes}분 ${seconds}초"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
    }

    fun sleepTimerOptionLabel(title: String, isSelected: Boolean): String {
        return if (isSelected) "$title, 선택됨" else title
    }

    fun sleepTimerOptionHint(title: String): String {
        return "수면 타이머를 $title 로 설정"
    }
}
