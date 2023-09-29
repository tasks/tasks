package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.FilterPicker.Companion.newFilterPicker
import org.tasks.dialogs.FilterPicker.Companion.setFilterPickerResultListener
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.extensions.Context.isNightMode
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.LocalePickerDialog
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeBase.DEFAULT_BASE_THEME
import org.tasks.themes.ThemeBase.EXTRA_THEME_OVERRIDE
import org.tasks.themes.ThemeColor
import org.tasks.themes.ThemeColor.getLauncherColor
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LookAndFeel : InjectingPreferenceFragment() {

    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var themeAccent: ThemeAccent
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var locale: Locale

    override fun getPreferenceXml() = R.xml.preferences_look_and_feel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFilterPickerResultListener(this) {
            defaultFilterProvider.setDefaultOpenFilter(it)
            findPreference(R.string.p_default_open_filter).summary = it.title
            localBroadcastManager.broadcastRefresh()
        }
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        val themePref = findPreference(R.string.p_theme)
        val themeNames = resources.getStringArray(R.array.base_theme_names)
        themePref.summary = themeNames[themeBase.index]
        themePref.setOnPreferenceClickListener {
            newThemePickerDialog(this, REQUEST_THEME_PICKER, themeBase.index)
                .show(parentFragmentManager, FRAG_TAG_THEME_PICKER)
            false
        }

        findPreference(R.string.p_desaturate_colors).setOnPreferenceChangeListener { _, _ ->
            if (context?.isNightMode == true) {
                activity?.recreate()
            }
            true
        }

        val defaultList = findPreference(R.string.p_default_open_filter)
        val filter = defaultFilterProvider.getDefaultOpenFilter()
        defaultList.summary = filter.title
        defaultList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launch {
                newFilterPicker(defaultFilterProvider.getDefaultOpenFilter())
                    .show(childFragmentManager, FRAG_TAG_FILTER_PICKER)
            }
            true
        }

        val languagePreference = findPreference(R.string.p_language)
        languagePreference.summary = locale.getDisplayName(locale)
        languagePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val dialog = LocalePickerDialog.newLocalePickerDialog()
            dialog.setTargetFragment(this, REQUEST_LOCALE)
            dialog.show(parentFragmentManager, FRAG_TAG_LOCALE_PICKER)
            false
        }

        openUrl(R.string.translations, R.string.url_translations)
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
    }

    private fun updateLauncherPreference() {
        val launcher = getLauncherColor(context, preferences.getInt(R.string.p_theme_launcher, 7))
        setupColorPreference(
            R.string.p_theme_launcher,
            launcher.pickerColor,
            ColorPickerAdapter.Palette.LAUNCHERS,
            REQUEST_LAUNCHER_PICKER
        )
    }

    private fun setBaseTheme(index: Int) {
        activity?.intent?.removeExtra(EXTRA_THEME_OVERRIDE)
        preferences.setInt(R.string.p_theme, index)
        if (themeBase.index != index) {
            Handler().post {
                ThemeBase(index).setDefaultNightMode()
                recreate()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PURCHASE -> {
                val index = if (inventory.hasPro) {
                    data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, DEFAULT_BASE_THEME)
                            ?: themeBase.index
                } else preferences.themeBase
                setBaseTheme(index)
            }
            REQUEST_THEME_PICKER -> {
                val index = data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, DEFAULT_BASE_THEME)
                        ?: preferences.themeBase
                if (resultCode == RESULT_OK) {
                    if (inventory.purchasedThemes() || ThemeBase(index).isFree) {
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
            }
            REQUEST_COLOR_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val color = data?.getIntExtra(
                            ColorWheelPicker.EXTRA_SELECTED,
                            themeColor.primaryColor
                    ) ?: themeColor.primaryColor

                    if (preferences.defaultThemeColor != color) {
                        preferences.setInt(R.string.p_theme_color, color)
                        recreate()
                    }
                }
            }
            REQUEST_ACCENT_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val index = data!!.getIntExtra(ColorPalettePicker.EXTRA_SELECTED, 0)
                    if (preferences.getInt(R.string.p_theme_accent, -1) != index) {
                        preferences.setInt(R.string.p_theme_accent, index)
                        recreate()
                    }
                }
            }
            REQUEST_LAUNCHER_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val index = data!!.getIntExtra(ColorPalettePicker.EXTRA_SELECTED, 0)
                    setLauncherIcon(index)
                    preferences.setInt(R.string.p_theme_launcher, index)
                    updateLauncherPreference()
                }
            }
            REQUEST_LOCALE -> {
                if (resultCode == RESULT_OK) {
                    val languageTag = data!!.getStringExtra(LocalePickerDialog.EXTRA_LOCALE)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(languageTag)
                    )
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun setLauncherIcon(index: Int) {
        val packageManager: PackageManager? = context?.packageManager
        for (i in ThemeColor.LAUNCHERS.indices) {
            val componentName = ComponentName(
                requireContext(),
                "com.todoroo.astrid.activity.TaskListActivity" + ThemeColor.LAUNCHERS[i]
            )
            packageManager?.setComponentEnabledSetting(
                componentName,
                if (index == i) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
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

    companion object {
        private const val REQUEST_THEME_PICKER = 10001
        private const val REQUEST_COLOR_PICKER = 10002
        private const val REQUEST_ACCENT_PICKER = 10003
        private const val REQUEST_LAUNCHER_PICKER = 10004
        private const val REQUEST_LOCALE = 10006
        private const val REQUEST_PURCHASE = 10007
        private const val FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker"
        private const val FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        private const val FRAG_TAG_FILTER_PICKER = "frag_tag_filter_picker"
    }
}