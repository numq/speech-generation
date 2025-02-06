package generation

import kotlin.time.Duration

sealed interface GenerationResult {
    data object Empty : GenerationResult

    data class Content(
        val text: String,
        val pcmBytes: ByteArray,
        val sampleRate: Int,
        val elapsedTime: Duration,
    ) : GenerationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Content

            if (text != other.text) return false
            if (!pcmBytes.contentEquals(other.pcmBytes)) return false
            if (sampleRate != other.sampleRate) return false
            if (elapsedTime != other.elapsedTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + pcmBytes.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + elapsedTime.hashCode()
            return result
        }
    }
}