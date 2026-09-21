package org.tasks.time

internal fun monotonicMillis(): Long = System.nanoTime() / 1_000_000
