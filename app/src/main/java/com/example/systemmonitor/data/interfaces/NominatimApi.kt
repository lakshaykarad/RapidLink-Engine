package com.example.systemmonitor.data.interfaces

import com.example.systemmonitor.data.model.SearchResult
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    // base url
    @GET("search")
    suspend fun search(
        @Query("q") query: String, // what user search
        @Query("format") format: String = "json", // i want data in json form only
        @Query("limit") limit: Int = 5, // i want only one result or city
    ) : List<SearchResult> // retur me list of result what you get.

}