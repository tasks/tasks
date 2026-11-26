package org.tasks.compose.settings

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.ShortcutManager
import org.tasks.ShortcutManager.ShortcutResult
import org.tasks.activities.BaseListSettingsActivity.Companion.createShortcutIcon
import org.tasks.analytics.Firebase
import org.tasks.data.UUIDHelper
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.contentColorFor
import org.tasks.widget.RequestPinWidgetReceiver
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_COLOR
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_FILTER
import org.tasks.widget.TasksWidget

fun AppCompatActivity.setReloadResult(filter: Filter) {
    setResult(
        Activity.RESULT_OK,
        Intent(TaskListFragment.ACTION_RELOAD).putExtra(MainActivity.OPEN_FILTER, filter),
    )
}

fun AppCompatActivity.setReloadResult(calendar: CaldavCalendar, account: CaldavAccount) =
    setReloadResult(CaldavFilter(calendar = calendar, account = account))

private fun AppCompatActivity.finishWithList(calendar: CaldavCalendar, account: CaldavAccount) {
    setReloadResult(calendar, account)
    finish()
}

private fun AppCompatActivity.addToHomeScreen(
    stateProvider: () -> ListSettingsState,
    save: (onSaved: (CaldavCalendar) -> Unit) -> Unit,
    add: suspend (state: ListSettingsState, filter: CaldavFilter) -> Unit,
): () -> Unit = click@{
    val s = stateProvider()
    val account = s.account ?: return@click
    // Save (creating or updating as needed) then return to the list, matching the Save button.
    save { calendar ->
        lifecycleScope.launch {
            add(s, CaldavFilter(calendar = calendar, account = account))
            finishWithList(calendar, account)
        }
    }
}

fun AppCompatActivity.canPinShortcut(): Boolean =
    ShortcutManagerCompat.isRequestPinShortcutSupported(this)

fun AppCompatActivity.canPinWidget(): Boolean =
    getSystemService(AppWidgetManager::class.java).isRequestPinAppWidgetSupported

fun AppCompatActivity.addShortcutCallback(
    stateProvider: () -> ListSettingsState,
    primaryColor: Color,
    defaultFilterProvider: DefaultFilterProvider,
    firebase: Firebase,
    shortcutManager: ShortcutManager,
    save: (onSaved: (CaldavCalendar) -> Unit) -> Unit,
): (() -> Unit)? {
    if (!canPinShortcut()) return null
    if (stateProvider().account == null) return null
    return addToHomeScreen(stateProvider, save) { s, filter ->
        createShortcut(
            filter = filter,
            title = s.name,
            icon = s.icon,
            color = if (s.color != 0) Color(s.color) else primaryColor,
            defaultFilterProvider = defaultFilterProvider,
            firebase = firebase,
            shortcutManager = shortcutManager,
        )
    }
}

fun AppCompatActivity.addWidgetCallback(
    stateProvider: () -> ListSettingsState,
    defaultFilterProvider: DefaultFilterProvider,
    firebase: Firebase,
    save: (onSaved: (CaldavCalendar) -> Unit) -> Unit,
): (() -> Unit)? {
    if (!canPinWidget()) return null
    if (stateProvider().account == null) return null
    return addToHomeScreen(stateProvider, save) { s, filter ->
        createWidget(
            filter = filter,
            color = s.color,
            defaultFilterProvider = defaultFilterProvider,
            firebase = firebase,
        )
    }
}

suspend fun AppCompatActivity.createShortcut(
    filter: Filter,
    title: String,
    icon: String?,
    color: Color,
    defaultFilterProvider: DefaultFilterProvider,
    firebase: Firebase,
    shortcutManager: ShortcutManager,
) {
    val filterId = defaultFilterProvider.getFilterPreferenceValue(filter)
    val shortcutInfo = ShortcutInfoCompat.Builder(this, UUIDHelper.newUUID())
        .setShortLabel(title.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
        .setIcon(
            icon?.let {
                try {
                    createShortcutIcon(
                        context = this,
                        backgroundColor = color,
                        icon = it,
                        iconColor = contentColorFor(color.toArgb()),
                    )
                } catch (e: Exception) {
                    firebase.reportException(e)
                    null
                }
            } ?: createShortcutIcon(this, backgroundColor = color)
        )
        .setIntent(TaskIntents.getTaskListByIdIntent(this, filterId))
        .build()

    val result = shortcutManager.createShortcut(shortcutInfo, launcherRequest = false, source = "settings_activity")
    if (result == ShortcutResult.Failed) {
        Toast.makeText(applicationContext, R.string.shortcut_creation_failed, Toast.LENGTH_LONG).show()
    }
}

fun AppCompatActivity.createWidget(
    filter: Filter,
    color: Int,
    defaultFilterProvider: DefaultFilterProvider,
    firebase: Firebase,
) {
    val appWidgetManager = getSystemService(AppWidgetManager::class.java)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val provider = ComponentName(this, TasksWidget::class.java)
        val configIntent = Intent(this, RequestPinWidgetReceiver::class.java).apply {
            action = RequestPinWidgetReceiver.ACTION_CONFIGURE_WIDGET
            putExtra(EXTRA_FILTER, defaultFilterProvider.getFilterPreferenceValue(filter))
            putExtra(EXTRA_COLOR, color)
        }
        val successCallback = PendingIntent.getBroadcast(
            this,
            filter.hashCode(),
            configIntent,
            if (atLeastS()) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
        appWidgetManager.requestPinAppWidget(provider, null, successCallback)
        firebase.logEvent(R.string.event_create_widget, R.string.param_type to "settings_activity")
    }
}
