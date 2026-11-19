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

data class TextSettings(
    val fontSize: Int = DEFAULT_TASK_LIST_FONT_SIZE,
    val rowSpacing: Int = DEFAULT_TASK_LIST_ROW_SPACING,
)

data class DisplaySettings(
    val showFullTitle: Boolean = false,
    val showDescription: Boolean = true,
    val showFullDescription: Boolean = false,
    val showLinks: Boolean = false,
)

data class ChipSettings(
    val subtaskChips: Boolean = true,
    val startDateChips: Boolean = true,
    val placeChips: Boolean = true,
    val listChips: Boolean = true,
    val tagChips: Boolean = true,
)

data class AppearanceSettings(
    val perListSort: Boolean = false,
    val chipAppearanceIndex: Int = 0,
)

data class TaskListOptionsState(
    val text: TextSettings = TextSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val chips: ChipSettings = ChipSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val showChipAppearanceDialog: Boolean = false,
    val chipEntries: List<String> = emptyList(),
    val chipValues: List<String> = emptyList(),
    val chipAppearanceSummary: String = "",
)

open class TaskListOptionsViewModel(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    var state by mutableStateOf(TaskListOptionsState())
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            state =
                state.copy(
                    text =
                        TextSettings(
                            fontSize = appPreferences.taskListFontSize(),
                            rowSpacing = appPreferences.taskListRowSpacing(),
                        ),
                    display =
                        DisplaySettings(
                            showFullTitle = appPreferences.taskListShowFullTitle(),
                            showDescription = appPreferences.taskListShowDescription(),
                            showFullDescription = appPreferences.taskListShowFullDescription(),
                            showLinks = appPreferences.taskListShowLinks(),
                        ),
                    chips =
                        ChipSettings(
                            subtaskChips = appPreferences.taskListSubtaskChips(),
                            startDateChips = appPreferences.taskListStartDateChips(),
                            placeChips = appPreferences.taskListPlaceChips(),
                            listChips = appPreferences.taskListListChips(),
                            tagChips = appPreferences.taskListTagChips(),
                        ),
                    appearance =
                        AppearanceSettings(
                            perListSort = appPreferences.taskListPerListSort(),
                            chipAppearanceIndex = appPreferences.taskListChipAppearance(),
                        ),
                )
            refreshChipAppearanceSummary()
        }
    }

    private fun refreshChipAppearanceSummary() {
        val index = state.appearance.chipAppearanceIndex.coerceAtLeast(0)
        val summary = state.chipEntries.getOrNull(index) ?: ""
        state = state.copy(chipAppearanceSummary = summary)
    }

    fun updateTextSettings(settings: TextSettings) {
        viewModelScope.launch {
            appPreferences.setTaskListFontSize(settings.fontSize)
            appPreferences.setTaskListRowSpacing(settings.rowSpacing)
            state = state.copy(text = settings)
        }
    }

    fun updateDisplaySettings(settings: DisplaySettings) {
        viewModelScope.launch {
            appPreferences.setTaskListFullTitle(settings.showFullTitle)
            appPreferences.setTaskListShowDescription(settings.showDescription)
            appPreferences.setTaskListShowFullDescription(settings.showFullDescription)
            appPreferences.setTaskListShowLinks(settings.showLinks)

            val updatedDisplay =
                if (!settings.showDescription) {
                    settings.copy(showFullDescription = false)
                } else {
                    settings
                }

            state = state.copy(display = updatedDisplay)
        }
    }

    fun updateChipSetting(
        index: Int,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            when (index) {
                0 -> appPreferences.setTaskListSubtaskChips(enabled)
                1 -> appPreferences.setTaskListStartDateChips(enabled)
                2 -> appPreferences.setTaskListPlaceChips(enabled)
                3 -> appPreferences.setTaskListListChips(enabled)
                4 -> appPreferences.setTaskListTagChips(enabled)
            }
            val currentChips = state.chips
            val updatedChips =
                when (index) {
                    0 -> currentChips.copy(subtaskChips = enabled)
                    1 -> currentChips.copy(startDateChips = enabled)
                    2 -> currentChips.copy(placeChips = enabled)
                    3 -> currentChips.copy(listChips = enabled)
                    4 -> currentChips.copy(tagChips = enabled)
                    else -> currentChips
                }
            state = state.copy(chips = updatedChips)
        }
    }

    fun updatePerListSort(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setTaskListPerListSort(enabled)
            state = state.copy(appearance = state.appearance.copy(perListSort = enabled))
        }
    }

    fun openChipAppearanceDialog() {
        viewModelScope.launch {
            val entries = loadChipEntries()
            val values = listOf("0", "1", "2")
            val index = state.appearance.chipAppearanceIndex.coerceAtLeast(0)
            val summary = entries.getOrNull(index) ?: ""
            state =
                state.copy(
                    chipEntries = entries,
                    chipValues = values,
                    chipAppearanceSummary = summary,
                    showChipAppearanceDialog = true,
                )
        }
    }

    fun dismissChipAppearanceDialog() {
        state = state.copy(showChipAppearanceDialog = false)
    }

    fun setChipAppearance(value: Int) {
        viewModelScope.launch {
            appPreferences.setTaskListChipAppearance(value)
            state =
                state.copy(
                    appearance = state.appearance.copy(chipAppearanceIndex = value),
                )
            refreshChipAppearanceSummary()
        }
    }

    private suspend fun loadChipEntries(): List<String> =
        listOf(
            getString(Res.string.chip_appearance_text_and_icon),
            getString(Res.string.chip_appearance_text_only),
            getString(Res.string.chip_appearance_icon_only),
        )
}
