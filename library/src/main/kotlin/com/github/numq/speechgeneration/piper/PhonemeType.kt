package com.github.numq.speechgeneration.piper

import com.google.gson.annotations.SerializedName

enum class PhonemeType {
    @SerializedName("text")
    TEXT,

    @SerializedName("espeak")
    ESPEAK
}