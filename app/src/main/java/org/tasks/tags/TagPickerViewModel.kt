package org.tasks.tags

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import javax.inject.Inject

@HiltViewModel
class TagPickerViewModel @Inject constructor(
        private val tagDataDao: TagDataDao
) : ViewModel() {

    private val tags = MutableLiveData<List<TagData>>()
    private val selected: MutableSet<TagData> = HashSet()
    private val partiallySelected: MutableSet<TagData> = HashSet()

    val searchText: State<String>
        get() = _searchText
    private val _searchText = mutableStateOf("")

    val tagToCreate: State<String>
        get() = _tagToCreate
    private val _tagToCreate = mutableStateOf("")

    fun observe(owner: LifecycleOwner, observer: (List<TagData>) -> Unit) =
            tags.observe(owner, observer)

    /* The property to access selected tags list from the @Composable activity */
    val tagsList: MutableLiveData<List<TagData>>
        get() = tags

    fun setSelected(selected: List<TagData>, partiallySelected: List<TagData>?) {
        this.selected.addAll(selected)
        if (partiallySelected != null) {
            this.partiallySelected.addAll(partiallySelected)
        }
    }

    fun getSelected() = ArrayList(selected)

    fun getPartiallySelected() = ArrayList(partiallySelected)

    fun search(newText: String) {
        if (newText == "" || !newText.equals(_searchText.value, ignoreCase = true)) {
            viewModelScope.launch {
                val results = tagDataDao.searchTags(newText)
                onUpdate(newText, results.toMutableList())
            }
        }
        _searchText.value = newText
    }

    private fun onUpdate(newText: String, results: MutableList<TagData>) {
        val sorted = results
            .sortedWith { l, r ->
                val lSelected = selected.contains(l) || partiallySelected.contains(r)
                val rSelected = selected.contains(r) || partiallySelected.contains(r)
                if (lSelected && !rSelected) {
                    -1
                } else if (rSelected) {
                    1
                } else {
                    0
                }
            }
            .toMutableList()
        if ( newText != "" && !results.any {newText.equals(it.name, ignoreCase = true) } )
            _tagToCreate.value = newText
        else
            _tagToCreate.value = ""
        tags.value = sorted
    }

    fun getState(tagData: TagData): ToggleableState {
        if (partiallySelected.contains(tagData)) {
            return ToggleableState.Indeterminate
        }
        return if (selected.contains(tagData)) ToggleableState.On else ToggleableState.Off
    }

    suspend fun toggle(tagData: TagData, checked: Boolean): ToggleableState {
        var tagData = tagData
        if (tagData.id == null) {
            tagData = TagData(tagData.name)
            tagDataDao.createNew(tagData)
        }
        partiallySelected.remove(tagData)
        return if (checked) {
            selected.add(tagData)
            ToggleableState.On
        } else {
            selected.remove(tagData)
            ToggleableState.Off
        }
    }

    suspend fun createNew(name: String) {
        val tagData = TagData(name)
        tagDataDao.createNew(tagData)
        selected.add(tagData)
        search("")
    }
}