package org.tasks.caldav.property

import at.bitfire.dav4jvm.PropertyRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.caldav.property.TestPropertyUtils.toProperty

class OCOwnerPrincipalTest {
    @Before
    fun setUp() {
        PropertyRegistry.register(OCOwnerPrincipal.Factory())
    }

    @Test
    fun ownerPrincipal() {
        val owner = "<oc:owner-principal>principals/users/test</oc:owner-principal>"
                .toProperty<OCOwnerPrincipal>("oc='${PropertyUtils.NS_OWNCLOUD}'")

        assertEquals("principals/users/test", owner.owner)
    }
}