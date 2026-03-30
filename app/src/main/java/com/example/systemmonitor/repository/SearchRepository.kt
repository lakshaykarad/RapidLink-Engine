package com.example.systemmonitor.repository

import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.data.interfaces.NominatimApi
import com.example.systemmonitor.data.model.SearchResult
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class SearchRepository @Inject constructor(
    val api: NominatimApi  // our api
) {
    // basic function with try and catch block
    suspend fun searchLocation(query: String) : Resource<List<SearchResult>>{
        return try {
            val response = api.search(query)
            if (response.isEmpty()){
                Resource.Error("No city found with this name")
            }else{
                Resource.Success(response)
            }
        } // SocketTimeoutException -> IOException -> Exception
        catch (e : SocketTimeoutException ){
            Resource.Error("Network is too slow. Please check your connection")
        } catch (e : IOException){
            Resource.Error("No internet connection ")
        } catch (e: CancellationException) {
            throw e
        } catch (e : Exception){
            Resource.Error( "Something went wrong")
        }
    }
}
