package com.delta.ping.network

import com.delta.ping.data.PingRequest
import com.delta.ping.data.PingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PingApiService {
    @POST("ping")
    suspend fun sendPing(@Body request: PingRequest): Response<PingResponse>
}