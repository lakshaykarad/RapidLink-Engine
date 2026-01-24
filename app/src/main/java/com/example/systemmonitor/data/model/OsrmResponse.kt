package com.example.systemmonitor.data.model

// for batter understanding go and check one response of OSRM demo where we can see this data class in json form.

data class OsrmResponse(
    val route: List<Route> // use to access whole data
)

data class Route(
    val geometry : Geometry // Shape of the line
)

data class Geometry(
    val coordinates : List<List<Double>>, // [[75.7, 26.9], [75.8, 27.0]]
    val type : String // LineString
)