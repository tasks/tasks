package org.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.tasks.compose.edit.EditScreenSection
import org.tasks.compose.edit.EditScreenSectionItem
import org.tasks.compose.edit.EditScreenSectionPref
import org.tasks.compose.edit.EditScreenSections
import org.tasks.preferences.TasksPreferences

/**
 * State and persistence for the desktop "Customize edit screen" screen:
 * reorder and hide/show the blocks on the task edit screen.
 *
 * The stored value is a JSON array of [EditScreenSectionPref] in the user's
 * display order, read back through [EditScreenSections.merge] so sections
 * added in a later build appear (enabled, appended) without migration and
 * sections that were removed are dropped. An absent value means "canonical
 * order, everything shown", so untouched installs never write the key.
 */
class EditScreenCustomizationViewModel(
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    data class State(
        val loaded: Boolean = false,
        val sections: ImmutableList<EditScreenSectionItem> = persistentListOf(),
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /** The sections the edit screen should render, in display order. */
    val visibleSections: StateFlow<List<EditScreenSection>> =
        _state
            .map { it.sections.filter { item -> item.enabled }.map { item -> item.section } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, EditScreenSections.CANONICAL)

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val stored = tasksPreferences
                .get(TasksPreferences.editScreenSections, "")
                .takeIf { it.isNotEmpty() }
                ?.let { raw -> runCatching { json.decodeFromString(prefListSerializer, raw) }.getOrNull() }
                .orEmpty()
            _state.value = State(
                loaded = true,
                sections = EditScreenSections.merge(stored).toPersistentList(),
            )
        }
    }

    /** Live move during a drag. [fromId]/[toId] are [EditScreenSectionItem.id]s. */
    fun onMove(fromId: String, toId: String) {
        var changed = false
        _state.update { s ->
            val items = s.sections.toMutableList()
            val from = items.indexOfFirst { it.id == fromId }
            val to = items.indexOfFirst { it.id == toId }
            if (from < 0 || to < 0 || from == to) {
                changed = false
                s
            } else {
                items.add(to, items.removeAt(from))
                changed = true
                s.copy(sections = items.toPersistentList())
            }
        }
        if (changed) {
            persist()
        }
    }

    /** Show/hide a section. Hidden rows keep their position. */
    fun setEnabled(id: String, enabled: Boolean) {
        _state.update { s ->
            s.copy(
                sections = s.sections.map { item ->
                    if (item.id == id) item.copy(enabled = enabled) else item
                }.toPersistentList(),
            )
        }
        persist()
    }

    /** Restore the canonical order with every section enabled. */
    fun reset() {
        _state.update {
            it.copy(
                sections = EditScreenSections.CANONICAL
                    .map { section -> EditScreenSectionItem(section, true) }
                    .toPersistentList(),
            )
        }
        persist()
    }

    /** True when the stored arrangement differs from the canonical default. */
    fun isCustomized(): Boolean {
        val current = _state.value.sections
        return current.any { !it.enabled } ||
            current.map { it.section } != EditScreenSections.CANONICAL
    }

    private fun persist() {
        val json = json.encodeToString(
            prefListSerializer,
            _state.value.sections.map { EditScreenSectionPref(it.id, it.enabled) },
        )
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.editScreenSections, json)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val prefListSerializer = ListSerializer(EditScreenSectionPref.serializer())
    }
}
