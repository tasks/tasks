package org.tasks.auth

import org.jetbrains.compose.resources.StringResource
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.url_host_name_required
import tasks.kmp.generated.resources.url_invalid_scheme

private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://(.*)$")

fun serverUrlError(url: String): StringResource? {
    if (url.any { it.isWhitespace() }) return Res.string.url_invalid_scheme
    val match = SCHEME.matchEntire(url) ?: return Res.string.url_invalid_scheme
    val (scheme, rest) = match.destructured
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) {
        return Res.string.url_invalid_scheme
    }
    val hostPort = rest
        .takeWhile { it != '/' && it != '?' && it != '#' }
        .substringAfterLast('@')
    val host = if (hostPort.startsWith("[")) hostPort.substringBefore(']').drop(1) else hostPort.substringBefore(':')
    if (host.isEmpty()) return Res.string.url_host_name_required
    return null
}
