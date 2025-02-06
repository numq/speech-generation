package com.github.numq.tts.piper

internal class NativePiperTextToSpeech(dataPath: String) {
    init {
        initNative(dataPath = dataPath)
    }

    private companion object {
        @JvmStatic
        external fun initNative(dataPath: String)

        @JvmStatic
        external fun phonemizeNative(voice: String, text: String): Array<String?>
    }

    fun phonemize(voice: String, text: String) = phonemizeNative(voice = voice, text = text)
}