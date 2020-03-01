package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.common.base.Strings
import com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1
import com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop
import com.todoroo.astrid.api.Filter
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.activities.FilterSelectionActivity
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.MyTimePickerDialog.newTimePicker
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.gtasks.PlayServices
import org.tasks.injection.FragmentComponent
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.Locale
import org.tasks.locale.LocalePickerDialog
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeCache
import org.tasks.themes.ThemeCache.EXTRA_THEME_OVERRIDE
import org.tasks.themes.ThemeColor
import org.tasks.time.DateTime
import org.tasks.ui.NavigationDrawerFragment.REQUEST_PURCHASE
import org.tasks.ui.SingleCheckedArrayAdapter
import org.tasks.ui.TimePreference
import org.tasks.ui.Toaster
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.TextStyle
import javax.inject.Inject

private const val REQUEST_THEME_PICKER = 10001
private const val REQUEST_COLOR_PICKER = 10002
private const val REQUEST_ACCENT_PICKER = 10003
private const val REQUEST_LAUNCHER_PICKER = 10004
private const val REQUEST_DEFAULT_LIST = 10005
private const val REQUEST_LOCALE = 10006
private const val REQUEST_MORNING = 10007
private const val REQUEST_AFTERNOON = 10008
private const val REQUEST_EVENING = 10009
private const val REQUEST_NIGHT = 10010
private const val FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker"
private const val FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker"
private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"

class LookAndFeel : InjectingPreferenceFragment(), Preference.OnPreferenceChangeListener {

    @Inject lateinit var themeCache: ThemeCache
    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var themeAccent: ThemeAccent
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var locale: Locale
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var toaster: Toaster

    override fun getPreferenceXml() = R.xml.preferences_look_and_feel

    override fun setupPreferences(savedInstanceState: Bundle?) {
        findPreference(R.string.p_show_subtasks)
            .setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                localBroadcastManager.broadcastRefresh()
                true
            }

        val themePref = findPreference(R.string.p_theme)
        themePref.summary = themeBase.name
        themePref.setOnPreferenceClickListener {
            newThemePickerDialog(this, REQUEST_THEME_PICKER, themeBase.index)
                .show(parentFragmentManager, FRAG_TAG_THEME_PICKER)
            false
        }

