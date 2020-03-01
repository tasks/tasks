package org.tasks.preferences.fragments

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.astrid.api.Filter
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.Locale
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeCache
import org.tasks.themes.WidgetTheme
import org.tasks.widget.TasksWidget
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject

private const val REQUEST_FILTER = 1005
private const val REQUEST_THEME_SELECTION = 1006
private const val REQUEST_COLOR_SELECTION = 1007

const val EXTRA_WIDGET_ID = "extra_widget_id"

class ScrollableWidget : InjectingPreferenceFragment() {

    companion object {
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"

        fun newScrollableWidget(appWidgetId: Int): ScrollableWidget {
            val widget = ScrollableWidget()
            val args = Bundle()
            args.putInt(EXTRA_WIDGET_ID, appWidgetId)
            widget.arguments = args
            return widget
        }
    }

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var themeCache: ThemeCache
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var widgetPreferences: WidgetPreferences
    private var appWidgetId = 0

    override fun getPreferenceXml() = R.xml.preferences_widget

    override fun setupPreferences(savedInstanceState: Bundle?) {
        appWidgetId = arguments!!.getInt(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetPreferences = WidgetPreferences(context, preferences, appWidgetId)

        setupSlider(R.string.p_widget_opacity, 100)
        setupSlider(R.string.p_widget_font_size, 16)
        setupCheckbox(R.string.p_widget_show_due_date)
        setupCheckbox(R.string.p_widget_show_checkboxes)
        val showHeader = setupCheckbox(R.string.p_widget_show_header)
        val showSettings = setupCheckbox(R.string.p_widget_show_settings)
        showSettings.dependency = showHeader.key

        findPreference(R.string.p_widget_filter)
            .setOnPreferenceClickListener {
                val intent = Intent(context, FilterSelectionActivity::class.java)
                intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, getFilter())
                intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true)
                startActivityForResult(intent, REQUEST_FILTER)
                false
            }

        findPreference(R.string.p_widget_theme)
            .setOnPreferenceClickListener {
                newColorPalette(this, REQUEST_THEME_SELECTION, ColorPickerAdapter.Palette.WIDGET_BACKGROUND)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
                false
            }

        val colorPreference = findPreference(R.string.p_widget_color_v2)
        colorPreference.dependency = showHeader.key
        colorPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            newColorPalette(this, REQUEST_COLOR_SELECTION, widgetPreferences.color)
                .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            false
        }

        updateFilter()
        updateTheme()
        updateColor()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILTER) {
            if (resultCode == Activity.RESULT_OK) {
                val filter: Filter =
                    data!!.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER)!!
                widgetPreferences.setFilter(defaultFilterProvider.getFilterPreferenceValue(filter))
                updateFilter()
            }
        } else if (requestCode == REQUEST_THEME_SELECTION) {
            if (resultCode == Activity.RESULT_OK) {
                widgetPreferences.setTheme(
                    data?.getIntExtra(
                        ColorPalettePicker.EXTRA_SELECTED,
                        0
                    ) ?: widgetPreferences.themeIndex
                )
                updateTheme()
            }
        } else if (requestCode == REQUEST_COLOR_SELECTION) {
            if (resultCode == Activity.RESULT_OK) {
                widgetPreferences.color = data!!.getIntExtra(
                    ColorWheelPicker.EXTRA_SELECTED,
                    0
                )
                updateColor()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.broadcastRefresh()
        // force update after setting preferences
        val intent = Intent(context, TasksWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        activity!!.sendBroadcast(intent)
    }

    private fun updateTheme() {
        val widgetTheme: WidgetTheme = themeCache.getWidgetTheme(widgetPreferences.themeIndex)
        findPreference(R.string.p_widget_theme).summary = widgetTheme.name
    }

    private fun updateColor() {
        tintColorPreference(R.string.p_widget_color_v2, widgetPreferences.color)
    }

    private fun updateFilter() {
        findPreference(R.string.p_widget_filter).summary = getFilter()!!.listingTitle
    }

    private fun getFilter(): Filter? {
        return defaultFilterProvider.getFilterFromPreference(widgetPreferences.filterId)
    }

    private fun setupSlider(resId: Int, defValue: Int): SeekBarPreference {
        val preference = findPreference(resId) as SeekBarPreference
        preference.key = widgetPreferences.getKey(resId)
        preference.value = preferences.getInt(preference.key, defValue)
        return preference
    }

    private fun setupCheckbox(resId: Int): SwitchPreferenceCompat {
        val preference = findPreference(resId) as SwitchPreferenceCompat
        val key = getString(resId) + appWidgetId
        preference.key = key
        preference.isChecked = preferences.getBoolean(key, true)
        return preference
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}