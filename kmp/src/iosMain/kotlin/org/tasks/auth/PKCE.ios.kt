package org.tasks.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.arc4random_buf

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(count: Int): ByteArray =
    ByteArray(count).also { bytes -> bytes.usePinned { arc4random_buf(it.addressOf(0), count.toULong()) } }
