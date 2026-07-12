package org.tasks.tags

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.data.searchTags
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

open class TagPickerViewModel(
    private val tagDataDao: TagDataDao,
    private val syncAdapters: SyncAdapters,
) : ViewModel() {

    private val _tags = mutableStateOf<List<TagData>>(emptyList())
    val tags: State<List<TagData>>
        get() = _tags

    private val selected = LinkedHashMap<String, TagData>()
    private val partiallySelected = LinkedHashMap<String, TagData>()

    val searchText: State<String>
        get() = _searchText
    private val _searchText = mutableStateOf("")

    val tagToCreate: State<String>
        get() = _tagToCreate
    private val _tagToCreate = mutableStateOf("")

    fun setSelected(selected: List<TagData>, partiallySelected: List<TagData>?) {
        selected.forEach { tag -> tag.remoteId?.let { this.selected[it] = tag } }
        partiallySelected?.forEach { tag -> tag.remoteId?.let { this.partiallySelected[it] = tag } }
    }

    fun getSelected() = ArrayList(selected.values)

    fun getPartiallySelected() = ArrayList(partiallySelected.values)

    private fun isSelected(tagData: TagData) = tagData.remoteId?.let { selected.containsKey(it) } == true

    private fun isPartiallySelected(tagData: TagData) =
        tagData.remoteId?.let { partiallySelected.containsKey(it) } == true

    private var searchJob: Job? = null

    fun search(newText: String) {
        if (newText == "" || !newText.equals(_searchText.value, ignoreCase = true)) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                val results = tagDataDao.searchTags(newText)
                ensureActive()
                onUpdate(newText, results.toMutableList())
            }
        }
        _searchText.value = newText
    }

    private fun onUpdate(newText: String, results: MutableList<TagData>) {
        val sorted = results.sortedByDescending {
            isSelected(it) || isPartiallySelected(it)
        }
        if (newText != "" && !results.any { newText.equals(it.name, ignoreCase = true) })
            _tagToCreate.value = newText
        else
            _tagToCreate.value = ""
        _tags.value = sorted
    }

    fun getState(tagData: TagData): ToggleableState {
        if (isPartiallySelected(tagData)) {
            return ToggleableState.Indeterminate
        }
        return if (isSelected(tagData)) ToggleableState.On else ToggleableState.Off
    }

    private suspend fun getOrCreateDirty(name: String): TagData =
        tagDataDao.createDirty(TagData(name = name))?.also {
            syncAdapters.sync(SyncSource.METADATA_CHANGE)
        } ?: tagDataDao.getOrCreateTag(name)

    suspend fun toggle(tagData: TagData, checked: Boolean): ToggleableState {
        val tag = if (tagData.id == null) getOrCreateDirty(tagData.name.orEmpty()) else tagData
        val key = tag.remoteId ?: return ToggleableState.Off
        partiallySelected.remove(key)
        return if (checked) {
            selected[key] = tag
            ToggleableState.On
        } else {
            selected.remove(key)
            ToggleableState.Off
        }
    }

    suspend fun createNew(name: String) {
        val tag = getOrCreateDirty(name)
        tag.remoteId?.let { selected[it] = tag }
        search("")
    }
}
