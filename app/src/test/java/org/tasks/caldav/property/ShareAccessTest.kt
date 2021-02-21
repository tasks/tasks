package org.tasks.caldav.property

import at.bitfire.dav4jvm.PropertyRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.caldav.property.PropertyUtils.toProperty
import org.tasks.caldav.property.ShareAccess.Companion.SHARED_OWNER

class ShareAccessTest {
    @Before
    fun setUp() {
        PropertyRegistry.register(ShareAccess.Factory())
    }

    @Test
    fun parseShareAccess() {
        val access: ShareAccess = """
            <d:share-access>
                    <d:shared-owner />
            </d:share-access>
        """.toProperty()

        assertEquals(ShareAccess(SHARED_OWNER), access)
    }
}