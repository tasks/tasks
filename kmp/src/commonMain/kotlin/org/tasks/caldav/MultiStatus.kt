package org.tasks.caldav

import at.bitfire.dav4jvm.ktor.MultiStatusItem
import at.bitfire.dav4jvm.ktor.Response
import at.bitfire.dav4jvm.ktor.responses
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

internal suspend fun Flow<MultiStatusItem>.members(): List<Response> {
    val responses = responses().toList()
    val collection = responses.firstOrNull()?.requestedUrl?.canonical() ?: return emptyList()
    return responses
        .map { it.copy(href = it.href.canonical()) }
        .filter { it.href.isMemberOf(collection) }
}

private fun Url.isMemberOf(collection: Url): Boolean =
    protocol == collection.protocol &&
            host == collection.host &&
            port == collection.port &&
            segments.size > collection.segments.size &&
            segments.subList(0, collection.segments.size) == collection.segments
