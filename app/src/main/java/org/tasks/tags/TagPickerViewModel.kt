package org.tasks.tags

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
    var text: String? = null
        private set

    private val _pattern = mutableStateOf("")
    val pattern: State<String>
        get() = _pattern

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
        if (!newText.equals(text, ignoreCase = true)) {
            viewModelScope.launch {
                val results = tagDataDao.searchTags(newText)
                onUpdate(results.toMutableList())
            }
        }
        text = newText
        _pattern.value = newText
    }

    private fun onUpdate(results: MutableList<TagData>) {
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
        if (pattern.value != "" && !results.any { pattern.value.equals(it.name, ignoreCase = true) }) {
            sorted.add(0, TagData(pattern.value))
        }
        tags.value = sorted
    }

    fun getState(tagData: TagData): CheckBoxTriStates.State {
        if (partiallySelected.contains(tagData)) {
            return CheckBoxTriStates.State.PARTIALLY_CHECKED
        }
        return if (selected.contains(tagData)) CheckBoxTriStates.State.CHECKED else CheckBoxTriStates.State.UNCHECKED
    }

    suspend fun toggle(tagData: TagData, checked: Boolean): CheckBoxTriStates.State {
        var tagData = tagData
        if (tagData.id == null) {
            tagData = TagData(tagData.name)
            tagDataDao.createNew(tagData)
        }
        partiallySelected.remove(tagData)
        return if (checked) {
            selected.add(tagData)
            CheckBoxTriStates.State.CHECKED
        } else {
            selected.remove(tagData)
            CheckBoxTriStates.State.UNCHECKED
        }
    }

    suspend fun createNew(name: String) {
        val tagData = TagData(name)
        tagDataDao.createNew(tagData)
        selected.add(tagData)
        search("")
    }
}