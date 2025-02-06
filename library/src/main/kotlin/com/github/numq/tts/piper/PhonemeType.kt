package com.github.numq.tts.piper

import com.google.gson.annotations.SerializedName

enum class PhonemeType {
    @SerializedName("text")
    TEXT,

    @SerializedName("espeak")
    ESPEAK
}