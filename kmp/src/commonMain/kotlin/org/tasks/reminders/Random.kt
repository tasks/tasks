package org.tasks.reminders

import kotlin.random.Random as KRandom

open class Random {
    open fun nextFloat(seed: Long): Float {
        return KRandom(seed).nextFloat()
    }
}
