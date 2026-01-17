package com.example.systemmonitor.repository

import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.data.NominatimApi
import com.example.systemmonitor.data.SearchResult
import javax.inject.Inject

class SearchRepository @Inject constructor(
    val api: NominatimApi  // our api
) {
    // basic funcation with try and catch block
    suspend fun searchLocation(query: String) : Resource<List<SearchResult>>{
        return try {
            val response = api.search(query)
            if (response.isEmpty()){
                Resource.Error("No city found with that name")
            }else{
                Resource.Success(response)
            }
        } catch (e : Exception){
            Resource.Error("Network Error ${e.localizedMessage}" ?: "Unknow Error")
        }
    }
}