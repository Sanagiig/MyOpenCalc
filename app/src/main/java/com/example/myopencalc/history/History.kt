package com.example.myopencalc.history

import java.util.UUID
import com.google.gson.annotations.SerializedName

data class History(
    @SerializedName("calculation") var calculation: String,
    @SerializedName("result") var result: String,
    @SerializedName("time") var time: String,
    @SerializedName("id") var id: String = UUID.randomUUID().toString()
)
