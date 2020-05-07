package org.tasks.preferences.fragments

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.preference.*
import com.todoroo.astrid.api.Filter
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.Locale
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
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
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var widgetPreferences: WidgetPreferences
    private var appWidgetId = 0

    override fun getPreferenceXml() = R.xml.preferences_widget

    override fun setupPreferences(savedInstanceState: Bundle?) {
        appWidgetId = requireArguments().getInt(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetPreferences = WidgetPreferences(context, preferences, appWidgetId)

        val row = setupSlider(R.string.p_widget_opacity, 100)
        val header = setupSlider(R.string.p_widget_header_opacity, row.value)
        val footer = setupSlider(R.string.p_widget_footer_opacity, row.value)

        val opacity = findPreference(R.string.opacity) as SeekBarPreference
        opacity.value = maxOf(header.value, row.value, footer.value)
        if (header.value != row.value || header.value != footer.value) {
            (findPreference(R.string.preferences_advanced) as PreferenceCategory).initialExpandedChildrenCount = 4
        }
        opacity.setOnPreferenceChangeListener { _, newValue ->
            header.value = newValue as Int
            row.value = newValue
            footer.value = newValue
            true
        }

        setupSlider(R.string.p_widget_font_size, 16)
        setupCheckbox(R.string.p_widget_show_checkboxes)
        setupCheckbox(R.string.p_widget_show_dividers)
        setupCheckbox(R.string.p_widget_show_subtasks)
        setupCheckbox(R.string.p_widget_show_full_task_title, false)
        val showDescription = setupCheckbox(R.string.p_widget_show_description, true)
        setupCheckbox(R.string.p_widget_show_full_description, false).dependency = showDescription.key
        setupList(R.string.p_widget_spacing)
        setupList(R.string.p_widget_footer_click)
        setupList(R.string.p_widget_due_date_click)
        setupList(R.string.p_widget_due_date_position, widgetPreferences.dueDatePosition.toString())
        val showHeader = setupCheckbox(R.string.p_widget_show_header)
        val showSettings = setupCheckbox(R.string.p_widget_show_settings)
        showSettings.dependency = showHeader.key
        val showMenu = setupCheckbox(R.string.p_widget_show_menu)
        showMenu.dependency = showHeader.key
        header.dependency = showHeader.key

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
                newThemePickerDialog(this, REQUEST_THEME_SELECTION, widgetPreferences.themeIndex, true)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
                false
            }

        val colorPreference = findPreference(R.string.p_widget_color_v2)
        colorPreference.dependency = showHeader.key
        colorPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            newColorPalette(this, REQUEST_COLOR_SELECTION, widgetPreferences.color, Palette.WIDGET)
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

        localBroadcastManager.reconfigureWidget(appWidgetId)
    }

    private fun updateTheme() {
        val widgetNames = resources.getStringArray(R.array.widget_themes)
        findPreference(R.string.p_widget_theme).summary = widgetNames[widgetPreferences.themeIndex]
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

    private fun setupCheckbox(resId: Int, defaultValue: Boolean = true): SwitchPreferenceCompat {
        val preference = findPreference(resId) as SwitchPreferenceCompat
        val key = getString(resId) + appWidgetId
        preference.key = key
        preference.isChecked = preferences.getBoolean(key, defaultValue)
        return preference
    }

    private fun setupList(resId: Int, defaultValue: String = "0"): ListPreference {
        val preference = findPreference(resId) as ListPreference
        val key = getString(resId) + appWidgetId
        preference.key = key
        preference.value = preferences.getStringValue(key) ?: defaultValue
        return preference
    }

    override fun inject(component: FragmentComponent) = component.inject(this)
}