        val defaultList = findPreference(R.string.p_default_list)
        val filter: Filter = defaultFilterProvider.defaultFilter
        defaultList.summary = filter.listingTitle
        defaultList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(context, FilterSelectionActivity::class.java)
            intent.putExtra(
                FilterSelectionActivity.EXTRA_FILTER,
                defaultFilterProvider.defaultFilter
            )
            intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true)
            startActivityForResult(intent, REQUEST_DEFAULT_LIST)
            true
        }

        val languagePreference = findPreference(R.string.p_language)
        updateLocale()
        languagePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val dialog = LocalePickerDialog.newLocalePickerDialog()
            dialog.setTargetFragment(this, REQUEST_LOCALE)
            dialog.show(parentFragmentManager, FRAG_TAG_LOCALE_PICKER)
            false
        }

        val startOfWeekPreference: ListPreference = getStartOfWeekPreference()
        startOfWeekPreference.entries = getWeekdayEntries()
        startOfWeekPreference.onPreferenceChangeListener = this

        initializeTimePreference(getMorningPreference(), REQUEST_MORNING)
        initializeTimePreference(getAfternoonPreference(), REQUEST_AFTERNOON)
        initializeTimePreference(getEveningPreference(), REQUEST_EVENING)
        initializeTimePreference(getNightPreference(), REQUEST_NIGHT)

        updateStartOfWeek(preferences.getStringValue(R.string.p_start_of_week)!!)

        requires(R.string.task_list_options, atLeastLollipop(), R.string.p_show_subtasks)

        requires(R.string.settings_localization, atLeastJellybeanMR1(), R.string.p_language)

        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR != "googleplay") {
            removeGroup(R.string.TEA_control_location)
        }
    }

    override fun onResume() {
        super.onResume()

        setupColorPreference(
            R.string.p_theme_color,
            themeColor.pickerColor,
            ColorPickerAdapter.Palette.COLORS,
            REQUEST_COLOR_PICKER
        )
        setupColorPreference(
            R.string.p_theme_accent,
            themeAccent.pickerColor,
            ColorPickerAdapter.Palette.ACCENTS,
            REQUEST_ACCENT_PICKER
        )
        updateLauncherPreference()

        @Suppress("ConstantConditionIf")
        if (BuildConfig.FLAVOR == "googleplay") {
            setupLocationPickers()
        }
    }

    private fun updateLauncherPreference() {
        val launcher = themeCache.getThemeColor(preferences.getInt(R.string.p_theme_launcher, 7))
        setupColorPreference(
            R.string.p_theme_launcher,
            launcher.pickerColor,
            ColorPickerAdapter.Palette.LAUNCHERS,
            REQUEST_LAUNCHER_PICKER
        )
    }

    private fun setupLocationPickers() {
        val choices =
            listOf(getString(R.string.map_provider_mapbox), getString(R.string.map_provider_google))
        val singleCheckedArrayAdapter = SingleCheckedArrayAdapter(context!!, choices, themeAccent)
        val mapProviderPreference = findPreference(R.string.p_map_provider)
        mapProviderPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            dialogBuilder
                .newDialog()
                .setSingleChoiceItems(
                    singleCheckedArrayAdapter,
                    getMapProvider()
                ) { dialog: DialogInterface, which: Int ->
                    if (which == 1) {
                        if (!playServices.refreshAndCheck()) {
                            playServices.resolve(activity)
                            dialog.dismiss()
                            return@setSingleChoiceItems
                        }
                    }
                    preferences.setInt(R.string.p_map_provider, which)
                    mapProviderPreference.summary = choices[which]
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            false
        }
        val mapProvider: Int = getMapProvider()
        mapProviderPreference.summary =
            if (mapProvider == -1) getString(R.string.none) else choices[mapProvider]

        val placeProviderPreference = findPreference(R.string.p_place_provider)
        placeProviderPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            dialogBuilder
                .newDialog()
                .setSingleChoiceItems(
                    singleCheckedArrayAdapter,
                    getPlaceProvider()
                ) { dialog: DialogInterface, which: Int ->
                    if (which == 1) {
                        if (!playServices.refreshAndCheck()) {
                            playServices.resolve(activity)
                            dialog.dismiss()
                            return@setSingleChoiceItems
                        }
                        if (!inventory.hasPro()) {
                            toaster.longToast(R.string.requires_pro_subscription)
                            dialog.dismiss()
                            return@setSingleChoiceItems
                        }
                    }
                    preferences.setInt(R.string.p_place_provider, which)
                    placeProviderPreference.summary = choices[which]
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            false
        }
        val placeProvider: Int = getPlaceProvider()
        placeProviderPreference.summary = choices[placeProvider]
    }

    private fun getPlaceProvider(): Int {
        return if (playServices.isPlayServicesAvailable && inventory.hasPro()) preferences.getInt(
            R.string.p_place_provider,
            0
        ) else 0
    }

    private fun getMapProvider(): Int {
        return if (playServices.isPlayServicesAvailable) preferences.getInt(
            R.string.p_map_provider,
            0
        ) else 0
    }

    private fun setBaseTheme(index: Int) {
        activity?.intent?.removeExtra(EXTRA_THEME_OVERRIDE)
        preferences.setInt(R.string.p_theme, index)
        if (themeBase.index != index) {
            Handler().post {
                themeCache.getThemeBase(index).setDefaultNightMode()
                recreate()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_PURCHASE) {
            val index = if (inventory.hasPro()) {
                data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, themeBase.index)
                    ?: themeBase.index
            } else {
                preferences.getInt(R.string.p_theme, 0)
            }
            setBaseTheme(index)
        } else if (requestCode == REQUEST_THEME_PICKER) {
            val index = data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, themeBase.index)
                ?: preferences.getInt(R.string.p_theme, 0)
            if (resultCode == RESULT_OK) {
                if (inventory.purchasedThemes() || index < 2) {
                    setBaseTheme(index)
                } else {
                    startActivityForResult(
                        Intent(context, PurchaseActivity::class.java),
                        REQUEST_PURCHASE
                    )
                }
            } else {
                setBaseTheme(index)
            }
        } else if (requestCode == REQUEST_COLOR_PICKER) {
            if (resultCode == RESULT_OK) {
                val color = data?.getIntExtra(
                    ColorWheelPicker.EXTRA_SELECTED,
                    themeColor.primaryColor
                )
                    ?: themeColor.primaryColor
                if (preferences.getInt(R.string.p_theme_color, -1) != color) {
                    preferences.setInt(R.string.p_theme_color, color)
                    recreate()
                }
            }
        } else if (requestCode == REQUEST_ACCENT_PICKER) {
            if (resultCode == RESULT_OK) {
                val index = data!!.getIntExtra(ColorPalettePicker.EXTRA_SELECTED, 0)
                if (preferences.getInt(R.string.p_theme_accent, -1) != index) {
                    preferences.setInt(R.string.p_theme_accent, index)
                    recreate()
                }
            }
        } else if (requestCode == REQUEST_LAUNCHER_PICKER) {
            if (resultCode == RESULT_OK) {
                val index = data!!.getIntExtra(ColorPalettePicker.EXTRA_SELECTED, 0)
                setLauncherIcon(index)
                preferences.setInt(R.string.p_theme_launcher, index)
                updateLauncherPreference()
            }
        } else if (requestCode == REQUEST_DEFAULT_LIST) {
            if (resultCode == RESULT_OK) {
                val filter: Filter =
                    data!!.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER)!!
                defaultFilterProvider.defaultFilter = filter
                findPreference(R.string.p_default_list).summary = filter.listingTitle
                localBroadcastManager.broadcastRefresh()
            }
        } else if (requestCode == REQUEST_LOCALE) {
            if (resultCode == RESULT_OK) {
                val newValue: Locale =
                    data!!.getSerializableExtra(LocalePickerDialog.EXTRA_LOCALE) as Locale
                val override: String? = newValue.languageOverride
                if (Strings.isNullOrEmpty(override)) {
                    preferences.remove(R.string.p_language)
                } else {
                    preferences.setString(R.string.p_language, override)
                }
                updateLocale()
                if (locale != newValue) {
                    showRestartDialog()
                }
            }
        } else if (requestCode == REQUEST_MORNING) {
            if (resultCode == RESULT_OK) {
                getMorningPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_AFTERNOON) {
            if (resultCode == RESULT_OK) {
                getAfternoonPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_EVENING) {
            if (resultCode == RESULT_OK) {
                getEveningPreference().handleTimePickerActivityIntent(data)
            }
        } else if (requestCode == REQUEST_NIGHT) {
            if (resultCode == RESULT_OK) {
                getNightPreference().handleTimePickerActivityIntent(data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun updateLocale() {
        val languagePreference = findPreference(R.string.p_language)
        val preference = preferences.getStringValue(R.string.p_language)
        languagePreference.summary = locale.withLanguage(preference).displayName
    }

    private fun setLauncherIcon(index: Int) {
        val packageManager: PackageManager? = context?.packageManager
        for (i in ThemeColor.LAUNCHERS.indices) {
            val componentName = ComponentName(
                context!!,
                "com.todoroo.astrid.activity.TaskListActivity" + ThemeColor.LAUNCHERS[i]
            )
            packageManager?.setComponentEnabledSetting(
                componentName,
                if (index == i) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    override fun inject(component: FragmentComponent) {
        component.inject(this)
    }

    private fun setupColorPreference(
        @StringRes prefId: Int,
        color: Int,
        palette: ColorPickerAdapter.Palette,
        requestCode: Int
    ) {
        tintColorPreference(prefId, color)
        findPreference(prefId).setOnPreferenceClickListener {
            newColorPalette(this, requestCode, color, palette)
                .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            false
        }
    }

    private fun initializeTimePreference(preference: TimePreference, requestCode: Int) {
        preference.onPreferenceChangeListener = this
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val current = DateTime().withMillisOfDay(preference.millisOfDay)
            newTimePicker(this, requestCode, current.millis)
                .show(parentFragmentManager, FRAG_TAG_TIME_PICKER)
            false
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        if (preference == getStartOfWeekPreference()) {
            updateStartOfWeek(newValue.toString())
        } else {
            val millisOfDay = newValue as Int
            if (preference == getMorningPreference()) {
                if (millisOfDay >= getAfternoonPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_morning, R.string.date_shortcut_afternoon)
                    return false
                }
            } else if (preference == getAfternoonPreference()) {
                if (millisOfDay <= getMorningPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_afternoon, R.string.date_shortcut_morning)
                    return false
                } else if (millisOfDay >= getEveningPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_afternoon, R.string.date_shortcut_evening)
                    return false
                }
            } else if (preference == getEveningPreference()) {
                if (millisOfDay <= getAfternoonPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_evening, R.string.date_shortcut_afternoon)
                    return false
                } else if (millisOfDay >= getNightPreference().millisOfDay) {
                    mustComeBefore(R.string.date_shortcut_evening, R.string.date_shortcut_night)
                    return false
                }
            } else if (preference == getNightPreference()) {
                if (millisOfDay <= getEveningPreference().millisOfDay) {
                    mustComeAfter(R.string.date_shortcut_night, R.string.date_shortcut_evening)
                    return false
                }
            }
        }
        return true
    }

    private fun mustComeBefore(settingResId: Int, relativeResId: Int) {
        invalidSetting(R.string.date_shortcut_must_come_before, settingResId, relativeResId)
    }

    private fun mustComeAfter(settingResId: Int, relativeResId: Int) {
        invalidSetting(R.string.date_shortcut_must_come_after, settingResId, relativeResId)
    }

    private fun invalidSetting(errorResId: Int, settingResId: Int, relativeResId: Int) {
        Toast.makeText(
            context,
            getString(errorResId, getString(settingResId), getString(relativeResId)),
            Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun updateStartOfWeek(value: String) {
        val preference = getStartOfWeekPreference()
        val index = preference.findIndexOfValue(value)
        val summary: String? = getWeekdayEntries()?.get(index)
        preference.summary = summary
    }

    private fun getStartOfWeekPreference(): ListPreference {
        return findPreference(R.string.p_start_of_week) as ListPreference
    }

    private fun getWeekdayDisplayName(dayOfWeek: DayOfWeek): String {
        return dayOfWeek.getDisplayName(TextStyle.FULL, locale.locale)
    }

    private fun getMorningPreference(): TimePreference {
        return getTimePreference(R.string.p_date_shortcut_morning)
    }

    private fun getAfternoonPreference(): TimePreference {
        return getTimePreference(R.string.p_date_shortcut_afternoon)
    }

    private fun getEveningPreference(): TimePreference {
        return getTimePreference(R.string.p_date_shortcut_evening)
    }

    private fun getNightPreference(): TimePreference {
        return getTimePreference(R.string.p_date_shortcut_night)
    }

    private fun getTimePreference(resId: Int): TimePreference {
        return findPreference(resId) as TimePreference
    }

    private fun getWeekdayEntries(): Array<String?>? {
        return arrayOf(
            getString(R.string.use_locale_default),
            getWeekdayDisplayName(DayOfWeek.SUNDAY),
            getWeekdayDisplayName(DayOfWeek.MONDAY)
        )
    }
}