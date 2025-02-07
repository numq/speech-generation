package com.github.numq.speechgeneration.piper

data class PhonemeIdConfiguration(
    var phonemeIdMap: Map<Char, Long>? = null,
    var pad: Char = '_',
    var bos: Char = '^',
    var eos: Char = '$',
    var interspersePad: Boolean = true,
    var addBos: Boolean = true,
    var addEos: Boolean = true,
)