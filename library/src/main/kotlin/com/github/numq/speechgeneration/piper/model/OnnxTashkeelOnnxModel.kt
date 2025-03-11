package com.github.numq.speechgeneration.piper.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

internal class OnnxTashkeelOnnxModel(modelPath: String) : TashkeelOnnxModel {
    private companion object {
        const val PAD_ID = 0
        const val UNK_ID = 1
        const val MAX_INPUT_CHARS = 315

        val harakatChars = setOf('\u064c', '\u064d', '\u064e', '\u064f', '\u0650', '\u0651', '\u0652')

        val invalidHarakaIds = setOf(UNK_ID, 8)

        val inputVocab: Map<Char, Int> = mapOf(
            '\u0009' to 8,
            '\u0020' to 28,
            '\u00a0' to 84,
            '\u00ab' to 74,
            '\u00ad' to 40,
            '\u00b0' to 5,
            '\u00b4' to 110,
            '\u00bb' to 30,
            '\u03ad' to 69,
            '\u03af' to 112,
            '\u03b1' to 47,
            '\u03b3' to 80,
            '\u03b5' to 7,
            '\u03b8' to 51,
            '\u03b9' to 36,
            '\u03ba' to 35,
            '\u03bc' to 54,
            '\u03bd' to 63,
            '\u03bf' to 114,
            '\u03c0' to 116,
            '\u03c1' to 26,
            '\u03c3' to 27,
            '\u03c4' to 78,
            '\u03c5' to 20,
            '\u03c7' to 14,
            '\u03c8' to 12,
            '\u03c9' to 89,
            '\u03cc' to 77,
            '\u03ce' to 103,
            '\u05d5' to 64,
            '\u061b' to 17,
            '\u061f' to 101,
            '\u0621' to 120,
            '\u0622' to 15,
            '\u0623' to 73,
            '\u0624' to 50,
            '\u0625' to 119,
            '\u0626' to 56,
            '\u0627' to 68,
            '\u0628' to 118,
            '\u0629' to 107,
            '\u062a' to 22,
            '\u062b' to 71,
            '\u062c' to 59,
            '\u062d' to 86,
            '\u062e' to 19,
            '\u062f' to 104,
            '\u0630' to 97,
            '\u0631' to 65,
            '\u0632' to 92,
            '\u0633' to 82,
            '\u0634' to 18,
            '\u0635' to 75,
            '\u0636' to 111,
            '\u0637' to 93,
            '\u0638' to 11,
            '\u0639' to 95,
            '\u063a' to 24,
            '\u0640' to 9,
            '\u0641' to 46,
            '\u0642' to 38,
            '\u0643' to 72,
            '\u0644' to 29,
            '\u0645' to 48,
            '\u0646' to 81,
            '\u0647' to 49,
            '\u0648' to 6,
            '\u0649' to 39,
            '\u064a' to 70,
            '\u066a' to 91,
            '\u0670' to 45,
            '\u0671' to 67,
            '\u06cc' to 105,
            '\u06d2' to 37,
            '\u06f5' to 109,
            '\u06f7' to 106,
            '\u06f8' to 10,
            '\u200b' to 52,
            '\u200d' to 31,
            '\u200e' to 117,
            '\u200f' to 60,
            '\u2013' to 42,
            '\u2018' to 34,
            '\u2019' to 41,
            '\u201c' to 55,
            '\u201d' to 85,
            '\u2022' to 62,
            '\u2026' to 23,
            '\u202b' to 94,
            '\u202c' to 108,
            '\u2030' to 115,
            '\ufb90' to 53,
            '\ufd3e' to 44,
            '\ufd3f' to 25,
            '\ufe81' to 16,
            '\ufe82' to 96,
            '\ufe83' to 87,
            '\ufe84' to 61,
            '\ufe87' to 57,
            '\ufe88' to 58,
            '\ufe8b' to 100,
            '\ufe8c' to 90,
            '\ufe91' to 32,
            '\ufe92' to 113,
            '\ufe94' to 76,
            '\ufed3' to 33,
            '\ufedb' to 13,
            '\ufedf' to 99,
            '\ufee0' to 66,
            '\ufee3' to 43,
            '\ufee7' to 102,
            '\ufef4' to 88,
            '\ufef5' to 83,
            '\ufef7' to 98,
            '\ufef9' to 21,
            '\ufefb' to 79
        )

        val outputVocab: Map<Int, List<Char>> = mapOf(
            4 to listOf('\u0640'),
            5 to listOf('\u064e'),
            6 to listOf('\u064f', '\u0651'),
            7 to listOf('\u064e', '\u0651'),
            8 to listOf('\u0640'),
            9 to listOf('\u0651', '\u0650'),
            10 to listOf('\u0651'),
            11 to listOf('\u0652', '\u0651'),
            12 to listOf('\u0651', '\u064d'),
            13 to listOf('\u0650', '\u0651'),
            14 to listOf('\u064d', '\u0651'),
            15 to listOf('\u064c', '\u0651'),
            16 to listOf('\u0651', '\u064e'),
            17 to listOf('\u064f'),
            18 to listOf('\u0651', '\u064c'),
            19 to listOf('\u0651', '\u064b'),
            20 to listOf('\u0652'),
            21 to listOf('\u064d'),
            22 to listOf('\u0650'),
            23 to listOf('\u0651', '\u064f'),
            24 to listOf('\u064b', '\u0651'),
            25 to listOf('\u064c'),
            26 to listOf('\u064b'),
            27 to listOf('\u0651', '\u0651')
        )
    }

    private val env by lazy { OrtEnvironment.getEnvironment() }

    private val session by lazy {
        env.createSession(modelPath, OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
        })
    }

    override fun process(text: String) = runCatching {
        val strippedText = text.filterNot { it in harakatChars }

        val inputIds = strippedText.map { inputVocab[it] ?: UNK_ID }.let {
            it + List(MAX_INPUT_CHARS - it.size) { PAD_ID }
        }

        OnnxTensor.createTensor(env, arrayOf(inputIds.map(Int::toFloat).toFloatArray())).use { inputTensor ->
            val inputs = mapOf("embedding_7_input" to inputTensor)

            session.run(inputs).use { output ->
                val outputTensor = output["dense_7"].get() as OnnxTensor

                val outputData = outputTensor.floatBuffer

                val outputShape = outputTensor.info.shape

                val numOutputChars = outputShape[1].toInt()

                val numOutputProbs = outputShape[2].toInt()

                buildString {
                    strippedText.forEachIndexed { i, c ->
                        append(c)

                        if (i < numOutputChars) {
                            var maxId = 0
                            var maxProb = Float.NEGATIVE_INFINITY

                            for (j in 0 until numOutputProbs) {
                                val prob = outputData[(i * numOutputProbs) + j]
                                if (prob > maxProb) {
                                    maxProb = prob
                                    maxId = j
                                }
                            }

                            if (maxId !in invalidHarakaIds && outputVocab.containsKey(maxId)) {
                                outputVocab[maxId]?.forEach { append(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun close() = runCatching { session.close() }.getOrDefault(Unit)
}