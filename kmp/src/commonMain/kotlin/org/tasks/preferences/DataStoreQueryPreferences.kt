package org.tasks.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.todoroo.astrid.core.SortHelper
import kotlinx.coroutines.runBlocking

class DataStoreQueryPreferences(
    private val tasksPreferences: TasksPreferences,
) : QueryPreferences {

    override var sortMode: Int by pref(Keys.sortMode, SortHelper.SORT_DUE)
    override var groupMode: Int by pref(Keys.groupMode, SortHelper.SORT_DUE)
    override var completedMode: Int by pref(Keys.completedMode, SortHelper.SORT_AUTO)
    override var subtaskMode: Int by pref(Keys.subtaskMode, SortHelper.SORT_MANUAL)
    override var isManualSort: Boolean by pref(Keys.manualSort, false)
    override var isAstridSort: Boolean by pref(Keys.astridSort, false)
    override var sortAscending: Boolean by pref(Keys.sortAscending, true)
    override var groupAscending: Boolean by pref(Keys.groupAscending, true)
    override var completedAscending: Boolean by pref(Keys.completedAscending, false)
    override var subtaskAscending: Boolean by pref(Keys.subtaskAscending, false)
    override val showHidden: Boolean = false
    override val showCompleted: Boolean = true
    override val alwaysDisplayFullDate: Boolean = false
    override var completedTasksAtBottom: Boolean by pref(Keys.completedTasksAtBottom, true)

    private inline fun <reified T> pref(
        key: androidx.datastore.preferences.core.Preferences.Key<T>,
        defaultValue: T,
    ) = object : kotlin.properties.ReadWriteProperty<Any?, T> {
        private var cached: T? = null

        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
            if (cached == null) {
                cached = runBlocking { tasksPreferences.get(key, defaultValue) }
            }
            return cached!!
        }

        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
            cached = value
            runBlocking { tasksPreferences.set(key, value) }
        }
    }

    private object Keys {
        val sortMode = intPreferencesKey("sort_mode")
        val groupMode = intPreferencesKey("group_mode")
        val completedMode = intPreferencesKey("completed_mode")
        val subtaskMode = intPreferencesKey("subtask_mode")
        val manualSort = booleanPreferencesKey("manual_sort")
        val astridSort = booleanPreferencesKey("astrid_sort")
        val sortAscending = booleanPreferencesKey("sort_ascending")
        val groupAscending = booleanPreferencesKey("group_ascending")
        val completedAscending = booleanPreferencesKey("completed_ascending")
        val subtaskAscending = booleanPreferencesKey("subtask_ascending")
        val completedTasksAtBottom = booleanPreferencesKey("completed_tasks_at_bottom")
    }
}
