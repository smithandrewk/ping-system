package com.delta.ping.data

import com.google.gson.annotations.SerializedName

data class PingResponse(
    @SerializedName("message")
    val message: String
)

data class ErrorResponse(
    @SerializedName("error")
    val error: String
)