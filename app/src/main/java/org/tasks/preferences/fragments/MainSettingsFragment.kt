package org.tasks.preferences.fragments

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.widget.AppWidgetManager
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment : InjectingPreferenceFragment() {

    @Inject lateinit var appWidgetManager: AppWidgetManager

    override fun getPreferenceXml() = R.xml.preferences

    override fun setupPreferences(savedInstanceState: Bundle?) {
        requires(BuildConfig.DEBUG, R.string.debug)

        requires(appWidgetManager.widgetIds.isNotEmpty(), R.string.widget_settings)
    }
}
