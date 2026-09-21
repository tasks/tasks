package org.tasks.api

import org.junit.Assert.fail

inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
    try {
        block()
    } catch (t: Throwable) {
        if (t is T) {
            return t
        }
        throw AssertionError("Expected ${T::class.java.simpleName} but was ${t::class.java.name}: ${t.message}", t)
    }
    fail("Expected ${T::class.java.simpleName}")
    error("unreachable")
}
