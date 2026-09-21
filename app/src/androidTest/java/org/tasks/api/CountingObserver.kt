package org.tasks.api

import android.database.ContentObserver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CountingObserver : ContentObserver(null) {
    private val latch = CountDownLatch(1)

    @Volatile
    var selfChange: Boolean = true
        private set

    override fun onChange(selfChange: Boolean) {
        this.selfChange = selfChange
        latch.countDown()
    }

    fun await(): Boolean = latch.await(10, TimeUnit.SECONDS)
}
