package org.tasks.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.TasksPreferences
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksServerEnvironment @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tasksPreferences: TasksPreferences,
) {
    private val devUrl = context.getString(R.string.tasks_dev_url)

    val caldavUrl: String
        get() = when (currentEnvironment) {
            ENV_STAGING -> STAGING_URL
            ENV_DEV -> devUrl
            else -> PRODUCTION_CALDAV_URL
        }

    val nominatimUrl: String
        get() = when (currentEnvironment) {
            ENV_STAGING -> STAGING_URL
            ENV_DEV -> devUrl
            else -> PRODUCTION_NOMINATIM_URL
        }

    val placesUrl: String
        get() = when (currentEnvironment) {
            ENV_STAGING -> STAGING_URL
            ENV_DEV -> devUrl
            else -> PRODUCTION_PLACES_URL
        }

    private var currentEnvironment: String = runBlocking {
        tasksPreferences.get(TasksPreferences.serverEnvironment, ENV_PRODUCTION)
    }

    val environments: List<Environment>
        get() = buildList {
            add(Environment(ENV_PRODUCTION, "Production", PRODUCTION_CALDAV_URL))
            add(Environment(ENV_STAGING, "Staging", STAGING_URL))
            if (devUrl.isNotBlank()) {
                add(Environment(ENV_DEV, "Development", devUrl))
            }
        }

    suspend fun getEnvironment(): String {
        currentEnvironment = tasksPreferences.get(
            TasksPreferences.serverEnvironment,
            ENV_PRODUCTION
        )
        return currentEnvironment
    }

    suspend fun setEnvironment(env: String) {
        currentEnvironment = env
        tasksPreferences.set(TasksPreferences.serverEnvironment, env)
    }

    data class Environment(val key: String, val label: String, val caldavUrl: String)

    companion object {
        const val ENV_PRODUCTION = "production"
        const val ENV_STAGING = "staging"
        const val ENV_DEV = "local"

        private const val PRODUCTION_CALDAV_URL = "https://caldav.tasks.org"
        private const val PRODUCTION_NOMINATIM_URL = "https://nominatim.tasks.org"
        private const val PRODUCTION_PLACES_URL = "https://places.tasks.org"
        private const val STAGING_URL = "https://staging.tasks.org"
    }
}
