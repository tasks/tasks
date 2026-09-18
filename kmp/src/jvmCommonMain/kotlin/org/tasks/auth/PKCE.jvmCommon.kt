package org.tasks.auth

import java.security.SecureRandom

internal actual fun secureRandomBytes(count: Int): ByteArray = ByteArray(count).also { SecureRandom().nextBytes(it) }
