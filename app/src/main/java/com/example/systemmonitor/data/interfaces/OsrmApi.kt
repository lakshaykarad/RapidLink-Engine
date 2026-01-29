package com.example.systemmonitor.data.interfaces

import com.example.systemmonitor.data.model.OsrmResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface OsrmApi {
    // We pass the coordinates as a string: "75.78,26.91;75.80,26.92"
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        // using encoded for ignore ? : and others
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full", // it give full line smooth and proper way
        @Query("geometries") geometries: String = "geojson" // hold coordinates
    ): OsrmResponse
}