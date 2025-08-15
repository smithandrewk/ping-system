package com.delta.ping.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.delta.ping.MainActivity
import com.delta.ping.R
import com.delta.ping.worker.PingWorker
import java.util.concurrent.TimeUnit

class PingService : Service() {
    
    companion object {
        private const val TAG = "PingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ping_service_channel"
        private const val PING_WORK_NAME = "periodic_ping_work"
        private const val PING_INTERVAL_MINUTES = 15L
        
        fun startService(context: Context) {
            val intent = Intent(context, PingService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, PingService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PingService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PingService started")
        
        startForeground(NOTIFICATION_ID, createNotification())
        schedulePingWork()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PingService destroyed")
        cancelPingWork()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ping Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows that ping service is running"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ping Service Active")
            .setContentText("Sending periodic pings to server")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun schedulePingWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val pingWorkRequest = PeriodicWorkRequestBuilder<PingWorker>(
            PING_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PING_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            pingWorkRequest
        )
        
        Log.d(TAG, "Ping work scheduled with ${PING_INTERVAL_MINUTES}-minute interval")
    }
    
    private fun cancelPingWork() {
        WorkManager.getInstance(this).cancelUniqueWork(PING_WORK_NAME)
        Log.d(TAG, "Ping work cancelled")
    }
}