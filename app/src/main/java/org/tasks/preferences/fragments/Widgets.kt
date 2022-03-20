package org.tasks.preferences.fragments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.IconPreference
import org.tasks.preferences.Preferences
import org.tasks.widget.AppWidgetManager
import org.tasks.widget.WidgetConfigActivity
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

@AndroidEntryPoint
class Widgets : InjectingPreferenceFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var appWidgetManager: AppWidgetManager

    override fun getPreferenceXml() = R.xml.preferences_widgets

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {}

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            preferenceScreen.removeAll()
            appWidgetManager.widgetIds.forEach {
                val widgetPrefs = WidgetPreferences(context, preferences, it)
                val pref = IconPreference(context)
                tintColorPreference(pref, widgetPrefs.color)
                pref.drawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.ic_keyboard_arrow_right_24px
                )?.mutate()
                pref.tint = context?.getColor(R.color.icon_tint_with_alpha)
                pref.iconVisible = true
                val filter = defaultFilterProvider.getFilterFromPreference(widgetPrefs.filterId)
                pref.title = filter.listingTitle
                pref.summary = getString(R.string.widget_id, it)
                val intent = Intent(context, WidgetConfigActivity::class.java)
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, it)
                intent.action = "widget_settings"
                pref.setOnPreferenceClickListener {
                    startActivity(intent)
                    false
                }
                preferenceScreen.addPreference(pref)
            }
        }
    }
}