package com.github.numq.speechgeneration.piper.model

internal interface TashkeelOnnxModel : AutoCloseable {
    fun process(text: String): Result<String>
}