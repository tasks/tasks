package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DEFAULT_TASK_LIST_FONT_SIZE
import org.tasks.preferences.DEFAULT_TASK_LIST_ROW_SPACING
import org.tasks.resources.getString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.chip_appearance
import tasks.kmp.generated.resources.chip_appearance_icon_only
import tasks.kmp.generated.resources.chip_appearance_text_and_icon
import tasks.kmp.generated.resources.chip_appearance_text_only

open class TaskListOptionsViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    var fontSize by mutableStateOf(DEFAULT_TASK_LIST_FONT_SIZE)
        private set
    var rowSpacing by mutableStateOf(DEFAULT_TASK_LIST_ROW_SPACING)
        private set
    var showFullTitle by mutableStateOf(false)
        private set
    var showDescription by mutableStateOf(true)
        private set
    var showFullDescription by mutableStateOf(false)
        private set
    var showLinks by mutableStateOf(false)
        private set
    var perListSort by mutableStateOf(false)
        private set
    var chipAppearanceIndex by mutableStateOf(0)
        private set
    var subtaskChips by mutableStateOf(true)
        private set
    var startDateChips by mutableStateOf(true)
        private set
    var placeChips by mutableStateOf(true)
        private set
    var listChips by mutableStateOf(true)
        private set
    var tagChips by mutableStateOf(true)
        private set
    var showChipAppearanceDialog by mutableStateOf(false)
        private set

    var chipEntries by mutableStateOf<List<String>>(emptyList())
        private set
    var chipValues by mutableStateOf<List<String>>(emptyList())
        private set
    var chipAppearanceSummary by mutableStateOf("")
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            fontSize = appPreferences.taskListFontSize()
            rowSpacing = appPreferences.taskListRowSpacing()
            showFullTitle = appPreferences.taskListShowFullTitle()
            showDescription = appPreferences.taskListShowDescription()
            showFullDescription = appPreferences.taskListShowFullDescription()
            showLinks = appPreferences.taskListShowLinks()
            perListSort = appPreferences.taskListPerListSort()
            subtaskChips = appPreferences.taskListSubtaskChips()
            startDateChips = appPreferences.taskListStartDateChips()
            placeChips = appPreferences.taskListPlaceChips()
            listChips = appPreferences.taskListListChips()
            tagChips = appPreferences.taskListTagChips()
            chipAppearanceIndex = appPreferences.taskListChipAppearance()
            refreshChipAppearance()
        }
    }

    private fun refreshChipAppearance() {
        val index = chipAppearanceIndex.coerceAtLeast(0)
        chipAppearanceSummary = chipEntries.getOrNull(index) ?: ""
    }

    fun updateFontSize(value: Int) {
        viewModelScope.launch {
            appPreferences.setTaskListFontSize(value)
            fontSize = value
        }
    }

    fun updateRowSpacing(value: Int) {
        viewModelScope.launch {
            appPreferences.setTaskListRowSpacing(value)
            rowSpacing = value
        }
    }

    fun updateShowFullTitle(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListFullTitle(enabled)
            showFullTitle = enabled
        }
    }

    fun updateShowDescription(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListShowDescription(enabled)
            showDescription = enabled
            if (!enabled) {
                appPreferences.setTaskListShowFullDescription(false)
                showFullDescription = false
            }
        }
    }

    fun updateShowFullDescription(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListShowFullDescription(enabled)
            showFullDescription = enabled
        }
    }

    fun updateShowLinks(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListShowLinks(enabled)
            showLinks = enabled
        }
    }

    fun updatePerListSort(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListPerListSort(enabled)
            perListSort = enabled
        }
    }

    fun updateSubtaskChips(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListSubtaskChips(enabled)
            subtaskChips = enabled
        }
    }

    fun updateStartDateChips(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListStartDateChips(enabled)
            startDateChips = enabled
        }
    }

    fun updatePlaceChips(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListPlaceChips(enabled)
            placeChips = enabled
        }
    }

    fun updateListChips(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListListChips(enabled)
            listChips = enabled
        }
    }

    fun updateTagChips(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListTagChips(enabled)
            tagChips = enabled
        }
    }

    fun openChipAppearanceDialog() {
        viewModelScope.launch {
            chipEntries = loadChipEntries()
            chipValues = listOf("0", "1", "2")
            chipAppearanceSummary = chipEntries.getOrNull(chipAppearanceIndex.coerceAtLeast(0)) ?: ""
            showChipAppearanceDialog = true
        }
    }

    fun dismissChipAppearanceDialog() {
        showChipAppearanceDialog = false
    }

    fun setChipAppearance(value: Int) {
        viewModelScope.launch {
            appPreferences.setTaskListChipAppearance(value)
            chipAppearanceIndex = value
            refreshChipAppearance()
        }
    }

    private suspend fun loadChipEntries(): List<String> =
        listOf(
            getString(Res.string.chip_appearance_text_and_icon),
            getString(Res.string.chip_appearance_text_only),
            getString(Res.string.chip_appearance_icon_only),
        )
}
