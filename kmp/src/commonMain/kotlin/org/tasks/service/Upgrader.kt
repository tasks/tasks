package org.tasks.service

import co.touchlab.kermit.Logger
import org.tasks.preferences.TasksPreferences

class Upgrader(
    private val preferences: TasksPreferences,
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
        if (from > 0) {
            steps
                .sortedBy { it.version }
                .filter { from < it.version }
                .forEach { step ->
                    step.upgrade().run()
                    preferences.set(TasksPreferences.currentVersion, step.version)
                }
        }
        preferences.set(TasksPreferences.currentVersion, to)
    }

    companion object {
        private const val TAG = "Upgrader"
    }
}
