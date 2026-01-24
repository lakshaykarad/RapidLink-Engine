package com.example.systemmonitor.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Configuration
import com.example.systemmonitor.MainActivity
import com.example.systemmonitor.R
import com.example.systemmonitor.data.local.LocationDao
import com.example.systemmonitor.data.local.LocationEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service(), LocationListener, SensorEventListener {

    @Inject
    lateinit var locationDao: LocationDao

    private lateinit var locationManager: LocationManager // Location Manager to manage location

    // Creating custom service so our app can work if one task faild
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Sensor Manager for Step Counter
    private lateinit var sensorManager: SensorManager
    private var stepSensor : Sensor? = null
    private var initialStep = -1

    override fun onCreate() {
        super.onCreate()
        // Location
        locationManager  = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Step Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.d("TrackingService","No Sensor Found on this device")
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                // for running our GPS we pass string that match with foreground service
                ServiceConstants.ACTION_START_OR_RESUME_SERVICE -> {
                    startForegroundService()
                    startLocationUpdates()
                    startStepCounting()
                }
                // this is for stop
                ServiceConstants.ACTION_STOP_SERVICE ->{
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopLocationUpdates()
                    stopStepCounting()
                    stopSelf() // kill the service
                }
            }
        }
        return START_STICKY
    }

    private fun startStepCounting(){
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("TrackingService", "Started Step Counting")
        }
    }

    private fun stopStepCounting(){
        stepSensor?.let {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER){
                val totalStepSinceBoot = it.values[0].toInt()

                if (initialStep == -1){
                    initialStep = totalStepSinceBoot
                }

                val currentSessionSteps = totalStepSinceBoot - initialStep
                Log.d("TrackingService","Step $currentSessionSteps")

            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {

    }

    @SuppressLint("MissingPermission") // using it because we already did it in main-activity
    private fun startLocationUpdates(){ // updating the location state

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("TrackingService", "No Location Permission! stopping updates.")
            stopSelf() // Stop the service to prevent crash
            return
        }

        try {
            // GPS Provider work outdoor only and more accurate
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                2f,
                this,
            )
            // work more perfectly inside indoor and less accurate.
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                2f,
                this
            )
            Log.d("TrackingService", "Started Location Updates")
        }catch (e : Exception){
            Log.d("TrackingService", "Error error location ${e.message}")
        }
    }

    private fun stopLocationUpdates(){
        locationManager.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lan = location.longitude
        serviceScope.launch {
            val entity = LocationEntity(
                latitude = lat,
                longitude = lan
                // We don't need to store time here.
            )
            locationDao.insertLocation(entity)
        }

        Log.d("TrackingService", "NEW LOCATION $lat, $lan")
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        super.onStatusChanged(provider, status, extras)
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
        serviceScope.cancel()
    }
}