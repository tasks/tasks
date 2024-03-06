package org.tasks.tags

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.tags.CheckBoxTriStates.State
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
        if (!isNullOrEmpty(text) && !results.any { text.equals(it.name, ignoreCase = true) }) {
            sorted.add(0, TagData(text))
        }
        tags.value = sorted
    }

    fun getState(tagData: TagData): State {
        if (partiallySelected.contains(tagData)) {
            return State.PARTIALLY_CHECKED
        }
        return if (selected.contains(tagData)) State.CHECKED else State.UNCHECKED
    }

    suspend fun toggle(tagData: TagData, checked: Boolean): State {
        var tagData = tagData
        if (tagData.id == null) {
            tagData = TagData(tagData.name)
            tagDataDao.createNew(tagData)
        }
        partiallySelected.remove(tagData)
        return if (checked) {
            selected.add(tagData)
            State.CHECKED
        } else {
            selected.remove(tagData)
            State.UNCHECKED
        }
    }

    suspend fun createNew(name: String) {
        val tagData = TagData(name)
        tagDataDao.createNew(tagData)
        selected.add(tagData)
        search("")
    }
}