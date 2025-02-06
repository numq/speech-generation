package com.github.numq.tts.bark

import com.github.numq.tts.TextToSpeech

internal class BarkTextToSpeech(private val nativeBarkTextToSpeech: NativeBarkTextToSpeech) : TextToSpeech.Bark {
    override val sampleRate = 22_500

    private fun convertFLT32ToPCM16(inputBytes: ByteArray): ByteArray {
        val inputSize = inputBytes.size / 4
        val outputBytes = ByteArray(inputSize * 2)

        for (i in 0 until inputSize) {
            val sample = Float.fromBits(
                (inputBytes[i * 4].toInt() and 0xFF) or
                        ((inputBytes[i * 4 + 1].toInt() and 0xFF) shl 8) or
                        ((inputBytes[i * 4 + 2].toInt() and 0xFF) shl 16) or
                        ((inputBytes[i * 4 + 3].toInt() and 0xFF) shl 24)
            )
            val scaledSample =
                (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outputBytes[i * 2] = (scaledSample and 0xFF).toByte()
            outputBytes[i * 2 + 1] = ((scaledSample shr 8) and 0xFF).toByte()
        }

        return outputBytes
    }

    override suspend fun generate(text: String) = runCatching {
        convertFLT32ToPCM16(nativeBarkTextToSpeech.generate(text = text))
    }

    override fun close() = runCatching {
        super.close()

        nativeBarkTextToSpeech.close()
    }.getOrDefault(Unit)
}