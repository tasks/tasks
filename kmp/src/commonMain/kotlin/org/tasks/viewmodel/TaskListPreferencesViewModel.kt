package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tasks.preferences.TasksPreferences

/**
 * State and persistence for the desktop "Task list options" settings pane.
 *
 * Mirrors the native Android `TaskListPreferencesViewModel` (same rows,
 * same defaults, same key semantics), but stores values in the shared
 * [TasksPreferences] DataStore instead of Android SharedPreferences.
 *
 * Chip appearance is an int (0 = text and icon, 1 = text only,
 * 2 = icon only) matching the native `p_chip_appearance` values; the
 * display labels are resolved in the composable layer so this view model
 * stays free of string-resource resolution.
 *
 * The two int sliders share [updateSlider] and the five per-chip toggles
 * share [updateChipVisibility] (keyed by option enum, mirroring the native
 * view model's rows one-to-one); every other option has its own setter.
 */
class TaskListPreferencesViewModel(
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    /** The two int-valued slider rows. */
    enum class SliderOption {
        FONT_SIZE,
        ROW_SPACING,
    }

    /** The five per-chip visibility toggle rows. */
    enum class ChipOption {
        SUBTASKS,
        START_DATE,
        PLACES,
        LISTS,
        TAGS,
    }

    data class State(
        val loaded: Boolean = false,
        val fontSize: Int = DEFAULT_FONT_SIZE,
        val rowSpacing: Int = DEFAULT_ROW_SPACING,
        val showFullTitle: Boolean = false,
        val showDescription: Boolean = true,
        val showFullDescription: Boolean = false,
        val showLinks: Boolean = false,
        val perListSort: Boolean = false,
        val chipAppearance: Int = CHIP_APPEARANCE_TEXT_AND_ICON,
        val subtaskChips: Boolean = true,
        val startDateChips: Boolean = true,
        val placeChips: Boolean = true,
        val listChips: Boolean = true,
        val tagChips: Boolean = true,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val state = State(
                loaded = true,
                fontSize = tasksPreferences.get(TasksPreferences.taskListFontSize, DEFAULT_FONT_SIZE),
                rowSpacing = tasksPreferences.get(TasksPreferences.taskListRowSpacing, DEFAULT_ROW_SPACING),
                showFullTitle = tasksPreferences.get(TasksPreferences.taskListShowFullTitle, false),
                showDescription = tasksPreferences.get(TasksPreferences.taskListShowDescription, true),
                showFullDescription = tasksPreferences.get(TasksPreferences.taskListShowFullDescription, false),
                showLinks = tasksPreferences.get(TasksPreferences.taskListShowLinks, false),
                perListSort = tasksPreferences.get(TasksPreferences.taskListPerListSort, false),
                chipAppearance = tasksPreferences.get(TasksPreferences.taskListChipAppearance, CHIP_APPEARANCE_TEXT_AND_ICON),
                subtaskChips = tasksPreferences.get(TasksPreferences.taskListSubtaskChips, true),
                startDateChips = tasksPreferences.get(TasksPreferences.taskListStartDateChips, true),
                placeChips = tasksPreferences.get(TasksPreferences.taskListPlaceChips, true),
                listChips = tasksPreferences.get(TasksPreferences.taskListListChips, true),
                tagChips = tasksPreferences.get(TasksPreferences.taskListTagChips, true),
            )
            _state.value = state
        }
    }

    fun updateSlider(option: SliderOption, value: Int) {
        when (option) {
            SliderOption.FONT_SIZE ->
                set(TasksPreferences.taskListFontSize, value) { it.copy(fontSize = value) }
            SliderOption.ROW_SPACING ->
                set(TasksPreferences.taskListRowSpacing, value) { it.copy(rowSpacing = value) }
        }
    }

    fun updateChipVisibility(option: ChipOption, enabled: Boolean) {
        when (option) {
            ChipOption.SUBTASKS ->
                set(TasksPreferences.taskListSubtaskChips, enabled) { it.copy(subtaskChips = enabled) }
            ChipOption.START_DATE ->
                set(TasksPreferences.taskListStartDateChips, enabled) { it.copy(startDateChips = enabled) }
            ChipOption.PLACES ->
                set(TasksPreferences.taskListPlaceChips, enabled) { it.copy(placeChips = enabled) }
            ChipOption.LISTS ->
                set(TasksPreferences.taskListListChips, enabled) { it.copy(listChips = enabled) }
            ChipOption.TAGS ->
                set(TasksPreferences.taskListTagChips, enabled) { it.copy(tagChips = enabled) }
        }
    }

    fun updateShowFullTitle(enabled: Boolean) {
        set(TasksPreferences.taskListShowFullTitle, enabled) { it.copy(showFullTitle = enabled) }
    }

    fun updateShowDescription(enabled: Boolean) {
        set(TasksPreferences.taskListShowDescription, enabled) { it.copy(showDescription = enabled) }
        if (!enabled) {
            // Like the native view model: hiding descriptions also hides the full description.
            updateShowFullDescription(false)
        }
    }

    fun updateShowFullDescription(enabled: Boolean) {
        set(TasksPreferences.taskListShowFullDescription, enabled) { it.copy(showFullDescription = enabled) }
    }

    fun updateShowLinks(enabled: Boolean) {
        set(TasksPreferences.taskListShowLinks, enabled) { it.copy(showLinks = enabled) }
    }

    fun updatePerListSort(enabled: Boolean) {
        set(TasksPreferences.taskListPerListSort, enabled) { it.copy(perListSort = enabled) }
    }

    fun updateChipAppearance(value: Int) {
        set(TasksPreferences.taskListChipAppearance, value) { it.copy(chipAppearance = value) }
    }

    private fun <T> set(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T, transform: (State) -> State) {
        _state.update(transform)
        viewModelScope.launch { tasksPreferences.set(key, value) }
    }

    companion object {
        const val DEFAULT_FONT_SIZE = 16
        const val DEFAULT_ROW_SPACING = 16
        const val CHIP_APPEARANCE_TEXT_AND_ICON = 0
        const val CHIP_APPEARANCE_TEXT_ONLY = 1
        const val CHIP_APPEARANCE_ICON_ONLY = 2
    }
}
