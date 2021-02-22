package org.tasks.caldav.property

import at.bitfire.dav4jvm.PropertyRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.caldav.property.PropertyUtils.NS_OWNCLOUD
import org.tasks.caldav.property.TestPropertyUtils.toProperty

class OCInviteTest {
    @Before
    fun setUp() {
        PropertyRegistry.register(OCInvite.Factory())
    }

    @Test
    fun emptyInvite() =
            assertTrue("<oc:invite />".toProperty<OCInvite>("oc" to NS_OWNCLOUD).users.isEmpty())

    @Test
    fun userHref() =
            assertEquals("principal:principals/users/testuser", user(SHARED_USER).href)

    @Test
    fun commonName() = assertEquals("testuser", user(SHARED_USER).commonName)

    @Test
    fun access() = assertEquals(OCAccess.READ_WRITE, user(SHARED_USER).access.access)

    @Test
    fun response() = assertEquals(OCUser.INVITE_ACCEPTED, user(SHARED_USER).response)

    private fun user(xml: String) =
            xml.toProperty<OCInvite>("oc" to NS_OWNCLOUD).users.first()

    companion object {
        private val SHARED_USER = """
            <oc:invite>
                <oc:user>
                    <d:href>principal:principals/users/testuser</d:href>
                    <oc:common-name>testuser</oc:common-name>
                    <oc:invite-accepted />
                    <oc:access>
                        <oc:read-write />
                    </oc:access>
                </oc:user>
            </oc:invite>
        """.trimIndent()
    }
}