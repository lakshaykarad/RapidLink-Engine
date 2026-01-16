package com.example.systemmonitor.ui.theme.Screens

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Lifecycling
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.ui.theme.ViewModel.MapScreenViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun RapidMapScreen(
    viewModel: MapScreenViewModel = hiltViewModel()
){
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }


    LaunchedEffect(searchState) {
        if (searchState is Resource.Success){
            val result = searchState.data
            if (!result.isNullOrEmpty()) {
                val city = result[0]

                val lat = city.lat?.toDoubleOrNull()
                val lon = city.lon?.toDoubleOrNull()

                if (lat == null || lon == null) {
                    Log.e("MapScreen", "Invalid lat/lon: lat=${city.lat}, lon=${city.lon}")
                    return@LaunchedEffect
                }

                val location = LatLng(lat, lon)

                mapController?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(location, 14.00),
                    2000
                )
            }
        }
    }


    Scaffold (
        modifier = Modifier.fillMaxSize()
    ){ paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)){
            MapLibreView(
                onMapReady = {map ->
                    mapController = map
                }
            )
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.0.dp), // Add some padding so it doesn't touch the edges
                placeholder = { Text("Search City (e.g. Jaipur)") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchLocation() }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            if (searchState is Resource.Loading){
                CircularProgressIndicator(modifier = Modifier.align (Alignment.Center))
            }

        }

    }
}


@Composable
fun MapLibreView(
    onMapReady: (MapLibreMap) -> Unit
){

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
              map.setStyle("https://api.maptiler.com/maps/streets/style.json?key=urVJndTUzEBx0xaseMA6")

                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false // careful with this, usually you need to keep it for free plans
                onMapReady(map)
            }

            mapView
        }
    )

}

