package org.tasks.tags

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.data.searchTags

open class TagPickerViewModel(
    private val tagDataDao: TagDataDao,
) : ViewModel() {

    private val _tags = mutableStateOf<List<TagData>>(emptyList())
    val tags: State<List<TagData>>
        get() = _tags

    private val selected: MutableSet<TagData> = HashSet()
    private val partiallySelected: MutableSet<TagData> = HashSet()

    val searchText: State<String>
        get() = _searchText
    private val _searchText = mutableStateOf("")

    val tagToCreate: State<String>
        get() = _tagToCreate
    private val _tagToCreate = mutableStateOf("")

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
        val sorted = results.sortedByDescending {
            selected.contains(it) || partiallySelected.contains(it)
        }
        if (newText != "" && !results.any { newText.equals(it.name, ignoreCase = true) })
            _tagToCreate.value = newText
        else
            _tagToCreate.value = ""
        _tags.value = sorted
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
            tagData = tagDataDao.getOrCreateTag(tagData.name.orEmpty())
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
        selected.add(tagDataDao.getOrCreateTag(name))
        search("")
    }
}
