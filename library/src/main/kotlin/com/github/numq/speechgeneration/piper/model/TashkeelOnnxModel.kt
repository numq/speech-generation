package com.github.numq.speechgeneration.piper.model

internal interface TashkeelOnnxModel : AutoCloseable {
    fun process(text: String): Result<String>

    companion object {
        fun create(modelPath: String): TashkeelOnnxModel = OnnxTashkeelOnnxModel(modelPath = modelPath)
    }
}