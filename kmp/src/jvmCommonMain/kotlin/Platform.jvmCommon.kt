package org.tasks.kmp

actual fun osDescription(): String =
    "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})"

actual fun languageDisplayName(languageTag: String): String? =
    languageTag
        .takeIf { it.isNotBlank() }
        ?.let { runCatching { java.util.Locale.forLanguageTag(it) }.getOrNull() }
        ?.takeIf { it.language.isNotBlank() }
        ?.let { it.getDisplayName(it) }
        ?.takeIf { it.isNotBlank() }
