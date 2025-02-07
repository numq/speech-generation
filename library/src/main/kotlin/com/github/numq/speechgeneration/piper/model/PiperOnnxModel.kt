package com.github.numq.speechgeneration.piper.model

internal interface PiperOnnxModel : AutoCloseable {
    fun generate(
        phonemeIds: LongArray,
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sid: Long?,
    ): Result<FloatArray>
}