package com.example.systemmonitor.data.model

import com.google.gson.annotations.SerializedName

// for batter understanding go and check one response of OSRM demo where we can see this data class in json form.
// Demo link -> https://router.project-osrm.org/route/v1/driving/75.8167,26.9124;74.6399,26.4499?overview=full&geometries=geojson
data class OsrmResponse(
    @SerializedName("routes")
    val routes: List<Route> // use to access whole data
)

data class Route(
    val geometry : Geometry // Shape of the line
)

data class Geometry(
    val coordinates : List<List<Double>>, // [[75.7, 26.9], [75.8, 27.0]]
    val type : String // LineString
)