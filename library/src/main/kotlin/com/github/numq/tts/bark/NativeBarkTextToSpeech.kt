package com.github.numq.tts.bark

import java.lang.ref.Cleaner

internal class NativeBarkTextToSpeech(modelPath: String) : AutoCloseable {
    private val nativeHandle = initNative(modelPath = modelPath).also { handle ->
        require(handle != -1L) { "Unable to initialize native library" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        external fun initNative(modelPath: String): Long

        @JvmStatic
        external fun generateNative(handle: Long, text: String): ByteArray

        @JvmStatic
        external fun freeNative(handle: Long)
    }

    fun generate(text: String) = generateNative(handle = nativeHandle, text = text)

    override fun close() = cleanable.clean()
}