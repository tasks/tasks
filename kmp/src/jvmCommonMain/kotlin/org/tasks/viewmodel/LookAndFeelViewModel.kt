package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tasks.PlatformConfiguration
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.filters.Filter
import org.tasks.filters.FilterPreferenceCodec
import org.tasks.filters.MyTasksFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.LookAndFeelSettings
import org.tasks.themes.BaseTheme
import java.util.Locale

open class LookAndFeelViewModel(
    private val appPreferences: AppPreferences,
    private val platformConfiguration: PlatformConfiguration,
    private val refreshBroadcaster: RefreshBroadcaster,
    persistenceScope: CoroutineScope,
    private val filterCodec: FilterPreferenceCodec,
) : ViewModel() {

    var settings by mutableStateOf(LookAndFeelSettings())
        private set

    var loaded by mutableStateOf(false)
        private set

    var defaultFilter by mutableStateOf<Filter?>(null)
        private set

    var showRestartDialog by mutableStateOf(false)
        private set

    private val writes = PreferenceWriteQueue(
        viewModelScope = viewModelScope,
        persistenceScope = persistenceScope,
        tag = TAG,
        reload = { reloadSafely() },
    )

    val themeOptions: List<Int> = buildList {
        add(BaseTheme.LIGHT)
        add(BaseTheme.BLACK)
        add(BaseTheme.DARK)
        if (platformConfiguration.supportsWallpaperTheme) add(BaseTheme.WALLPAPER)
        if (platformConfiguration.supportsAutoNightTheme) add(BaseTheme.DAY_NIGHT)
        add(BaseTheme.SYSTEM_DEFAULT)
    }

    val showLauncherIcon: Boolean get() = platformConfiguration.supportsLauncherIcon

    val showMarkdown: Boolean get() = platformConfiguration.supportsMarkdownToggle
    val showLanguage: Boolean get() = platformConfiguration.supportsLanguageSelection

    open val themeIndex: Int get() = settings.theme
    open val themeColor: Int get() = settings.themeColor
    open val launcherColor: Int get() = settings.themeColor
    open val dynamicColorAvailable: Boolean get() = platformConfiguration.supportsDynamicColor
    open val dynamicColorEnabled: Boolean get() = settings.dynamicColor
    open val dynamicColorProOnly: Boolean get() = false

    open val localeName: String
        get() = (settings.languageTag?.toLocaleOrNull() ?: Locale.getDefault()).displayName()

    val defaultFilterName: String get() = defaultFilter?.title.orEmpty()

    init {
        viewModelScope.launch {
            reloadSafely()
        }
    }

    private suspend fun reload() {
        val loadedSettings = appPreferences.lookAndFeelSettings()
        settings = loadedSettings
        defaultFilter = filterCodec.decode(
            loadedSettings.defaultOpenFilter,
            default = MyTasksFilter.create(),
        )
        loaded = true
    }

    private suspend fun reloadSafely() {
        try {
            reload()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to reload look and feel settings" }
        }
    }

    open fun refreshState() = writes.refresh()

    private fun persist(block: suspend () -> Unit) = writes.write {
        block()
        refreshBroadcaster.broadcastRefresh()
    }

    open fun setTheme(index: Int) {
        settings = settings.copy(theme = index)
        persist { appPreferences.setTheme(index) }
    }

    open fun setThemeColor(color: Int) {
        settings = settings.copy(themeColor = color)
        persist { appPreferences.setThemeColor(color) }
    }

    fun setDynamicColor(enabled: Boolean) {
        settings = settings.copy(dynamicColor = enabled)
        persist { appPreferences.setDynamicColor(enabled) }
    }

    fun setMarkdown(enabled: Boolean) {
        settings = settings.copy(markdown = enabled)
        persist { appPreferences.setMarkdown(enabled) }
    }

    fun setOpenLastViewedList(enabled: Boolean) {
        settings = settings.copy(openLastViewedList = enabled)
        persist { appPreferences.setOpenLastViewedList(enabled) }
    }

    fun setDefaultOpenFilter(filter: Filter) {
        val encoded = filterCodec.encode(filter)
        settings = settings.copy(defaultOpenFilter = encoded)
        defaultFilter = filter
        persist { appPreferences.setDefaultOpenFilter(encoded) }
    }

    open fun setLanguage(languageTag: String?) {
        settings = settings.copy(languageTag = languageTag)
        if (platformConfiguration.localeChangeRequiresRestart) {
            showRestartDialog = true
        }
        persist { appPreferences.setLanguageTag(languageTag) }
    }

    fun dismissRestartDialog() {
        showRestartDialog = false
    }
}

internal fun String.toLocaleOrNull(): Locale? =
    takeIf { it.isNotBlank() }
        ?.let { runCatching { Locale.forLanguageTag(it) }.getOrNull() }
        ?.takeIf { it.language.isNotBlank() }

internal fun Locale.displayName(): String = getDisplayName(this)

private const val TAG = "LookAndFeelViewModel"
