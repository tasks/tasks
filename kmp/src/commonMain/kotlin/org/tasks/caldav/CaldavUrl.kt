package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.toUrlOrNull
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.io.IOException

internal fun Url.canonical(): Url = URLBuilder(this).apply {
    host = host.canonicalHost()
    pathSegments = pathSegments
}.build()

internal fun String.canonicalHost(): String = toAsciiHost().lowercase()

internal expect fun String.toAsciiHost(): String

internal fun String?.canonicalUrlOrNull(): Url? =
    this?.takeIf { it.contains("://") }?.toUrlOrNull()?.canonical()

fun String.canonicalUrl(): String = canonicalUrlOrNull()?.toString() ?: this

internal fun String.toCaldavUrl(): Url =
    canonicalUrlOrNull() ?: throw IOException("Invalid URL: $this")
