package org.tasks.time

class SystemMillisProvider : MillisProvider {
    override val millis: Long
        get() = System.currentTimeMillis()
}
