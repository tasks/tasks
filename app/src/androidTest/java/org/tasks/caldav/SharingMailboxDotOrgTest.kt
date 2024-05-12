package org.tasks.caldav

import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.dao.PrincipalDao
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class SharingMailboxDotOrgTest : CaldavTest() {

    @Inject lateinit var principalDao: PrincipalDao

    @Test
    fun ownerAccess() = runBlocking {
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = "3",
            password = encryption.encrypt("password"),
            url = server.url("/caldav/").toString(),
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
        val calendar = CaldavCalendar(
            account = this@SharingMailboxDotOrgTest.account.uuid,
            ctag = "1614876450015",
            url = "${this@SharingMailboxDotOrgTest.account.url}MzM/",
        )
        caldavDao.insert(calendar)
        enqueue(SHARE_OWNER)

        sync()

        // TODO: mailbox.org uses share-access differently, need to figure out how to set owner
        assertEquals(ACCESS_READ_WRITE, caldavDao.getCalendar(calendar.uuid!!)!!.access)
    }

    @Test
    fun principalForSharee() = runBlocking {
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = "3",
            password = encryption.encrypt("password"),
            url = server.url("/caldav/").toString(),
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
        val calendar = CaldavCalendar(
            account = this@SharingMailboxDotOrgTest.account.uuid,
            ctag = "1614876450015",
            url = "${this@SharingMailboxDotOrgTest.account.url}MzM/",
        )
        caldavDao.insert(calendar)
        enqueue(SHARE_OWNER)

        sync()

        val principal = principalDao.getAll().first()

        assertEquals(calendar.id, principal.list)
        assertEquals("/principals/users/5", principal.href)
        assertNull(principal.displayName)
        assertEquals(CaldavCalendar.INVITE_ACCEPTED, principal.inviteStatus)
        assertEquals(CaldavCalendar.ACCESS_UNKNOWN, principal.access.access)
    }

    companion object {
        private val SHARE_OWNER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <D:multistatus xmlns:APPLE="http://apple.com/ns/ical/" xmlns:CAL="urn:ietf:params:xml:ns:caldav"
                xmlns:CS="http://calendarserver.org/ns/" xmlns:D="DAV:">
                <D:response>
                    <D:href>/caldav/MzM/</D:href>
                    <D:propstat>
                        <D:prop>
                            <D:current-user-privilege-set>
                                <D:privilege>
                                    <D:read-acl />
                                </D:privilege>
                                <D:privilege>
                                    <D:read-current-user-privilege-set />
                                </D:privilege>
                                <D:privilege>
                                    <D:read />
                                </D:privilege>
                                <D:privilege>
                                    <D:write />
                                </D:privilege>
                                <D:privilege>
                                    <D:write-content />
                                </D:privilege>
                                <D:privilege>
                                    <D:write-properties />
                                </D:privilege>
                                <D:privilege>
                                    <D:write-acl />
                                </D:privilege>
                                <D:privilege>
                                    <D:bind />
                                </D:privilege>
                                <D:privilege>
                                    <D:unbind />
                                </D:privilege>
                            </D:current-user-privilege-set>
                            <D:displayname>Tasks</D:displayname>
                            <D:current-user-principal>
                                <D:href>/principals/users/3</D:href>
                            </D:current-user-principal>
                            <calendar-color symbolic-color="custom" xmlns="http://apple.com/ns/ical/">
                                #CEE7FFFF
                            </calendar-color>
                            <D:invite>
                                <D:sharee>
                                    <D:href>/principals/users/5</D:href>
                                    <D:invite-accepted />
                                    <D:share-access>read</D:share-access>
                                </D:sharee>
                            </D:invite>
                            <D:sync-token>1614876450015</D:sync-token>
                            <D:share-access>shared-owner</D:share-access>
                            <D:resourcetype>
                                <D:collection />
                                <CAL:calendar />
                            </D:resourcetype>
                            <supported-calendar-component-set xmlns="urn:ietf:params:xml:ns:caldav">
                                <CAL:comp name="VTODO" />
                            </supported-calendar-component-set>
                            <getctag xmlns="http://calendarserver.org/ns/">33-1614876450015</getctag>
                        </D:prop>
                        <D:status>HTTP/1.1 200 OK</D:status>
                    </D:propstat>
                    <D:propstat>
                        <D:prop>
                            <invite xmlns="http://owncloud.org/ns" />
                            <owner-principal xmlns="http://owncloud.org/ns" />
                        </D:prop>
                        <D:status>HTTP/1.1 404 NOT FOUND</D:status>
                    </D:propstat>
                </D:response>
            </D:multistatus>
        """.trimIndent()
    }
}