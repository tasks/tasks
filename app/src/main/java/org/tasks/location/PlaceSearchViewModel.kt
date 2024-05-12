package org.tasks.location

import android.os.Bundle
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tasks.Event
import org.tasks.data.entity.Place
import javax.inject.Inject

@HiltViewModel
class PlaceSearchViewModel @Inject constructor(
        private val search: PlaceSearch
): ViewModel() {
    private val searchResults = MutableLiveData<List<PlaceSearchResult>>()
    private val error = MutableLiveData<Event<String>>()
    private val selection = MutableLiveData<Place>()

    fun observe(
        owner: LifecycleOwner?,
        onResults: Observer<List<PlaceSearchResult>>?,
        onSelection: Observer<Place>?,
        onError: Observer<Event<String>>?) {
        searchResults.observe(owner!!, onResults!!)
        selection.observe(owner, onSelection!!)
        error.observe(owner, onError!!)
    }

    fun saveState(outState: Bundle) {
        search.saveState(outState)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        search.restoreState(savedInstanceState)
    }

    fun query(query: String?, bias: MapPosition?) = viewModelScope.launch {
        if (query.isNullOrBlank()) {
            searchResults.postValue(emptyList())
        } else {
            try {
                searchResults.value = search.search(query, bias)
            } catch (e: Exception) {
                e.message?.let { setError(it) }
            }
        }
    }

    fun fetch(result: PlaceSearchResult) = viewModelScope.launch {
        try {
            selection.value = search.fetch(result)
        } catch (e: Exception) {
            e.message?.let { setError(it) }
        }
    }

    private fun setError(message: String) {
        error.value = Event(message)
    }

    fun getAttributionRes(dark: Boolean) = search.getAttributionRes(dark)
}