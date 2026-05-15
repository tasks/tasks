package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tasks.preferences.TasksPreferences
import org.tasks.themes.BLUE
import java.util.Locale

open class LookAndFeelViewModel(
    private val tasksPreferences: TasksPreferences,
) : ViewModel() {

    var themeIndex by mutableIntStateOf(5)
        private set
    var themeColor by mutableIntStateOf(BLUE)
        private set
    var markdownEnabled by mutableStateOf(false)
        private set
    var openLastViewedList by mutableStateOf(true)
        private set
    var languageTag by mutableStateOf("")
        private set
    var showRestartDialog by mutableStateOf(false)
        private set

    val localeName: String
        get() {
            val tag = languageTag
            if (tag.isBlank()) return ""
            return try {
                val locale = Locale.forLanguageTag(tag)
                locale.getDisplayName(locale)
            } catch (_: Exception) {
                tag
            }
        }

    init {
        viewModelScope.launch {
            themeIndex = tasksPreferences.get(TasksPreferences.theme, 5)
            themeColor = tasksPreferences.get(TasksPreferences.themeColor, BLUE)
            markdownEnabled = tasksPreferences.get(TasksPreferences.markdownEnabled, false)
            openLastViewedList = tasksPreferences.get(TasksPreferences.openLastViewedList, true)
            languageTag = tasksPreferences.get(TasksPreferences.languageTag, "")
        }
    }

    fun setTheme(index: Int) {
        themeIndex = index
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.theme, index)
        }
    }

    fun updateThemeColor(color: Int) {
        themeColor = color
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.themeColor, color)
        }
    }

    fun updateMarkdownEnabled(enabled: Boolean) {
        markdownEnabled = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.markdownEnabled, enabled)
        }
    }

    fun updateOpenLastViewedList(enabled: Boolean) {
        openLastViewedList = enabled
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.openLastViewedList, enabled)
        }
    }

    fun updateLanguageTag(tag: String) {
        languageTag = tag
        if (tag.isNotBlank()) {
            Locale.setDefault(Locale.forLanguageTag(tag))
        }
        showRestartDialog = true
        viewModelScope.launch {
            tasksPreferences.set(TasksPreferences.languageTag, tag)
        }
    }

    fun dismissRestartDialog() {
        showRestartDialog = false
    }

    companion object {
        val LOCALE_TAGS = listOf(
            "en", "af", "ar", "ast", "be", "bg", "bn", "bs", "ca", "ckb",
            "cs", "cv", "da", "de", "el", "eo", "es", "et", "eu", "fa",
            "fi", "fr", "gl", "hr", "hu", "hy", "ia", "id", "it", "iw",
            "he", "ja", "kmr", "kn", "ko", "krl", "lt", "ml", "mr", "my",
            "nah", "nb", "ne", "nl", "or", "pl", "pt", "pt-BR", "ro", "ru",
            "si", "sk", "sl", "sr", "sv", "szl", "ta", "th", "tl", "tr",
            "uk", "ur", "vi", "zh-CN", "zh-TW",
        )
    }
}
