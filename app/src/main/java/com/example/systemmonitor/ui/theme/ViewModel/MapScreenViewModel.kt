package com.example.systemmonitor.ui.theme.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.systemmonitor.common.Resource
import com.example.systemmonitor.data.SearchResult
import com.example.systemmonitor.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class MapScreenViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel(){

    // user enter location
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // search state start from empty list then modity later itself.
    private val _searchState = MutableStateFlow<Resource<List<SearchResult>>>(Resource.Success(emptyList()))
    val searchState = _searchState.asStateFlow()

    // change empty
    fun onSearchQueryChange(newQuery : String){
        _searchQuery.value = newQuery
    }

    // deal with repo and pass to ui
    fun searchLocation(){
        val query = _searchQuery.value

        if (query.isEmpty()){
            _searchState.value = Resource.Error("Please enter the location")
            return
        }

        viewModelScope.launch {
            _searchState.value = Resource.Loading()

            try {
                val result = repository.searchLocation(query)
                if (result.data.isNullOrEmpty()){
                    _searchState.value = Resource.Error("No result found for $query" ?: "Unknow Error")
                }else{
                    _searchState.value = result
                }
            }catch (e : Exception){
                    _searchState.value = Resource.Error("${e.localizedMessage}" ?: "Somthing went wrong")
            }

        }

    }

}