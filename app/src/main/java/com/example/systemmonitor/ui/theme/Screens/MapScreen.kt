package com.example.systemmonitor.ui.theme.Screens

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Lifecycling
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.systemmonitor.ui.theme.ViewModel.MapScreenViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

@Composable
fun RapidMapScreen(
    viewModel: MapScreenViewModel = hiltViewModel()
){
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold (
        modifier = Modifier.fillMaxSize()
    ){ paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)){
            MapLibreView()
            OutlinedTextField(
                value = query,
                onValueChange = {newQuery->
                    viewModel.onSearchQueryChange(newQuery)
                },
                modifier = Modifier.fillMaxWidth(),
            )

        }

    }
}


@Composable
fun MapLibreView(){

    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    remember {
        MapLibre.getInstance(context)
    }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }


    DisposableEffect(lifecycle,mapView) {

        val lifecycleObserver = LifecycleEventObserver{ _, event->
            when(event){
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }

    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
              map.setStyle("https://api.maptiler.com/maps/streets/style.json?key=urVJndTUzEBx0xaseMA6") { style ->

              }

                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false // careful with this, usually you need to keep it for free plans
            }
            mapView
        }
    )

}

