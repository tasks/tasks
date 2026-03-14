package org.tasks.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    suspend fun removeByPrefix(prefix: String) {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith(prefix) }
                .forEach { prefs.remove(it) }
        }
    }

    suspend fun <T> getAndSet(key: Preferences.Key<T>, value: T): T? {
        var previous: T? = null
        dataStore.edit {
            previous = it[key]
            it[key] = value
        }
        return previous
    }

    companion object {
        val collapseFilters = booleanPreferencesKey("drawer_collapse_filters")
        val collapseTags = booleanPreferencesKey("drawer_collapse_tags")
        val showDebugFilters = booleanPreferencesKey("show_debug_filters")
        val collapseDebug = booleanPreferencesKey("drawer_collapse_debug")
        val collapsePlaces = booleanPreferencesKey("drawer_collapse_places")
        val acceptedTosVersion = intPreferencesKey("accepted_tos_version")
        val hasLoggedOnboardingComplete = booleanPreferencesKey("has_logged_onboarding_complete")
        val subscriptionDismissedAccounts = stringSetPreferencesKey("subscription_dismissed_accounts")
        val syncSource = stringPreferencesKey("sync_source")
        val cachedAccountData = stringPreferencesKey("cached_account_data")
        val serverEnvironment = stringPreferencesKey("server_environment")
        val syncOngoing = booleanPreferencesKey("sync_ongoing")
        val syncOngoingAndroid = booleanPreferencesKey("sync_ongoing_android")
    }
}
