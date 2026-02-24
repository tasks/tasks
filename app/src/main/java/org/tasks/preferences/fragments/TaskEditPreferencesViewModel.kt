package org.tasks.preferences.fragments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.R
import org.tasks.preferences.Preferences
import javax.inject.Inject

@HiltViewModel
class TaskEditPreferencesViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {

    var showLinks by mutableStateOf(false)
        private set
    var backButtonSaves by mutableStateOf(false)
        private set
    var multilineTitle by mutableStateOf(false)
        private set
    var showComments by mutableStateOf(false)
        private set
    var showWithoutUnlock by mutableStateOf(false)
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        showLinks = preferences.getBoolean(R.string.p_linkify_task_edit, false)
        backButtonSaves = preferences.getBoolean(R.string.p_back_button_saves_task, false)
        multilineTitle = preferences.getBoolean(R.string.p_multiline_title, false)
        showComments = preferences.getBoolean(R.string.p_show_task_edit_comments, false)
        showWithoutUnlock = preferences.getBoolean(
            R.string.p_show_edit_screen_without_unlock, false
        )
    }

    fun updateShowLinks(enabled: Boolean) {
        preferences.setBoolean(R.string.p_linkify_task_edit, enabled)
        showLinks = enabled
    }

    fun updateBackButtonSaves(enabled: Boolean) {
        preferences.setBoolean(R.string.p_back_button_saves_task, enabled)
        backButtonSaves = enabled
    }

    fun updateMultilineTitle(enabled: Boolean) {
        preferences.setBoolean(R.string.p_multiline_title, enabled)
        multilineTitle = enabled
    }

    fun updateShowComments(enabled: Boolean) {
        preferences.setBoolean(R.string.p_show_task_edit_comments, enabled)
        showComments = enabled
    }

    fun updateShowWithoutUnlock(enabled: Boolean) {
        preferences.setBoolean(R.string.p_show_edit_screen_without_unlock, enabled)
        showWithoutUnlock = enabled
    }
}
