package org.tasks.preferences.fragments

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.SortSettingsActivity
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.filters.AstridOrderingFilter
import org.tasks.filters.Filter
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.widget.WidgetPreferences
import javax.inject.Inject


@AndroidEntryPoint
class WidgetSettings : InjectingPreferenceFragment() {

    companion object {
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        private const val REQUEST_KEY_COLOR = "widget_color_picker_result"

        fun newWidgetSettings(appWidgetId: Int): WidgetSettings {
            val widget = WidgetSettings()
            val args = Bundle()
            args.putInt(EXTRA_WIDGET_ID, appWidgetId)
            widget.arguments = args
            return widget
        }
    }

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var widgetPreferences: WidgetPreferences
    private var appWidgetId = 0
    private val listPickerLauncher = registerForFilterPickerResult {
        widgetPreferences.setFilter(defaultFilterProvider.getFilterPreferenceValue(it))
        updateFilter()
    }

    override fun getPreferenceXml() = R.xml.preferences_widget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            ThemePickerDialog.REQUEST_KEY, this
        ) { _, bundle ->
            val selected = bundle.getInt(ThemePickerDialog.EXTRA_SELECTED, widgetPreferences.themeIndex)
            widgetPreferences.setTheme(selected)
            updateTheme()
        }
        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY_COLOR, this
        ) { _, bundle ->
            val selected = bundle.getInt(ColorWheelPicker.EXTRA_SELECTED, 0)
            widgetPreferences.setColor(selected)
            updateColor()
        }
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        appWidgetId = requireArguments().getInt(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        widgetPreferences = WidgetPreferences(requireContext(), preferences, appWidgetId)
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
        setupCheckbox(R.string.p_widget_show_start_dates)
        setupCheckbox(R.string.p_widget_show_places)
        setupCheckbox(R.string.p_widget_show_lists)
        setupCheckbox(R.string.p_widget_show_tags)
        setupCheckbox(R.string.p_widget_show_full_task_title, false)
        val showDescription = setupCheckbox(R.string.p_widget_show_description, true)
        setupCheckbox(R.string.p_widget_show_full_description, false).dependency = showDescription.key
        setupList(R.string.p_widget_spacing)
        setupList(R.string.p_widget_header_spacing)
        setupList(R.string.p_widget_due_date_position, widgetPreferences.dueDatePosition.toString())
        val showHeader = setupCheckbox(R.string.p_widget_show_header)
        val showTitle = setupCheckbox(R.string.p_widget_show_title)
        showTitle.dependency = showHeader.key
        val showSettings = setupCheckbox(R.string.p_widget_show_settings)
        showSettings.dependency = showHeader.key
        val showMenu = setupCheckbox(R.string.p_widget_show_menu)
        showMenu.dependency = showHeader.key
        header.dependency = showHeader.key

        findPreference(R.string.p_widget_sort).setOnPreferenceClickListener {
            lifecycleScope.launch {
                val filter = getFilter()
                requireActivity().startActivity(
                    SortSettingsActivity.getIntent(
                        requireActivity(),
                        filter.supportsManualSort(),
                        filter is AstridOrderingFilter,
                        appWidgetId,
                        completedAndHiddenEnabled = filter.supportsHiddenTasks(),
                    )
                )
            }
            false
        }

        findPreference(R.string.p_widget_filter)
            .setOnPreferenceClickListener {
                lifecycleScope.launch {
                    listPickerLauncher.launch(requireContext(), selectedFilter = getFilter())
                }
                false
            }

        findPreference(R.string.p_widget_theme)
            .setOnPreferenceClickListener {
                newThemePickerDialog(widgetPreferences.themeIndex, widget = true)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
                false
            }

        val colorPreference = findPreference(R.string.p_widget_color_v2)
        colorPreference.dependency = showHeader.key
        colorPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            newColorPalette(REQUEST_KEY_COLOR, widgetPreferences.color, Palette.WIDGET)
                .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            false
        }

        updateFilter()
        updateTheme()
        updateColor()
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.reconfigureWidget(appWidgetId)
    }

    private fun updateTheme() {
        val index = widgetPreferences.themeIndex
        val widgetNames = resources.getStringArray(R.array.widget_themes)
        findPreference(R.string.p_widget_theme).summary = widgetNames[index]
        findPreference(R.string.p_widget_color_v2).isVisible = index != 4
    }

    private fun updateColor() {
        tintColorPreference(R.string.p_widget_color_v2, widgetPreferences.color)
    }

    private fun updateFilter() = lifecycleScope.launch {
        findPreference(R.string.p_widget_filter).summary = getFilter().title
    }

    private suspend fun getFilter(): Filter =
            defaultFilterProvider.getFilterFromPreference(widgetPreferences.filterId)

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
}