package org.tasks.time

import kotlinx.datetime.Clock

class SystemMillisProvider : MillisProvider {
    override val millis: Long
        get() = Clock.System.now().toEpochMilliseconds()
}
