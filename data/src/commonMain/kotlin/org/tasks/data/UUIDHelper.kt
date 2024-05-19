package org.tasks.data

import java.util.UUID

object UUIDHelper {
    private const val MIN_UUID: Long = 100000000

    fun newUUID(): String {
        var uuid: Long
        do {
            uuid = UUID.randomUUID().leastSignificantBits and 0x7fffffffffffffffL
        } while (uuid < MIN_UUID)
        return uuid.toString()
    }
}
