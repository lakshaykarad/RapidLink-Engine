package com.example.systemmonitor.data.model

import com.google.gson.annotations.SerializedName

/**
 * search result is data class that show us result
 * name lat lan city osm_id class type place_rank importance addresstype boundingbox
 * But we are only use here search that display name, lat and lan to move camera // 17-01-2026
**/
data class SearchResult(
    @SerializedName("display_name")
    val search : String,
    @SerializedName("lat")
    val lat : String,
    @SerializedName("lon")
    val lon : String
)