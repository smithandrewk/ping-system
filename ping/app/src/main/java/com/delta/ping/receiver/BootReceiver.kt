package com.delta.ping.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.delta.ping.service.PingService

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Boot completed, starting PingService")
                
                try {
                    PingService.startService(context)
                    Log.d(TAG, "PingService started successfully after boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start PingService after boot", e)
                }
            }
            else -> {
                Log.d(TAG, "Received unhandled intent: ${intent.action}")
            }
        }
    }
}