package org.tasks.widget

import androidx.core.content.pm.ShortcutInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.ShortcutManager
import org.tasks.ShortcutManager.ShortcutResult
import org.tasks.analytics.Firebase
import javax.inject.Inject

@HiltViewModel
class ShortcutConfigViewModel @Inject constructor(
    private val shortcutManager: ShortcutManager,
    private val firebase: Firebase,
) : ViewModel() {
    private val _results = Channel<ShortcutResult>(Channel.BUFFERED)
    val results: Flow<ShortcutResult> = _results.receiveAsFlow()
    private var request: Job? = null

    fun save(shortcut: ShortcutInfoCompat, launcherRequest: Boolean) {
        if (request?.isActive == true) {
            return
        }
        request = viewModelScope.launch {
            val result = shortcutManager.createShortcut(shortcut, launcherRequest, SOURCE)
            if (launcherRequest && result is ShortcutResult.Created) {
                firebase.logEvent(R.string.event_create_shortcut, R.string.param_type to SOURCE)
            }
            _results.send(result)
        }
    }

    companion object {
        const val SOURCE = "shortcut_config"
    }
}
