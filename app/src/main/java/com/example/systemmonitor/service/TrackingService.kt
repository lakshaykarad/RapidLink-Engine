package com.example.systemmonitor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.systemmonitor.MainActivity
import com.example.systemmonitor.R

class TrackingService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ServiceConstants.ACTION_START_OR_RESUME_SERVICE -> {
                    startForegroundService()
                }
                ServiceConstants.ACTION_STOP_SERVICE ->{
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf() // kill the service
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(){
        // Notification Manager to manage everything
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Mandatory for Displaying Notifications above then Android 8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                ServiceConstants.NOTIFICATION_CHANNEL_ID,
                ServiceConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel) // Create teh channel with the help of notification manager
        }

        // When user click on notification, come to application
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Notification code
        val notification = NotificationCompat.Builder(this, ServiceConstants.NOTIFICATION_CHANNEL_ID )
            .setAutoCancel(false) // user can't swap it.
            .setOngoing(true)   // cannot be dismissed by the user
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tracking")
            .setContentText("Tracking your location and steps....")
            .setContentIntent(pendingIntent) // pending intent user click and come to application
            .build()
        startForeground(ServiceConstants.NOTIFICATION_ID, notification)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}