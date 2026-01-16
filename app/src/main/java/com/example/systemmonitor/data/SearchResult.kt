package com.example.systemmonitor.data

import com.google.gson.annotations.SerializedName

data class SearchResult(
    @SerializedName("display_name")
    val search : String,
    @SerializedName("lat")
    val lat : String,
    @SerializedName("lon")
    val lon : String
)