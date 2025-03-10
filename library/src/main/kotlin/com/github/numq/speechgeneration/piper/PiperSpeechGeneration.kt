package com.github.numq.speechgeneration.piper

import com.github.numq.speechgeneration.SpeechGeneration
import com.github.numq.speechgeneration.piper.model.PiperOnnxModel
import com.github.numq.speechgeneration.piper.model.TashkeelOnnxModel
import java.io.ByteArrayOutputStream
import java.text.Normalizer

internal class PiperSpeechGeneration(
    private val nativePiperSpeechGeneration: NativePiperSpeechGeneration,
    private val piperModel: PiperOnnxModel,
    private val tashkeelModel: TashkeelOnnxModel,
    configuration: PiperConfiguration,
) : SpeechGeneration.Piper {
    private companion object {
        private const val CLAUSE_TYPE_CLAUSE = 0x00040000
        private const val CLAUSE_TYPE_SENTENCE = 0x00080000

        private const val CLAUSE_INTONATION_FULL_STOP = 0x00000040
        private const val CLAUSE_INTONATION_COMMA = 0x00000020
        private const val CLAUSE_INTONATION_QUESTION = 0x00000080
        private const val CLAUSE_INTONATION_EXCLAMATION = 0x00000100

        private const val CLAUSE_PERIOD = 0x00000001 or CLAUSE_INTONATION_FULL_STOP or CLAUSE_TYPE_SENTENCE
        private const val CLAUSE_COMMA = 0x00000002 or CLAUSE_INTONATION_COMMA or CLAUSE_TYPE_CLAUSE
        private const val CLAUSE_QUESTION = 0x00000004 or CLAUSE_INTONATION_QUESTION or CLAUSE_TYPE_SENTENCE
        private const val CLAUSE_EXCLAMATION = 0x00000008 or CLAUSE_INTONATION_EXCLAMATION or CLAUSE_TYPE_SENTENCE
        private const val CLAUSE_COLON = 0x00000010 or CLAUSE_INTONATION_FULL_STOP or CLAUSE_TYPE_CLAUSE
        private const val CLAUSE_SEMICOLON = 0x00000020 or CLAUSE_INTONATION_COMMA or CLAUSE_TYPE_CLAUSE

        val DEFAULT_PHONEME_ID_MAP = mapOf(
            '_' to 0L,
            '^' to 1L,
            '$' to 2L,
            ' ' to 3L,
            '!' to 4L,
            '\'' to 5L,
            '(' to 6L,
            ')' to 7L,
            ',' to 8L,
            '-' to 9L,
            '.' to 10L,
            ':' to 11L,
            ';' to 12L,
            '?' to 13L,
            'a' to 14L,
            'b' to 15L,
            'c' to 16L,
            'd' to 17L,
            'e' to 18L,
            'f' to 19L,
            'h' to 20L,
            'i' to 21L,
            'j' to 22L,
            'k' to 23L,
            'l' to 24L,
            'm' to 25L,
            'n' to 26L,
            'o' to 27L,
            'p' to 28L,
            'q' to 29L,
            'r' to 30L,
            's' to 31L,
            't' to 32L,
            'u' to 33L,
            'v' to 34L,
            'w' to 35L,
            'x' to 36L,
            'y' to 37L,
            'z' to 38L,
            'æ' to 39L,
            'ç' to 40L,
            'ð' to 41L,
            'ø' to 42L,
            'ħ' to 43L,
            'ŋ' to 44L,
            'œ' to 45L,
            'ǀ' to 46L,
            'ǁ' to 47L,
            'ǂ' to 48L,
            'ǃ' to 49L,
            'ɐ' to 50L,
            'ɑ' to 51L,
            'ɒ' to 52L,
            'ɓ' to 53L,
            'ɔ' to 54L,
            'ɕ' to 55L,
            'ɖ' to 56L,
            'ɗ' to 57L,
            'ɘ' to 58L,
            'ə' to 59L,
            'ɚ' to 60L,
            'ɛ' to 61L,
            'ɜ' to 62L,
            'ɞ' to 63L,
            'ɟ' to 64L,
            'ɠ' to 65L,
            'ɡ' to 66L,
            'ɢ' to 67L,
            'ɣ' to 68L,
            'ɤ' to 69L,
            'ɥ' to 70L,
            'ɦ' to 71L,
            'ɧ' to 72L,
            'ɨ' to 73L,
            'ɪ' to 74L,
            'ɫ' to 75L,
            'ɬ' to 76L,
            'ɭ' to 77L,
            'ɮ' to 78L,
            'ɯ' to 79L,
            'ɰ' to 80L,
            'ɱ' to 81L,
            'ɲ' to 82L,
            'ɳ' to 83L,
            'ɴ' to 84L,
            'ɵ' to 85L,
            'ɶ' to 86L,
            'ɸ' to 87L,
            'ɹ' to 88L,
            'ɺ' to 89L,
            'ɻ' to 90L,
            'ɽ' to 91L,
            'ɾ' to 92L,
            'ʀ' to 93L,
            'ʁ' to 94L,
            'ʂ' to 95L,
            'ʃ' to 96L,
            'ʄ' to 97L,
            'ʈ' to 98L,
            'ʉ' to 99L,
            'ʊ' to 100L,
            'ʋ' to 101L,
            'ʌ' to 102L,
            'ʍ' to 103L,
            'ʎ' to 104L,
            'ʏ' to 105L,
            'ʐ' to 106L,
            'ʑ' to 107L,
            'ʒ' to 108L,
            'ʔ' to 109L,
            'ʕ' to 110L,
            'ʘ' to 111L,
            'ʙ' to 112L,
            'ʛ' to 113L,
            'ʜ' to 114L,
            'ʝ' to 115L,
            'ʟ' to 116L,
            'ʡ' to 117L,
            'ʢ' to 118L,
            'ʲ' to 119L,
            'ˈ' to 120L,
            'ˌ' to 121L,
            'ː' to 122L,
            'ˑ' to 123L,
            '˞' to 124L,
            'β' to 125L,
            'θ' to 126L,
            'χ' to 127L,
            'ᵻ' to 128L,
            'ⱱ' to 129L,

            // tones
            '0' to 130L,
            '1' to 131L,
            '2' to 132L,
            '3' to 133L,
            '4' to 134L,
            '5' to 135L,
            '6' to 136L,
            '7' to 137L,
            '8' to 138L,
            '9' to 139L,
            '\u0327' to 140,
            '\u0303' to 141,
            '\u032a' to 142,
            '\u032f' to 143,
            '\u0329' to 144,
            'ʰ' to 145,
            'ˤ' to 146,
            'ε' to 147,
            '↓' to 148,
            '#' to 149,
            '"' to 150,
            '↑' to 151,
            '\u033a' to 152,
            '\u033b' to 153,
            'g' to 154,
            'ʦ' to 155,
            'X' to 156,
            '\u031d' to 157,
            '\u030a' to 158
        )
    }

    private val modelConfig = configuration.modelConfig.apply {
        require(languageCode.isNotBlank()) { "Invalid Piper model language code" }
    }

    private val phonemeConfig = configuration.phonemeConfig.apply {
        require(voice.isNotBlank()) { "Invalid Piper voice" }
    }

    private val synthesisConfig = configuration.synthesisConfig

    private fun phonemesToIds(
        phonemes: List<Char>,
        config: PhonemeIdConfiguration,
        phonemeIds: MutableList<Long>,
        missingPhonemes: MutableMap<Char, Long>,
    ) {
        val phonemeIdMap = config.phonemeIdMap ?: DEFAULT_PHONEME_ID_MAP

        if (config.addBos) {
            phonemeIdMap[config.bos]?.let(phonemeIds::add)

            if (config.interspersePad) {
                phonemeIdMap[config.pad]?.let(phonemeIds::add)
            }
        }

        if (config.interspersePad) {
            for (phoneme in phonemes) {
                if (phoneme !in phonemeIdMap) {
                    missingPhonemes[phoneme] = missingPhonemes.getOrDefault(phoneme, 0) + 1
                    continue
                }

                phonemeIdMap[phoneme]?.let(phonemeIds::add)

                phonemeIdMap[config.pad]?.let(phonemeIds::add)
            }
        } else {
            for (phoneme in phonemes) {
                phonemeIdMap[phoneme]?.let(phonemeIds::add)
            }
        }

        if (config.addEos) {
            phonemeIdMap[config.eos]?.let(phonemeIds::add)
        }
    }

    private fun convertFLT32ToPCM16(inputSamples: FloatArray): ByteArray {
        val outputBytes = ByteArray(inputSamples.size * 2)

        for (i in inputSamples.indices) {
            val sample = inputSamples[i]
            val scaledSample =
                (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            outputBytes[i * 2] = (scaledSample and 0xFF).toByte()
            outputBytes[i * 2 + 1] = ((scaledSample shr 8) and 0xFF).toByte()
        }

        return outputBytes
    }

    override val sampleRate = synthesisConfig.sampleRate

    override val channels = synthesisConfig.channels

    override val numSpeakers = modelConfig.numSpeakers

    override suspend fun generate(text: String, speakerId: Long) = runCatching {
        val phonemes: MutableList<Char> = mutableListOf()

        val phonemeIds = mutableListOf<Long>()

        val missingPhonemes = mutableMapOf<Char, Long>()

        var unprocessedText = text.trim()

        if (modelConfig.languageCode.startsWith("ar")) {
            unprocessedText = tashkeelModel.process(unprocessedText).getOrThrow()
        }

        while (unprocessedText.isNotBlank()) {
            val phonemizationResult = nativePiperSpeechGeneration.phonemize(
                voice = phonemeConfig.voice,
                text = unprocessedText
            )

            check(phonemizationResult.isNotEmpty()) { "Failed to phonemize text" }

            val remainingText = phonemizationResult[0] ?: ""

            unprocessedText = remainingText

            val rawPhonemes = phonemizationResult[1] ?: ""

            val clauseTerminator = phonemizationResult[2]?.toIntOrNull() ?: 0

            val normalizedPhonemes = Normalizer.normalize(rawPhonemes, Normalizer.Form.NFD)

            phonemes.addAll(normalizedPhonemes.toList())

            when (clauseTerminator and 0x000FFFFF) {
                CLAUSE_EXCLAMATION -> phonemes.add('!')

                CLAUSE_QUESTION -> phonemes.add('?')

                CLAUSE_COMMA -> phonemes.add(',')

                CLAUSE_COLON -> phonemes.add(':')

                CLAUSE_SEMICOLON -> phonemes.add(';')

                CLAUSE_PERIOD -> phonemes.add('.')
            }

            if ((clauseTerminator and CLAUSE_TYPE_SENTENCE) == CLAUSE_TYPE_SENTENCE) {
                phonemes.add('\n')
            } else {
                phonemes.add(' ')
            }
        }

        ByteArrayOutputStream().use { baos ->
            phonemesToIds(phonemes, PhonemeIdConfiguration(), phonemeIds, missingPhonemes)

            baos.apply {
                writeBytes(
                    with(synthesisConfig) {
                        piperModel.generate(
                            phonemeIds = phonemeIds.toLongArray(),
                            noiseScale = noiseScale,
                            lengthScale = lengthScale,
                            noiseW = noiseW,
                            sid = speakerId.coerceIn(0L, numSpeakers - 1L)
                        ).mapCatching(::convertFLT32ToPCM16).getOrThrow()
                    }
                )
            }.toByteArray()
        }
    }

    override fun close() = runCatching {
        piperModel.close()
        tashkeelModel.close()
    }.getOrDefault(Unit)
}