package org.tasks.preferences.fragments

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.settings.WidgetsScreen
import com.todoroo.andlib.utility.AndroidUtilities.atLeastS
import org.tasks.preferences.BasePreferences
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import org.tasks.widget.RequestPinWidgetReceiver
import org.tasks.widget.ShortcutConfigActivity
import org.tasks.widget.TasksWidget
import org.tasks.widget.WidgetConfigActivity
import javax.inject.Inject

@AndroidEntryPoint
class Widgets : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: WidgetsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            val context = requireContext()
            val showAddShortcut = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
            val showAddWidget = context.getSystemService(AppWidgetManager::class.java)
                .isRequestPinAppWidgetSupported
            WidgetsScreen(
                widgets = viewModel.widgets,
                showAddShortcut = showAddShortcut,
                showAddWidget = showAddWidget,
                onWidgetClick = { widgetId ->
                    val intent = Intent(context, WidgetConfigActivity::class.java)
                    intent.putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId,
                    )
                    intent.action = "widget_settings"
                    startActivity(intent)
                },
                onAddShortcut = {
                    startActivity(Intent(context, ShortcutConfigActivity::class.java))
                },
                onAddWidget = {
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                    val provider = ComponentName(context, TasksWidget::class.java)
                    val configIntent = Intent(context, RequestPinWidgetReceiver::class.java).apply {
                        action = RequestPinWidgetReceiver.ACTION_CONFIGURE_WIDGET
                    }
                    val successCallback = PendingIntent.getBroadcast(
                        context,
                        0,
                        configIntent,
                        if (atLeastS()) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    appWidgetManager.requestPinAppWidget(provider, null, successCallback)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }
}
