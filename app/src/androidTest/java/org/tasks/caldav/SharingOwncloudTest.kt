package org.tasks.caldav

import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.dao.PrincipalDao
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class SharingOwncloudTest : CaldavTest() {

    @Inject lateinit var principalDao: PrincipalDao

    private suspend fun setupAccount(user: String) {
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = user,
            password = encryption.encrypt("password"),
            url = server.url("/remote.php/dav/calendars/$user/").toString(),
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
    }

    @Test
    fun calendarOwner() = runBlocking {
        setupAccount("user1")
        val calendar = CaldavCalendar(
            account = this@SharingOwncloudTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingOwncloudTest.account.url}test-shared/",
        )
        caldavDao.insert(calendar)
        enqueue(OC_OWNER)

        sync()

        assertEquals(ACCESS_OWNER, caldavDao.getCalendarByUuid(calendar.uuid!!)?.access)
    }

    @Test
    fun readOnly() = runBlocking {
        setupAccount("user2")
        val calendar = CaldavCalendar(
            account = this@SharingOwncloudTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/2",
            url = "${this@SharingOwncloudTest.account.url}test-shared_shared_by_user1/",
        )
        caldavDao.insert(calendar)
        enqueue(OC_READ_ONLY)

        sync()

        assertEquals(ACCESS_READ_ONLY, caldavDao.getCalendarByUuid(calendar.uuid!!)?.access)
    }

    @Test
    fun principalForSharee() = runBlocking {
        setupAccount("user1")
        val calendar = CaldavCalendar(
            account = this@SharingOwncloudTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/1",
            url = "${this@SharingOwncloudTest.account.url}test-shared/",
        )
        caldavDao.insert(calendar)
        enqueue(OC_OWNER)

        sync()

        val principal = principalDao.getAll()
            .apply { assertTrue(size == 1) }
            .first()

        assertEquals(calendar.id, principal.list)
        assertEquals("principal:principals/users/user2", principal.href)
        assertEquals("user2", principal.name)
        assertEquals(CaldavCalendar.INVITE_ACCEPTED, principal.inviteStatus)
        assertEquals(ACCESS_READ_ONLY, principal.access.access)
    }

    @Test
    fun principalForOwner() = runBlocking {
        setupAccount("user2")
        val calendar = CaldavCalendar(
            account = this@SharingOwncloudTest.account.uuid,
            ctag = "http://sabre.io/ns/sync/2",
            url = "${this@SharingOwncloudTest.account.url}test-shared_shared_by_user1/",
        )
        caldavDao.insert(calendar)
        enqueue(OC_READ_ONLY)

        sync()

        val principal = principalDao.getAll()
            .apply { assertTrue(size == 1) }
            .first()

        assertEquals(calendar.id, principal.list)
        assertEquals("principals/users/user1", principal.href)
        assertEquals(null, principal.displayName)
        assertEquals(CaldavCalendar.INVITE_ACCEPTED, principal.inviteStatus)
        assertEquals(ACCESS_OWNER, principal.access.access)
    }

    companion object {
        private val OC_OWNER = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav"
              xmlns:cs="http://calendarserver.org/ns/" xmlns:oc="http://owncloud.org/ns">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype>
                                <d:collection />
                                <cal:calendar />
                            </d:resourcetype>
                            <d:displayname>Test shared</d:displayname>
                            <cal:supported-calendar-component-set>
                                <cal:comp name="VTODO" />
                            </cal:supported-calendar-component-set>
                            <cs:getctag>http://sabre.io/ns/sync/1</cs:getctag>
                            <x1:calendar-color xmlns:x1="http://apple.com/ns/ical/">#0082c9</x1:calendar-color>
                            <d:sync-token>http://sabre.io/ns/sync/1</d:sync-token>
                            <oc:owner-principal>principals/users/user1</oc:owner-principal>
                            <oc:invite>
                                <oc:user>
                                    <d:href>principal:principals/users/user2</d:href>
                                    <oc:common-name>user2</oc:common-name>
                                    <oc:invite-accepted />
                                    <oc:access>
                                        <oc:read />
                                    </oc:access>
                                </oc:user>
                            </oc:invite>
                            <d:current-user-privilege-set>
                                <d:privilege>
                                    <d:write />
                                </d:privilege>
                                <d:privilege>
                                    <d:write-properties />
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
                                    <d:read />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-acl />
                                </d:privilege>
                                <d:privilege>
                                    <d:read-current-user-privilege-set />
                                </d:privilege>
                                <d:privilege>
                                    <cal:read-free-busy />
                                </d:privilege>
                            </d:current-user-privilege-set>
                            <d:current-user-principal>
                                <d:href>/remote.php/dav/principals/users/user1/</d:href>
                            </d:current-user-principal>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <d:share-access />
                            <d:invite />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        val OC_READ_ONLY = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:cal="urn:ietf:params:xml:ns:caldav" xmlns:cs="http://calendarserver.org/ns/"
                xmlns:d="DAV:" xmlns:nc="http://nextcloud.org/ns"
                xmlns:oc="http://owncloud.org/ns" xmlns:s="http://sabredav.org/ns">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user2/test-shared_shared_by_user1/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:resourcetype>
                                <d:collection />
                                <cal:calendar />
                            </d:resourcetype>
                            <d:displayname>Test shared (user1)</d:displayname>
                            <cal:supported-calendar-component-set>
                                <cal:comp name="VTODO" />
                            </cal:supported-calendar-component-set>
                            <cs:getctag>http://sabre.io/ns/sync/2</cs:getctag>
                            <x1:calendar-color xmlns:x1="http://apple.com/ns/ical/">#0082c9</x1:calendar-color>
                            <d:sync-token>http://sabre.io/ns/sync/2</d:sync-token>
                            <oc:owner-principal>principals/users/user1</oc:owner-principal>
                            <oc:invite />
                            <d:current-user-privilege-set>
                                <d:privilege>
                                    <d:write-properties />
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
                                    <cal:read-free-busy />
                                </d:privilege>
                            </d:current-user-privilege-set>
                            <d:current-user-principal>
                                <d:href>/remote.php/dav/principals/users/user2/</d:href>
                            </d:current-user-principal>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <d:share-access />
                            <d:invite />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()
    }
}