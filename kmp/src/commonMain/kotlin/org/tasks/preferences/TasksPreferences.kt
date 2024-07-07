package org.tasks.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class TasksPreferences(private val dataStore: DataStore<Preferences>) {

    suspend fun <T> get(key: Preferences.Key<T>, defaultValue: T): T =
        dataStore.data.map { it[key] }.firstOrNull() ?: defaultValue

    fun <T> flow(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { it[key] ?: defaultValue }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    companion object {
        val collapseFilters = booleanPreferencesKey("drawer_collapse_filters")
        val collapseTags = booleanPreferencesKey("drawer_collapse_tags")
        val collapseDebug = booleanPreferencesKey("drawer_collapse_debug")
        val collapsePlaces = booleanPreferencesKey("drawer_collapse_places")
    }
}