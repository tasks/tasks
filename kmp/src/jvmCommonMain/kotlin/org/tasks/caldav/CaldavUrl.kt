package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.toUrlOrNull
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import java.net.IDN
import java.net.MalformedURLException

internal fun Url.canonical(): Url = URLBuilder(this).apply {
    host = host.canonicalHost()
    pathSegments = pathSegments
}.build()

internal fun String.canonicalHost(): String =
    try {
        IDN.toASCII(this)
    } catch (_: IllegalArgumentException) {
        this
    }.lowercase()

internal fun String?.canonicalUrlOrNull(): Url? =
    this?.takeIf { it.contains("://") }?.toUrlOrNull()?.canonical()

fun String.canonicalUrl(): String = canonicalUrlOrNull()?.toString() ?: this

internal fun String.toCaldavUrl(): Url =
    canonicalUrlOrNull() ?: throw MalformedURLException("Invalid URL: $this")
