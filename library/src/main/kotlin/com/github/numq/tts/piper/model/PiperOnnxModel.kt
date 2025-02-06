package com.github.numq.tts.piper.model

internal interface PiperOnnxModel : AutoCloseable {
    fun generate(
        phonemeIds: LongArray,
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sid: Long?,
    ): Result<FloatArray>
}