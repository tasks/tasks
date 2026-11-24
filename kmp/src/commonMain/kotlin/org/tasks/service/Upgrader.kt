package org.tasks.service

import androidx.room3.useWriterConnection
import co.touchlab.kermit.Logger
import org.tasks.data.db.Database
import org.tasks.preferences.TasksPreferences

class Upgrader(
    private val preferences: TasksPreferences,
    private val database: Database,
    private val steps: List<UpgradeStep>,
) {
    suspend fun upgrade(to: Int) {
        if (to <= 0) {
            return
        }
        val from = preferences.get(TasksPreferences.currentVersion, 0)
        if (from == to) {
            return
        }
        Logger.i(TAG) { "Upgrading $from => $to" }
        database.useWriterConnection { /* forces schema migration here */ }
        steps.runFrom(from) { preferences.set(TasksPreferences.currentVersion, it) }
        preferences.set(TasksPreferences.currentVersion, to)
    }

    companion object {
        private const val TAG = "Upgrader"
    }
}
