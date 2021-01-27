package org.tasks.location

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.todoroo.andlib.utility.AndroidUtilities
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.Event
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.Place
import javax.inject.Inject

@HiltViewModel
class PlaceSearchViewModel @Inject constructor(
        private val searchProvider: PlaceSearchProvider
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

    fun saveState(outState: Bundle?) {
        searchProvider.saveState(outState)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        searchProvider.restoreState(savedInstanceState)
    }

    fun query(query: String?, bias: MapPosition?) {
        AndroidUtilities.assertMainThread()
        if (isNullOrEmpty(query)) {
            searchResults.postValue(emptyList())
        } else {
            searchProvider.search(query, bias, { value: List<PlaceSearchResult> -> searchResults.setValue(value) }) { message: String -> setError(message) }
        }
    }

    fun fetch(result: PlaceSearchResult?) {
        searchProvider.fetch(result, { value: Place -> selection.setValue(value) }) { message: String -> setError(message) }
    }

    private fun setError(message: String) {
        error.value = Event(message)
    }

    fun getAttributionRes(dark: Boolean) = searchProvider.getAttributionRes(dark)
}