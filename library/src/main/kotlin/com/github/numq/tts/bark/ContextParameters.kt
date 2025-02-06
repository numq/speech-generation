package com.github.numq.tts.bark

data class ContextParameters(
    val verbosity: Int,
    val temp: Float,
    val fineTemp: Float,
    val minEosP: Float,
    val slidingWindowSize: Int,
    val maxCoarseHistory: Int,
    val sampleRate: Int,
    val targetBandwidth: Int,
    val clsTokenId: Int,
    val sepTokenId: Int,
    val nStepsTextEncoder: Int,
    val textPadToken: Int,
    val textEncodingOffset: Int,
    val semanticRateHz: Float,
    val semanticPadToken: Int,
    val semanticVocabSize: Int,
    val semanticInferToken: Int,
    val coarseRateHz: Float,
    val coarseInferToken: Int,
    val coarseSemanticPadToken: Int,
    val nCoarseCodebooks: Int,
    val nFineCodebooks: Int,
    val codebookSize: Int,
    val progressCallback: Long,
    val progressCallbackUserData: Long,
)

// todo: UNUSED