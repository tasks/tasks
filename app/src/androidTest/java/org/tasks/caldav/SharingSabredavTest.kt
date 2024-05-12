package org.tasks.caldav

import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.INVITE_ACCEPTED
import org.tasks.data.dao.PrincipalDao
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class SharingSabredavTest : CaldavTest() {

    @Inject lateinit var principalDao: PrincipalDao

    private suspend fun setupAccount(user: String) {
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = user,
            password = encryption.encrypt("password"),
            url = server.url("/calendars/$user/").toString(),
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
    }

    @Test
    fun calendarOwner() = runBlocking {
        setupAccount("user1")
        val calendar = CaldavCalendar(
            account = this@SharingSabredavTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingSabredavTest.account.url}940468858232147861/",
        )
        caldavDao.insert(calendar)
        enqueue(SD_OWNER)

        sync()

        assertEquals(
            ACCESS_OWNER,
            caldavDao.getCalendarByUuid(calendar.uuid!!)?.access
        )
    }

    @Test
    fun calendarSharee() = runBlocking {
        setupAccount("user2")
        val calendar = CaldavCalendar(
            account = this@SharingSabredavTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingSabredavTest.account.url}c3853d69-cb7a-476c-a23b-30ffd70f110b/",
        )
        caldavDao.insert(calendar)
        enqueue(SD_SHAREE)

        sync()

        assertEquals(
            ACCESS_READ_WRITE,
            caldavDao.getCalendarByUuid(calendar.uuid!!)?.access
        )
    }

    @Test
    fun excludeCurrentUserPrincipalFromSharees() = runBlocking {
        setupAccount("user1")
        caldavDao.insert(
            CaldavCalendar(
                account = account.uuid,
                ctag = "http://sabre.io/ns/sync/1",
                url = "${account.url}940468858232147861/",
            )
        )
        enqueue(SD_OWNER)

        sync()

        assertEquals(1, principalDao.getAll().size)
    }

    @Test
    fun principalForSharee() = runBlocking {
        setupAccount("user1")
        val calendar = CaldavCalendar(
            account = this@SharingSabredavTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingSabredavTest.account.url}940468858232147861/",
        )
        caldavDao.insert(calendar)
        enqueue(SD_OWNER)

        sync()

        val principal = principalDao.getAll().first()

        assertEquals(calendar.id, principal.list)
        assertEquals("mailto:user@example.com", principal.href)
        assertEquals("Example User", principal.displayName)
        assertEquals(INVITE_ACCEPTED, principal.inviteStatus)
        assertEquals(ACCESS_READ_WRITE, principal.access.access)
    }

    @Test
    fun principalForOwner() = runBlocking {
        setupAccount("user2")
        val calendar = CaldavCalendar(
            account = this@SharingSabredavTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingSabredavTest.account.url}c3853d69-cb7a-476c-a23b-30ffd70f110b/",
        )
        caldavDao.insert(calendar)
        enqueue(SD_SHAREE)

        sync()

        val principal = principalDao.getAll()
            .apply { assertTrue(size == 1) }
            .first()

        assertEquals(calendar.id, principal.list)
        assertEquals("/principals/user1", principal.href)
        assertEquals(null, principal.displayName)
        assertEquals(INVITE_ACCEPTED, principal.inviteStatus)
        assertEquals(ACCESS_OWNER, principal.access.access)
    }

    companion object {
        private val SD_OWNER = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/"
                xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns">
                <d:response>
                    <d:href>/calendars/user1/940468858232147861/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype>
                                <d:collection />
                                <cal:calendar />
                                <cs:shared-owner />
                            </d:resourcetype>
                            <d:displayname>Shared</d:displayname>
                            <cal:supported-calendar-component-set>
                                <cal:comp name="VTODO" />
                            </cal:supported-calendar-component-set>
                            <cs:getctag>http://sabre.io/ns/sync/1</cs:getctag>
                            <d:sync-token>http://sabre.io/ns/sync/1</d:sync-token>
                            <d:share-access>
                                <d:shared-owner />
                            </d:share-access>
                            <d:invite>
                                <d:sharee>
                                    <d:href>/principals/user1</d:href>
                                    <d:prop />
                                    <d:share-access>
                                        <d:shared-owner />
                                    </d:share-access>
                                    <d:invite-accepted />
                                </d:sharee>
                                <d:sharee>
                                    <d:href>mailto:user@example.com</d:href>
                                    <d:prop>
                                        <d:displayname>Example User</d:displayname>
                                    </d:prop>
                                    <d:share-access>
                                        <d:read-write />
                                    </d:share-access>
                                    <d:invite-accepted />
                                </d:sharee>
                            </d:invite>
                            <d:current-user-privilege-set>
                                <d:privilege>
                                    <cal:read-free-busy />
                                </d:privilege>
                                <d:privilege>
                                    <d:read />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-acl />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-current-user-privilege-set />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-properties />
                                </d:privilege>
                                <d:privilege>
                                    <d:write />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-content />
                                </d:privilege>
                                <d:privilege>
                                    <d:unlock />
                                </d:privilege>
                                <d:privilege>
                                    <d:bind />
                                </d:privilege>
                                <d:privilege>
                                    <d:unbind />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-acl />
                                </d:privilege>
                                <d:privilege>
                                    <d:share />
                                </d:privilege>
                            </d:current-user-privilege-set>
                            <d:current-user-principal>
                                <d:href>/principals/user1/</d:href>
                            </d:current-user-principal>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <x1:calendar-color xmlns:x1="http://apple.com/ns/ical/" />
                            <x2:owner-principal xmlns:x2="http://owncloud.org/ns" />
                            <x2:invite xmlns:x2="http://owncloud.org/ns" />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private val SD_SHAREE = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/"
                xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns">
                <d:response>
                    <d:href>/calendars/user2/c3853d69-cb7a-476c-a23b-30ffd70f110b/
                    </d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype>
                                <d:collection />
                                <cal:calendar />
                            </d:resourcetype>
                            <d:displayname>Shared</d:displayname>
                            <cal:supported-calendar-component-set>
                                <cal:comp name="VTODO" />
                            </cal:supported-calendar-component-set>
                            <cs:getctag>http://sabre.io/ns/sync/1</cs:getctag>
                            <d:sync-token>http://sabre.io/ns/sync/1</d:sync-token>
                            <d:share-access>
                                <d:read-write />
                            </d:share-access>
                            <d:invite>
                                <d:sharee>
                                    <d:href>/principals/user1</d:href>
                                    <d:prop />
                                    <d:share-access>
                                        <d:shared-owner />
                                    </d:share-access>
                                    <d:invite-accepted />
                                </d:sharee>
                            </d:invite>
                            <d:current-user-privilege-set>
                                <d:privilege>
                                    <cal:read-free-busy />
                                </d:privilege>
                                <d:privilege>
                                    <d:read />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-acl />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-current-user-privilege-set />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-properties />
                                </d:privilege>
                                <d:privilege>
                                    <d:write />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-content />
                                </d:privilege>
                                <d:privilege>
                                    <d:unlock />
                                </d:privilege>
                                <d:privilege>
                                    <d:bind />
                                </d:privilege>
                                <d:privilege>
                                    <d:unbind />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-acl />
                                </d:privilege>
                            </d:current-user-privilege-set>
                            <d:current-user-principal>
                                <d:href>/principals/user2/</d:href>
                            </d:current-user-principal>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <x1:calendar-color xmlns:x1="http://apple.com/ns/ical/" />
                            <x2:owner-principal xmlns:x2="http://owncloud.org/ns" />
                            <x2:invite xmlns:x2="http://owncloud.org/ns" />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()
    }
}