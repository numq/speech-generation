package com.github.numq.speechgeneration.piper.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

internal class OnnxPiperOnnxModel(modelPath: String) : PiperOnnxModel {
    private val env by lazy { OrtEnvironment.getEnvironment() }

    private val session by lazy {
        env.createSession(modelPath, OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT)
        })
    }

    private fun extractTensorData(output: OrtSession.Result): FloatArray {
        val outputTensor = output.firstOrNull()?.value as? OnnxTensor

        val tensorData = outputTensor?.value as? Array<Array<Array<FloatArray>>>

        return tensorData?.lastOrNull()?.lastOrNull()?.lastOrNull() ?: floatArrayOf()
    }

    override fun generate(
        phonemeIds: LongArray,
        noiseScale: Float,
        lengthScale: Float,
        noiseW: Float,
        sid: Long?,
    ) = runCatching {
        OnnxTensor.createTensor(env, arrayOf(phonemeIds)).use { inputTensor ->
            OnnxTensor.createTensor(env, longArrayOf(phonemeIds.size.toLong())).use { lengthTensor ->
                OnnxTensor.createTensor(env, floatArrayOf(noiseScale, lengthScale, noiseW)).use { scalesTensor ->
                    val inputs = mutableMapOf(
                        "input" to inputTensor,
                        "input_lengths" to lengthTensor,
                        "scales" to scalesTensor
                    )

                    sid?.let {
                        OnnxTensor.createTensor(env, longArrayOf(it)).use { sidTensor ->
                            inputs["sid"] = sidTensor
                            session.run(inputs).use { output ->
                                extractTensorData(output)
                            }
                        }
                    } ?: session.run(inputs).use { output ->
                        extractTensorData(output)
                    }
                }
            }
        }
    }

    override fun close() = runCatching { session.close() }.getOrDefault(Unit)
}
