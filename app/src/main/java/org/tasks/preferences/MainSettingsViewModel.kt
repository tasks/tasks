package org.tasks.preferences

import android.appwidget.AppWidgetManager as SystemAppWidgetManager
import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.BillingClient
import org.tasks.billing.Inventory
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inventory: Inventory,
    private val preferences: Preferences,
    private val appWidgetManager: AppWidgetManager,
    private val billingClient: BillingClient,
) : ViewModel() {

    val showWidgets: Boolean
        get() = appWidgetManager.widgetIds.isNotEmpty()
                || ShortcutManagerCompat.isRequestPinShortcutSupported(context)
                || context.getSystemService(SystemAppWidgetManager::class.java)
                    .isRequestPinAppWidgetSupported

    fun refreshPurchases(onError: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                billingClient.queryPurchases(throwError = true)
                if (inventory.subscription.value == null) {
                    onError(context.getString(R.string.no_google_play_subscription))
                }
            } catch (e: Exception) {
                onError(e.message)
            }
        }
    }

    fun unsubscribe(context: Context) {
        inventory.unsubscribe(context)
    }
}
