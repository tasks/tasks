package org.tasks.kmp

import org.tasks.extensions.parseInteger

actual fun osDescription(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"

actual fun languageDisplayName(languageTag: String): String? =
    languageTag
        .takeIf { it.isNotBlank() }
        ?.let { runCatching { java.util.Locale.forLanguageTag(it) }.getOrNull() }
        ?.takeIf { it.language.isNotBlank() }
        ?.let { it.getDisplayName(it) }
        ?.takeIf { it.isNotBlank() }

actual fun firstDayOfWeek(): kotlinx.datetime.DayOfWeek =
    kotlinx.datetime.DayOfWeek(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek.value)

actual fun parseLocalizedInteger(text: String?): Int? = java.util.Locale.getDefault().parseInteger(text)
