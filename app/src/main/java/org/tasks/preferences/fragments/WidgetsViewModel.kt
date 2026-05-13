package org.tasks.preferences.fragments

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.compose.settings.WidgetItem
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.widget.AppWidgetManager
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@HiltViewModel
class WidgetsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val appWidgetManager: AppWidgetManager,
) : ViewModel() {

    var widgets by mutableStateOf(emptyList<WidgetItem>())
        private set

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            widgets = appWidgetManager
                .widgetIds
                .filter { appWidgetManager.exists(it) }
                .map { id ->
                    val widgetPrefs = WidgetPreferences(context, preferences, id)
                    val filter = defaultFilterProvider.getFilterFromPreference(widgetPrefs.filterId)
                    WidgetItem(
                        widgetId = id,
                        filterTitle = filter.title,
                        color = widgetPrefs.color,
                    )
                }
        }
    }
}
