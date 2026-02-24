package org.tasks.preferences.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.filters.Filter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeBase.DEFAULT_BASE_THEME
import org.tasks.themes.ThemeColor
import org.tasks.themes.ThemeColor.getLauncherColor
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LookAndFeelViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val inventory: Inventory,
    private val refreshBroadcaster: RefreshBroadcaster,
    private val locale: Locale,
) : ViewModel() {

    var themeName by mutableStateOf("")
        private set
    var dynamicColorEnabled by mutableStateOf(false)
        private set
    var dynamicColorProOnly by mutableStateOf(false)
        private set
    var currentThemeColor by mutableIntStateOf(0)
        private set
    var currentLauncherColor by mutableIntStateOf(0)
        private set
    var markdownEnabled by mutableStateOf(false)
        private set
    var openLastViewedList by mutableStateOf(true)
        private set
    var defaultFilterName by mutableStateOf("")
        private set
    var localeName by mutableStateOf("")
        private set

    val dynamicColorAvailable: Boolean
        get() = DynamicColors.isDynamicColorAvailable()

    private val themeNames: Array<String> = context.resources.getStringArray(R.array.base_theme_names)

    init {
        refreshState(
            themeBaseIndex = preferences.themeBase,
            themeColorPickerColor = preferences.defaultThemeColor,
        )
    }

    private var currentThemeBaseIndex: Int = DEFAULT_BASE_THEME

    fun refreshState(
        themeBaseIndex: Int,
        themeColorPickerColor: Int,
    ) {
        currentThemeBaseIndex = themeBaseIndex
        themeName = themeNames[themeBaseIndex]
        dynamicColorEnabled = if (dynamicColorAvailable && inventory.hasPro) {
            preferences.dynamicColor
        } else {
            false
        }
        dynamicColorProOnly = dynamicColorAvailable && !inventory.hasPro
        currentThemeColor = themeColorPickerColor
        val launcher = getLauncherColor(context, preferences.getInt(R.string.p_theme_launcher, 7))
        currentLauncherColor = launcher.pickerColor
        markdownEnabled = preferences.markdown
        openLastViewedList = preferences.getBoolean(R.string.p_open_last_viewed_list, true)
        localeName = locale.getDisplayName(locale)
        viewModelScope.launch {
            val filter = defaultFilterProvider.getDefaultOpenFilter()
            defaultFilterName = filter.title ?: ""
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        if (!inventory.hasPro) {
            return
        }
        preferences.setBoolean(R.string.p_dynamic_color, enabled)
        dynamicColorEnabled = enabled
    }

    fun updateMarkdown(enabled: Boolean) {
        preferences.setBoolean(R.string.p_markdown, enabled)
        markdownEnabled = enabled
    }

    fun updateOpenLastViewedList(enabled: Boolean) {
        preferences.setBoolean(R.string.p_open_last_viewed_list, enabled)
        openLastViewedList = enabled
    }

    suspend fun getDefaultOpenFilter() = defaultFilterProvider.getDefaultOpenFilter()

    fun setDefaultFilter(filter: Filter) {
        defaultFilterProvider.setDefaultOpenFilter(filter)
        defaultFilterName = filter.title ?: ""
        refreshBroadcaster.broadcastRefresh()
    }

    fun setBaseTheme(index: Int): Boolean {
        preferences.setInt(R.string.p_theme, index)
        return currentThemeBaseIndex != index
    }

    fun handleThemePickerResult(selectedIndex: Int): ThemePickerResult {
        return if (inventory.purchasedThemes() || ThemeBase(selectedIndex).isFree) {
            ThemePickerResult.ApplyTheme(selectedIndex)
        } else {
            ThemePickerResult.PurchaseRequired
        }
    }

    fun handlePurchaseResult(data: Intent?): Int {
        return if (inventory.hasPro) {
            data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, DEFAULT_BASE_THEME)
                ?: currentThemeBaseIndex
        } else {
            preferences.themeBase
        }
    }

    fun handleColorPickerResult(selectedColor: Int): Boolean {
        if (preferences.defaultThemeColor != selectedColor) {
            preferences.setInt(R.string.p_theme_color, selectedColor)
            return true
        }
        return false
    }

    fun handleLauncherPickerResult(context: Context, selectedIndex: Int) {
        setLauncherIcon(context, selectedIndex)
        preferences.setInt(R.string.p_theme_launcher, selectedIndex)
        val launcher = getLauncherColor(context, selectedIndex)
        currentLauncherColor = launcher.pickerColor
    }

    private fun setLauncherIcon(context: Context, index: Int) {
        val packageManager = context.packageManager
        for (i in ThemeColor.LAUNCHERS.indices) {
            val componentName = ComponentName(
                context,
                "com.todoroo.astrid.activity.TaskListActivity" + ThemeColor.LAUNCHERS[i]
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                if (index == i) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    sealed interface ThemePickerResult {
        data class ApplyTheme(val index: Int) : ThemePickerResult
        data object PurchaseRequired : ThemePickerResult
    }
}
