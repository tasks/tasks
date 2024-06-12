package org.tasks.preferences.fragments

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.todoroo.astrid.core.SortHelper.SORT_ALPHA
import com.todoroo.astrid.core.SortHelper.SORT_CREATED
import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.core.SortHelper.SORT_IMPORTANCE
import com.todoroo.astrid.core.SortHelper.SORT_LIST
import com.todoroo.astrid.core.SortHelper.SORT_MODIFIED
import com.todoroo.astrid.core.SortHelper.SORT_START
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.SortSettingsActivity
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
        private const val REQUEST_THEME_SELECTION = 1006
        private const val REQUEST_COLOR_SELECTION = 1007
        private const val REQUEST_SORT = 1008

        const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"

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
    private val listPickerLauncher = registerForListPickerResult {
        widgetPreferences.setFilter(defaultFilterProvider.getFilterPreferenceValue(it))
        updateFilter()
    }

    override fun getPreferenceXml() = R.xml.preferences_widget

    override fun onResume() {
        super.onResume()

        updateSort()
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
        setupCheckbox(R.string.p_widget_show_hidden, true)
        setupCheckbox(R.string.p_widget_show_completed, false)
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
                requireActivity().startActivityForResult(
                    SortSettingsActivity.getIntent(
                        requireActivity(),
                        filter.supportsManualSort(),
                        filter is AstridOrderingFilter,
                        appWidgetId,
                    ),
                    REQUEST_SORT
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
        when (requestCode) {
            REQUEST_THEME_SELECTION -> if (resultCode == Activity.RESULT_OK) {
                widgetPreferences.setTheme(
                        data?.getIntExtra(
                                ColorPalettePicker.EXTRA_SELECTED,
                                0
                        ) ?: widgetPreferences.themeIndex
                )
                updateTheme()
            }
            REQUEST_COLOR_SELECTION -> if (resultCode == Activity.RESULT_OK) {
                widgetPreferences.setColor(
                    data!!.getIntExtra(ColorWheelPicker.EXTRA_SELECTED, 0)
                )
                updateColor()
            }
            REQUEST_SORT -> if (resultCode == Activity.RESULT_OK) updateSort()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
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
        updateSort()
    }

    private fun updateSort() = lifecycleScope.launch {
        val filter = getFilter()
        findPreference(R.string.p_widget_sort).setSummary(
                if (filter.supportsManualSort() && widgetPreferences.isManualSort) {
                    R.string.SSD_sort_my_order
                } else if (filter is AstridOrderingFilter && widgetPreferences.isAstridSort) {
                    R.string.astrid_sort_order
                } else {
                    when (widgetPreferences.sortMode) {
                        SORT_DUE -> R.string.SSD_sort_due
                        SORT_START -> R.string.SSD_sort_start
                        SORT_IMPORTANCE -> R.string.SSD_sort_importance
                        SORT_ALPHA -> R.string.SSD_sort_alpha
                        SORT_MODIFIED -> R.string.SSD_sort_modified
                        SORT_CREATED -> R.string.sort_created
                        SORT_LIST -> R.string.sort_list
                        else -> R.string.SSD_sort_auto
                    }
                })
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