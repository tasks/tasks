package org.tasks.billing

import kotlin.test.Test
import kotlin.test.assertNotNull

class UnlockSourceTest {

    @Test
    fun everyProviderThatCoversAnAccountCanBeNamed() {
        EntitlementProvider.entries.forEach { assertNotNull(it.unlockSource, it.name) }
    }
}
