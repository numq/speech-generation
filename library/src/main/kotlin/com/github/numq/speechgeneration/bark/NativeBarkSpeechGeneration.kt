package com.github.numq.speechgeneration.bark

import java.lang.ref.Cleaner

internal class NativeBarkSpeechGeneration(modelPath: String, temperature: Float, seed: Long) : AutoCloseable {
    private val nativeHandle = initNative(
        modelPath = modelPath,
        temperature = temperature,
        seed = seed
    ).also { handle ->
        require(handle != -1L) { "Unable to initialize native library" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        external fun initNative(modelPath: String, temperature: Float, seed: Long): Long

        @JvmStatic
        external fun generateNative(handle: Long, text: String): ByteArray

        @JvmStatic
        external fun freeNative(handle: Long)
    }

    fun generate(text: String) = generateNative(handle = nativeHandle, text = text)

    override fun close() = cleanable.clean()
}