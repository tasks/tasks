package org.tasks.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object UUIDHelper {
    private const val MIN_UUID: Long = 100000000

    @OptIn(ExperimentalUuidApi::class)
    fun newUUID(): String {
        var uuid: Long
        do {
            uuid = Uuid.random().toLongs { most, least -> least }.and(0x7fffffffffffffffL)
        } while (uuid < MIN_UUID)
        return uuid.toString()
    }
}
