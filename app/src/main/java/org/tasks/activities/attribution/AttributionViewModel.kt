package org.tasks.activities.attribution

import android.content.Context
import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class AttributionViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    val attributions = MutableLiveData<Map<String, Map<String, List<LibraryAttribution>>>>()

    init {
        viewModelScope.launch {
            val licenses = withContext(Dispatchers.IO) {
                context.assets.open("licenses.json")
            }
            val reader = InputStreamReader(licenses, StandardCharsets.UTF_8)
            val list = GsonBuilder().create().fromJson(reader, AttributionList::class.java)
            attributions.value = list.libraries!!
                .groupBy { it.license!! }.toSortedMap()
                .mapValues { (_, libraries) ->
                    libraries.groupBy { it.copyrightHolder!! }.toSortedMap()
                }
        }
    }

    internal class AttributionList {
        var libraries: List<LibraryAttribution>? = null
    }

    class LibraryAttribution {
        var copyrightHolder: String? = null

        @get:Keep
        var license: String? = null
        var libraryName: String? = null
    }
}