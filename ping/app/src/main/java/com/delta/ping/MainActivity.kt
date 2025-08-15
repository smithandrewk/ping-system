package com.delta.ping

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.delta.ping.service.PingService
import com.delta.ping.utils.DeviceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var deviceIdText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var batteryOptButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        
        displayDeviceId()
        startStatusUpdates()
    }
    
    private fun initViews() {
        statusText = findViewById(R.id.status_text)
        deviceIdText = findViewById(R.id.device_id_text)
        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)
        batteryOptButton = findViewById(R.id.battery_opt_button)
    }
    
    private fun setupClickListeners() {
        startButton.setOnClickListener {
            PingService.startService(this)
        }
        
        stopButton.setOnClickListener {
            PingService.stopService(this)
        }
        
        batteryOptButton.setOnClickListener {
            requestBatteryOptimizationExemption()
        }
    }
    
    private fun displayDeviceId() {
        val deviceId = DeviceUtils.getDeviceId(this)
        deviceIdText.text = "Device ID: $deviceId"
    }
    
    private fun startStatusUpdates() {
        lifecycleScope.launch {
            while (true) {
                updateServiceStatus()
                delay(2000)
            }
        }
    }
    
    private fun updateServiceStatus() {
        val isServiceRunning = isServiceRunning()
        val workStatus = getWorkStatus()
        
        val statusMessage = buildString {
            appendLine("Service: ${if (isServiceRunning) "Running" else "Stopped"}")
            appendLine("Ping Work: $workStatus")
            appendLine("Last Update: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        }
        
        statusText.text = statusMessage
        
        startButton.isEnabled = !isServiceRunning
        stopButton.isEnabled = isServiceRunning
    }
    
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == PingService::class.java.name }
    }
    
    private fun getWorkStatus(): String {
        val workManager = WorkManager.getInstance(this)
        val workInfos = workManager.getWorkInfosForUniqueWork("periodic_ping_work")
        
        return try {
            val workInfo = workInfos.get().firstOrNull()
            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED -> "Scheduled"
                WorkInfo.State.RUNNING -> "Running"
                WorkInfo.State.SUCCEEDED -> "Last Run: Success"
                WorkInfo.State.FAILED -> "Last Run: Failed"
                WorkInfo.State.BLOCKED -> "Blocked"
                WorkInfo.State.CANCELLED -> "Cancelled"
                null -> "Not Scheduled"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallbackIntent)
        }
    }
}