package com.ps168.idr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LoggingService : Service() {

    private lateinit var logger: SensorLogger
    private val channelId = "ps168_logging_channel"

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        var isServiceRunning = false
            private set
        var currentLogger: SensorLogger? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        logger = SensorLogger(applicationContext)
        currentLogger = logger
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(1, buildNotification("Logging sensors..."))
                try {
                    logger.start()
                    isServiceRunning = true
                } catch (e: Exception) {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                logger.stop()
                isServiceRunning = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isServiceRunning) {
            logger.stop()
            isServiceRunning = false
        }
        currentLogger = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "PS168 Logging", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PS168 Sensor Logger")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }
}