package org.tasks.http

import java.net.NoRouteToHostException
import java.net.UnknownHostException

internal actual fun Throwable.isUnreachableHost(): Boolean =
    this is UnknownHostException || this is NoRouteToHostException
