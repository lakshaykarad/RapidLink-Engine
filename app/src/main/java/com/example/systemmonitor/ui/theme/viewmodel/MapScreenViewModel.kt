package com.example.systemmonitor.ui.theme.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.data.interfaces.OsrmApi
import com.example.systemmonitor.data.local.LocationDao
import com.example.systemmonitor.data.local.LocationEntity
import com.example.systemmonitor.data.model.SearchResult
import com.example.systemmonitor.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class)
class MapScreenViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val locationDao: LocationDao,
    private val osrmApi: OsrmApi,
    private val searchRepository: SearchRepository
) : ViewModel(){

    // user enter location
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // search state start from empty list then modify later itself.
    private val _searchState =
        MutableStateFlow<Resource<List<SearchResult>>>(Resource.Success(emptyList()))
    val searchState = _searchState.asStateFlow()

    // draw the line on map
    private val _routePoints =
        MutableStateFlow<Resource<List<List<Double>>>>(Resource.Success(emptyList()))
    val routePoints = _routePoints.asStateFlow()


    // this block of code use to set the limitation of nominatim 1 per second request
    // modify the query here
    init {
        viewModelScope.launch {
            _searchQuery.debounce(500)
                .distinctUntilChanged()  // don't request for "A" -> to again -> "A"
                .collectLatest { query-> // send the query
                    if (query.isNotEmpty()){
                        searchLocation(query)
                    }
                }
        }
    }

    // change empty
    fun onSearchQueryChange(newQuery : String){
        _searchQuery.value = newQuery
    }

    fun searchLocation(query: String) {
         viewModelScope.launch {
             performSearchLogic(query)
         }
    }

    private suspend fun performSearchLogic(query: String) {
        _searchState.value = Resource.Loading()

        val result = repository.searchLocation(query)

        _searchState.value = result.data?.let { data ->
            if (data.isNotEmpty()) {
                result // Success
            } else {
                Resource.Error("No result found for $query")
            }
        } ?: run {
            result // Error
        }
    }


    fun getRouteTo(destinationLat: Double, destinationLon: Double){
        viewModelScope.launch {
            _routePoints.value = Resource.Loading()

            // Get CurrentLocation
            val currentList = locationDao.getAllLocations().first()

            if (currentList.isNullOrEmpty()){
                _routePoints.value = Resource.Error("Waiting for GPS... Walk a few steps!")
                return@launch
            }

            val lastLocation = currentList.last()
            val startLat = lastLocation.latitude
            val startLon = lastLocation.longitude

            // we need to check json data file where we see coordinates and also check the data class for batter understanding
            val coordinates = "$startLon,$startLat;$destinationLon,$destinationLat"

            try {
                val response = osrmApi.getRoute(coordinates)
                if (response.routes.isNotEmpty()){
                    val shape = response.routes[0].geometry.coordinates.toMutableList() // shape -> The exact road path made of many small points
                    val myLocationPoint = listOf(startLon,startLat)
                    shape.add(0,myLocationPoint)
                    _routePoints.value = Resource.Success(shape)
                    Log.d("OSRM", "Route found $shape")
                }else{
                    _routePoints.value = Resource.Error("No route found")
                }
            }catch (e : Exception){
                _routePoints.value = Resource.Error("${e.message} : Something went wrong")
                Log.e("OSRM", "Error: ${e.message}")
            }

        }
    }

    // Update the list automatically
    val pathPoints = locationDao.getAllLocations().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Clear Path
    fun clearPath(){
        viewModelScope.launch {
            locationDao.clearAllLocations()
        }
    }

    fun clearRoute(){
       viewModelScope.launch {
           _searchState.value = Resource.Success(emptyList())
           _searchQuery.value = "" // clear with clean text.
           _routePoints.value = Resource.Success(emptyList())
       }
    }

    // CurrentSpeed
    val currentSpeed = locationDao.getAllLocations().map { list->
        if (list.isNotEmpty()){
            calculateCurrentSpeed(list)
        }else{
            0f
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),0f )
    // Distance
    val totalDistance = locationDao.getAllLocations().map { list->
        calculateTotalDistance(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Total Steps
    val stepCount = totalDistance.map { distance ->
        (distance * 1.3).toInt() // Rough math: 1.3 steps per meter
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    private fun calculateTotalDistance(points: List<LocationEntity>): Float {
        var distance = 0f
        if (points.size < 2) return 0f

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i+1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude,
                results
            )
            distance += results[0]
        }
        return distance // Returns meters
    }

    private fun calculateCurrentSpeed(points: List<LocationEntity>): Float {
        if (points.size < 2) return 0f

        val last = points.last()
        val secondLast = points[points.size - 2]

        // Distance in meters
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            secondLast.latitude, secondLast.longitude,
            last.latitude, last.longitude,
            results
        )
        val distanceMeters = results[0]

        // Time in seconds
        val timeDiff = (last.timestamp - secondLast.timestamp) / 1000f

        if (timeDiff <= 0) return 0f

        val speedMps = distanceMeters / timeDiff
        return speedMps * 3.6f // Convert to km/h
    }

}