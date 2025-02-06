package com.github.numq.tts.piper

data class PiperConfiguration(
    val modelConfig: ModelConfig,
    val phonemeConfig: PhonemeConfig,
    val synthesisConfig: SynthesisConfig,
) {
    data class ModelConfig(
        var numSpeakers: Int = 0,
        var speakerIdMap: MutableMap<String, Int> = mutableMapOf(),
    )

    data class PhonemeConfig(
        var voice: String = "en-us",
        var phonemeType: PhonemeType = PhonemeType.TEXT,
        var phonemeIdMap: MutableMap<Char, List<Long>> = mutableMapOf(),
    )

    data class SynthesisConfig(
        var sampleRate: Int = 22050,
        var channels: Int = 1,
        var noiseScale: Float = 0.667f,
        var lengthScale: Float = 1.0f,
        var noiseW: Float = 0.8f,
        var phonemeSilenceSeconds: MutableMap<Char, Float>? = null,
    )
}