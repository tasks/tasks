package org.tasks.compose.settings

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import org.tasks.R
import org.tasks.activities.BaseListSettingsActivity.Companion.createShortcutIcon
import org.tasks.analytics.Firebase
import org.tasks.data.UUIDHelper
import org.tasks.filters.Filter
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.contentColorFor
import org.tasks.widget.RequestPinWidgetReceiver
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_COLOR
import org.tasks.widget.RequestPinWidgetReceiver.Companion.EXTRA_FILTER
import org.tasks.widget.TasksWidget

fun AppCompatActivity.createShortcut(
    filter: Filter,
    title: String,
    icon: String?,
    color: Color,
    defaultFilterProvider: DefaultFilterProvider,
    firebase: Firebase,
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

    val pinnedShortcutCallbackIntent = ShortcutManagerCompat
        .createShortcutResultIntent(this, shortcutInfo)

    val successCallback = PendingIntent.getBroadcast(
        this, 0,
        pinnedShortcutCallbackIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    ShortcutManagerCompat.requestPinShortcut(
        this,
        shortcutInfo,
        successCallback.intentSender
    )

    firebase.logEvent(R.string.event_create_shortcut, R.string.param_type to "settings_activity")
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
