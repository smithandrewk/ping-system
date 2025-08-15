package com.delta.ping.data

import com.google.gson.annotations.SerializedName

data class PingRequest(
    @SerializedName("device_id")
    val deviceId: String
)