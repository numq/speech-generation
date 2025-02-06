package playback

sealed interface PlaybackState {
    data object Empty : PlaybackState

    sealed interface Generated : PlaybackState {
        val pcmBytes: ByteArray
        val sampleRate: Int

        data class Stopped(override val pcmBytes: ByteArray, override val sampleRate: Int) : Generated {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Stopped

                if (!pcmBytes.contentEquals(other.pcmBytes)) return false
                if (sampleRate != other.sampleRate) return false

                return true
            }

            override fun hashCode(): Int {
                var result = pcmBytes.contentHashCode()
                result = 31 * result + sampleRate
                return result
            }
        }

        data class Playing(override val pcmBytes: ByteArray, override val sampleRate: Int) : Generated {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Playing

                if (!pcmBytes.contentEquals(other.pcmBytes)) return false
                if (sampleRate != other.sampleRate) return false

                return true
            }

            override fun hashCode(): Int {
                var result = pcmBytes.contentHashCode()
                result = 31 * result + sampleRate
                return result
            }
        }
    }
}