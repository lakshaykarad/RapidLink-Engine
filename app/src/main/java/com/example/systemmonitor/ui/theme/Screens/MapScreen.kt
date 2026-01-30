package com.example.systemmonitor.ui.theme.Screens

import android.R
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsProperties.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.data.local.LocationEntity
import com.example.systemmonitor.ui.theme.viewmodel.MapScreenViewModel
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
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

    val distanceMeters by viewModel.totalDistance.collectAsStateWithLifecycle() // Distance
    val speedKmph by viewModel.currentSpeed.collectAsStateWithLifecycle()  // Speed
    val step by viewModel.stepCount.collectAsStateWithLifecycle()

    LaunchedEffect(searchState) {
        if (searchState is Resource.Success) {
            val result = searchState.data
            if (!result.isNullOrEmpty()) {
                val city = result[0] // first city that search
                // Get the location of the city
                val lat = city.lat?.toDoubleOrNull()
                val lon = city.lon?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val location = LatLng(lat, lon)

                    mapController?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(location, 14.00)
                    )
                    viewModel.getRouteTo(lat, lon)
                } else {
                    Log.e("MapScreen", "Invalid lat/lon: lat=${city.lat}, lon=${city.lon}")
                    return@LaunchedEffect
                }

            }
        }

    }

    LaunchedEffect(routePointsState) {
        if (routePointsState is Resource.Success) {
            val coords = routePointsState.data
            if (!coords.isNullOrEmpty()) {
                val first = coords.first()
                val last = coords.last()
                val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                    .include(LatLng(first[1], first[0]))
                    .include(LatLng(last[1], last[0]))
                    .build()

                mapController?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 2000)
            }
        }

    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column { // Use Column to stack buttons
                // BUTTON 1: WHERE AM I? (New Feature)
                FloatingActionButton(
                    onClick = {
                        if (pathPoint.isNotEmpty()) {
                            val last = pathPoint.last()
                            val userLocation = LatLng(last.latitude, last.longitude)
                            mapController?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(userLocation, 16.0),
                                1500 // 1.5 seconds speed
                            )
                        } else {

                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "My Location")
                }

                FloatingActionButton(
                    onClick = { viewModel.clearPath() },
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Path")
                }
                FloatingActionButton(
                    onClick = { viewModel.clearRoute() },
                    modifier = Modifier.padding(bottom = 80.dp),
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Path")
                }
            }
        }
    ){ paddingValues ->

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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search City ") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp), // Modern rounded corners
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearRoute() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },

                keyboardActions = KeyboardActions(
                    onSearch = { /* Optional: trigger search logic on enter */ }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 10.dp), // Lift it up so FAB don't cover it
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.1f".format(speedKmph),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = Color.LightGray
                    )

                    // DISTANCE METER
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Convert meters to km if > 1000
                        val distanceText = if (distanceMeters > 1000) {
                            "%.2f km".format(distanceMeters / 1000)
                        } else {
                            "%.0f m".format(distanceMeters)
                        }

                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(40.dp)
                            .width(1.dp),
                        color = Color.LightGray
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$step",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight =  FontWeight.Bold,
                            color = Color(0xFF4CAF50) // Green color for fitness
                        )
                        Text("Steps", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            // Loading State
            if (searchState is Resource.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (searchState is Resource.Error){
                Text(
                    text = searchState.message ?: "Unknown error",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
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
        MapLibre.getInstance(context) // Map instance
    }

    val mapView = remember {
        MapView(context)
    }


    // Observering the lifecyle for mapview
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(Bundle()) // create the map with lifecycle observation
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

        lifecycle.addObserver(lifecycleObserver) // added the lifecycle observer
        onDispose { lifecycle.removeObserver(lifecycleObserver) }

    }

    AndroidView( // mapview is not compose component so we are using the bridge of android and compose
        factory = { // Run one time and create the map

            // map is not ready immediately GPU + rendering engine take time
            mapView.getMapAsync { map ->
                // set the style of map.
                map.setStyle("https://api.maptiler.com/maps/streets/style.json?key=urVJndTUzEBx0xaseMA6") { style ->

                    // Red line source for tracking
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

                    // blue line for navigation
                    val blueSource = GeoJsonSource("blue-route-source")
                    style.addSource(blueSource)
                    val blueLayer = LineLayer("blue-route-layer", "blue-route-source").apply {
                        setProperties(
                            PropertyFactory.lineColor("#4285F4"),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        )
                    }
                    style.addLayer(blueLayer)

                    // location layer for where we are standing
                    val locationSource = GeoJsonSource("location-source")
                    style.addSource(locationSource)

                    val locationLayer = CircleLayer("location-layer", "location-source").apply {
                       setProperties(
                           PropertyFactory.circleColor("#2196F3"), // blue dot
                           PropertyFactory.circleRadius(8f),
                           PropertyFactory.circleStrokeColor("white"),
                           PropertyFactory.circleStrokeWidth(3f)
                       )
                    }
                    style.addLayer(locationLayer)
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
                    if (!style.isFullyLoaded) return@getStyle
                    if (pathPoints.isNotEmpty()) { // show the standing place if we don't find destination plase
                        val source = style.getSourceAs<GeoJsonSource>("route-source")
                        val points = pathPoints.map {
                            Point.fromLngLat(it.longitude, it.latitude)
                        }

                        val lineString = org.maplibre.geojson.LineString.fromLngLats(points)
                        source?.setGeoJson(lineString)

                        val lastPoint = pathPoints.last()
                        val locationSource = style.getSourceAs<GeoJsonSource>("location-source")
                        val currentPoint = Point.fromLngLat(lastPoint.longitude,lastPoint.latitude)

                        locationSource?.setGeoJson(currentPoint)

                    }


                    val blueSource = style.getSourceAs<GeoJsonSource>("blue-route-source")

                    if (routePointsState is Resource.Success) {
                        val coordinates = routePointsState.data

                        if (!coordinates.isNullOrEmpty()) {
                            val routePoints = coordinates.map { Point.fromLngLat(it[0], it[1]) }
                            blueSource?.setGeoJson(org.maplibre.geojson.LineString.fromLngLats(routePoints))
                        } else {
                            blueSource?.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(arrayOf()))
                        }
                    }

                }
            }
        }

    )

}

