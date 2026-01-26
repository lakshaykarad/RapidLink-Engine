package com.example.systemmonitor.ui.theme.Screens

import android.R
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import com.example.systemmonitor.data.local.LocationEntity
import com.example.systemmonitor.ui.theme.ViewModel.MapScreenViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

@Composable
fun RapidMapScreen(
    viewModel: MapScreenViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle() // Query what to search
    val searchState by viewModel.searchState.collectAsStateWithLifecycle() // search state lading success error
    // marking the points on map while walking
    val pathPoint by viewModel.pathPoints.collectAsStateWithLifecycle()
    // contorl the map with animation camera style movement etc
    var mapController by remember { mutableStateOf<MapLibreMap?>(null) }

    // draw the line on map
    val routePointsState by viewModel.routePoints.collectAsStateWithLifecycle()


    LaunchedEffect(searchState) {
        if (searchState is Resource.Success) {
            val result = searchState.data
            if (!result.isNullOrEmpty()) {
                val city = result[0] // first city that search

                val lat = city.lat?.toDoubleOrNull()
                val lon = city.lon?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val location = LatLng(lat, lon)

                    mapController?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(location, 14.00),
                        2000
                    )
                    viewModel.getRouteTo(lat, lon)
                } else {
                    Log.e("MapScreen", "Invalid lat/lon: lat=${city.lat}, lon=${city.lon}")
                    return@LaunchedEffect
                }

            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.clearPath() },
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Path")
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {
            MapLibreView(
                pathPoints = pathPoint,
                onMapReady = { map -> mapController = map },
                routePointsState = routePointsState
            )
            // Search
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
            // Loading State
            if (searchState is Resource.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

    }
}

@Composable
fun MapLibreView(
    onMapReady: (MapLibreMap) -> Unit,
    pathPoints: List<LocationEntity>,
    routePointsState: Resource<List<List<Double>>>
) {
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


    // Observering the lifecyle for mapview
    DisposableEffect(lifecycle, mapView) {

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
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

    AndroidView( // mapview is not compose component so we are using the bridge of android and compose
        factory = { // Run one time and create the map

            // map is not ready immediately GPU + rendering engine take time
            mapView.getMapAsync { map ->
                // set the style of map.
                map.setStyle("https://api.maptiler.com/maps/streets/style.json?key=urVJndTUzEBx0xaseMA6") { style ->

                    val source = GeoJsonSource("route-source")
                    style.addSource(source)

                    val layer = LineLayer("route-layer", "route-source").apply {
                        setProperties(
                            PropertyFactory.lineColor("red"),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    }
                    style.addLayer(layer)

                    val blueSource = GeoJsonSource("blue-route-source")
                    style.addSource(blueSource)
                    val blueLayer = LineLayer("blue-route-layer", "blue-route-source").apply {
                        setProperties(
                            PropertyFactory.lineColor("blue"),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineDasharray(arrayOf(2f, 2f))
                        )
                    }
                    style.addLayer(blueLayer)
                }

                map.uiSettings.isLogoEnabled = false // remove logo of maplibre
                map.uiSettings.isAttributionEnabled = false // data source info copyright text
                onMapReady(map)
            }
            mapView
        },

        update = { view ->
            // Get the map
            view.getMapAsync { map ->
                // Get the style of the map and modify it using the red line
                map.getStyle { style ->
                    // Get the source for red line
                    val source = style.getSourceAs<GeoJsonSource>("route-source")
                    if (pathPoints.isNotEmpty()) { // show the stading place if we don't find destination plase
                        val points = pathPoints.map {
                            Point.fromLngLat(it.longitude, it.latitude)
                        }
                        val lineString = org.maplibre.geojson.LineString.fromLngLats(points)
                        source?.setGeoJson(lineString)
                    }

                    // 🔵 Blue route (OSRM route)
                    val blueSource =
                        style.getSourceAs<GeoJsonSource>("blue-route-source")

                    if (routePointsState is Resource.Success) {
                        val coordinates = routePointsState.data

                        if (!coordinates.isNullOrEmpty()) {

                            // 1️⃣ Draw the route
                            val routePoints = coordinates.map {
                                Point.fromLngLat(it[0], it[1]) // lon, lat
                            }

                            val lineString =
                                org.maplibre.geojson.LineString.fromLngLats(routePoints)
                            blueSource?.setGeoJson(lineString)

                            // 2️⃣ Move camera to show whole route (IMPORTANT)
                            val first = coordinates.first()
                            val last = coordinates.last()

                            val bounds =
                                org.maplibre.android.geometry.LatLngBounds.Builder()
                                    .include(LatLng(first[1], first[0])) // lat, lon
                                    .include(LatLng(last[1], last[0]))   // lat, lon
                                    .build()

                            map.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds, 100),
                                2000
                            )
                        }
                    }
                }
            }
        }

    )

}
