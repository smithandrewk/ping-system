package com.delta.ping.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.delta.ping.data.PingRequest
import com.delta.ping.network.ApiClient
import com.delta.ping.utils.DeviceUtils
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.pow

class PingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "PingWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "PingWorker started")
        
        val deviceId = DeviceUtils.getDeviceId(applicationContext)
        val pingRequest = PingRequest(deviceId)
        
        return try {
            sendPingWithRetry(pingRequest)
            Log.d(TAG, "Ping sent successfully for device: $deviceId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping after all retries", e)
            Result.failure()
        }
    }
    
    private suspend fun sendPingWithRetry(request: PingRequest) {
        var lastException: Exception? = null
        
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val response = ApiClient.pingApiService.sendPing(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Ping successful on attempt ${attempt + 1}")
                    return
                } else {
                    val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                    Log.w(TAG, "Ping failed on attempt ${attempt + 1}: $errorMsg")
                    lastException = HttpException(response)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network error on attempt ${attempt + 1}: ${e.message}")
                lastException = e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error on attempt ${attempt + 1}", e)
                lastException = e
            }
            
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val delayMs = calculateBackoffDelay(attempt)
                Log.d(TAG, "Retrying in ${delayMs}ms...")
                delay(delayMs)
            }
        }
        
        throw lastException ?: RuntimeException("All retry attempts failed")
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        return (BASE_DELAY_MS * 2.0.pow(attempt.toDouble())).toLong()
    }
}