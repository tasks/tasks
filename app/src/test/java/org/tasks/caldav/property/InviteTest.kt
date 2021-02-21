package org.tasks.caldav.property

import at.bitfire.dav4jvm.PropertyRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.tasks.caldav.property.PropertyUtils.toProperty
import org.tasks.caldav.property.ShareAccess.Companion.SHARED_OWNER

class InviteTest {
    @Before
    fun setUp() {
        PropertyRegistry.register(listOf(
                ShareAccess.Factory(),
                Invite.Factory()
        ))
    }

    @Test
    fun emptyInvite() {
        val invite: Invite = "<d:invite />".toProperty()

        assertTrue(invite.sharees.isEmpty())
    }

    @Test
    fun shareeAccess() {
        assertEquals(ShareAccess(SHARED_OWNER), sharee(SHARE_OWNER).access)
    }

    @Test
    fun shareeHref() {
        assertEquals("/principals/102967489186752069531", sharee(SHARE_OWNER).href)
    }

    @Test
    fun inviteStatus() {
        assertEquals(Sharee.INVITE_ACCEPTED, sharee(SHARE_OWNER).response)
    }

    @Test
    fun noComment() {
        assertNull(sharee(SHARE_OWNER).comment)
    }

    private fun sharee(xml: String): Sharee = xml.toProperty<Invite>().sharees.first()

    companion object {
        val SHARE_OWNER = """
            <d:invite>
                <d:sharee>
                    <d:href>/principals/102967489186752069531</d:href>
                    <d:prop />
                    <d:share-access>
                        <d:shared-owner />
                    </d:share-access>
                    <d:invite-accepted />
                </d:sharee>
            </d:invite>
        """.trimIndent()
    }
}