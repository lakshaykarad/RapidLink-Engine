package com.example.systemmonitor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.systemmonitor.service.ServiceConstants
import com.example.systemmonitor.service.TrackingService
import com.example.systemmonitor.ui.theme.Screens.RapidMapScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {permission ->
            // check every type of activity to run map
            val hasFineLocation = permission[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val hasCoarseLocation = permission[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val hasNotification = permission[Manifest.permission.POST_NOTIFICATIONS] == true
            
            if (hasFineLocation || hasCoarseLocation){
                startTrackingService()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotification){
                    Toast.makeText(this, "Notifications disabled, tracking silently", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this, "Map cannot work without Location", Toast.LENGTH_SHORT).show()
            }
        }

            LaunchedEffect(Unit) {
               val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        arrayOf(
                            Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                }else{
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                }
                permissionLauncher.launch(permissionsToRequest)
            }

            RapidMapScreen()
        }

    }

    private fun startTrackingService(){
        Intent(this, TrackingService::class.java).also {
            it.action = ServiceConstants.ACTION_START_OR_RESUME_SERVICE
            startService(it)
        }
    }

}