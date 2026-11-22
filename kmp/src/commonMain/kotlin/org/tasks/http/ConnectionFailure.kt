package org.tasks.http

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

fun Throwable.isConnectionFailure(): Boolean = causes().any { it.failedToConnect() }

fun Throwable.isReadTimeout(): Boolean = causes().any { it is SocketTimeoutException }

private fun Throwable.causes(): Sequence<Throwable> = generateSequence(this) { it.cause }

private fun Throwable.failedToConnect(): Boolean =
    this is ConnectionException ||
        this is HttpRequestTimeoutException ||
        this is ConnectTimeoutException ||
        (this is Exception && translated() is ConnectionException) ||
        isUnreachableHost()

internal expect fun Throwable.isUnreachableHost(): Boolean
