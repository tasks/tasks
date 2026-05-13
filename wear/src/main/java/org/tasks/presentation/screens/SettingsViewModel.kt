package org.tasks.presentation.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.tasks.presentation.WearSettings

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val wearSettings = WearSettings.getInstance(application)
    val viewState = wearSettings.stateFlow

    fun setShowHidden(showHidden: Boolean) {
        wearSettings.setShowHidden(showHidden)
    }

    fun setShowCompleted(showCompleted: Boolean) {
        wearSettings.setShowCompleted(showCompleted)
    }

    fun setFilter(filter: String) {
        wearSettings.setFilter(filter)
    }

    fun setSortMode(sortMode: Int) {
        wearSettings.setSortMode(sortMode)
    }

    fun setGroupMode(groupMode: Int) {
        wearSettings.setGroupMode(groupMode)
    }
}
