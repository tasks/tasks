package org.tasks.reminders

import java.util.Random

open class Random {
    open fun nextFloat(seed: Long): Float {
        random.setSeed(seed)
        return random.nextFloat()
    }

    companion object {
        private val random = Random()
    }
}
