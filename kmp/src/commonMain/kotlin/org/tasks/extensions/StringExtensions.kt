package org.tasks.extensions

fun String.truncate(max: Int, ellipsis: String = ""): String {
    if (max <= 0) {
        return ""
    }
    if (length <= max) {
        return this
    }
    val end = if (this[max - 1].isHighSurrogate()) max - 1 else max
    return substring(0, end) + ellipsis
}

fun String.htmlEscape(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#x27;")
