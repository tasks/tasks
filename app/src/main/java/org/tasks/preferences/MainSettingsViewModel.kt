package org.tasks.preferences

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.PlatformConfiguration
import org.tasks.billing.Inventory
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject
import android.appwidget.AppWidgetManager as SystemAppWidgetManager

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val inventory: Inventory,
    private val appWidgetManager: AppWidgetManager,
    platformConfiguration: PlatformConfiguration,
) : org.tasks.viewmodel.MainSettingsViewModel(
    platformConfiguration = platformConfiguration,
) {

    override val supportsWidgets: Boolean
        get() = appWidgetManager.widgetIds.isNotEmpty()
                || ShortcutManagerCompat.isRequestPinShortcutSupported(context)
                || context.getSystemService(SystemAppWidgetManager::class.java)
                    .isRequestPinAppWidgetSupported

    fun unsubscribe(context: Context) {
        viewModelScope.launch { inventory.unsubscribe(context) }
    }
}
