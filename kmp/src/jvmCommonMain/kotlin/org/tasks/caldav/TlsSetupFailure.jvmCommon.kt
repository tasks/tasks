package org.tasks.caldav

import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

internal actual fun Throwable.isTlsSetupFailure(): Boolean =
    this is KeyManagementException || this is NoSuchAlgorithmException
