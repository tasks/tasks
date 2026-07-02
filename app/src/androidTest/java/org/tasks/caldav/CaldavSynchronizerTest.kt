package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import org.tasks.data.UUIDHelper
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.TagData
import org.tasks.data.entity.CaldavCalendar
import org.tasks.makers.CaldavTaskMaker.CALENDAR
import org.tasks.makers.CaldavTaskMaker.ETAG
import org.tasks.makers.CaldavTaskMaker.OBJECT
import org.tasks.makers.CaldavTaskMaker.TASK
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@HiltAndroidTest
class CaldavSynchronizerTest : CaldavTest() {

    @Inject lateinit var dirtyDao: DirtyDao
    @Inject lateinit var tagDao: TagDao
    @Inject lateinit var tagDataDao: TagDataDao

    @Before
    override fun setUp() = runBlocking {
        super.setUp()
        account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            username = "username",
            password = encryption.encrypt("password"),
            url = server.url("/remote.php/dav/calendars/user1/").toString(),
        ).let {
            it.copy(id = caldavDao.insert(it))
        }
    }

    @Test
    fun setMessageOnError() = runBlocking {
        enqueue()

        synchronizer.sync(account, hasPro = true)

        assertEquals("HTTP 500 Server Error", caldavDao.getAccounts().first().error)
    }

    @Test
    fun dontFetchCalendarIfCtagMatches() = runBlocking {
        caldavDao.insert(
            CaldavCalendar(
                account = this@CaldavSynchronizerTest.account.uuid,
                ctag = "http://sabre.io/ns/sync/1",
                url = "${this@CaldavSynchronizerTest.account.url}test-shared/",
            )
        )
        enqueue(OC_SHARE_PROPFIND)

        sync()
    }

    @Test
    fun dontFetchTaskIfEtagMatches() = runBlocking {
        val calendar = CaldavCalendar(
            account = this@CaldavSynchronizerTest.account.uuid,
            uuid = UUIDHelper.newUUID(),
            url = "${this@CaldavSynchronizerTest.account.url}test-shared/",
        )
        caldavDao.insert(calendar)
        val caldavTaskId = caldavDao.insert(newCaldavTask(
            with(TASK, taskDao.insert(newTask())),
            with(OBJECT, "3164728546640386952.ics"),
            with(ETAG, "43b3ffaac5131880e4dd07a79adba82a"),
            with(CALENDAR, calendar.uuid)
        ))
        dirtyDao.markSynced(caldavTaskId)
        enqueue(OC_SHARE_PROPFIND, OC_SHARE_REPORT)

        sync()
    }

    @Test
    fun syncNewTask() = runBlocking {
        enqueue(OC_SHARE_PROPFIND, OC_SHARE_REPORT, OC_SHARE_TASK)

        sync()

        val calendar = caldavDao.getCalendars().takeIf { it.size == 1 }!!.first()
        val caldavTask = caldavDao.getTaskByRemoteId(calendar.uuid!!, "3164728546640386952")!!
        assertEquals("Test task", taskDao.fetch(caldavTask.task)!!.title)
    }

    @Test
    fun addRemoteTagToExistingTask() = runBlocking {
        enqueueRemoteTagUpdate(initial = "Tag1", updated = "Tag1,Tag2")

        sync()
        sync()

        assertTags("Tag1", "Tag2")
    }

    @Test
    fun removeRemoteTagFromExistingTask() = runBlocking {
        enqueueRemoteTagUpdate(initial = "Tag1,Tag2", updated = "Tag1")

        sync()
        sync()

        assertTags("Tag1")
    }

    @Test
    fun replaceRemoteTagOnExistingTask() = runBlocking {
        enqueueRemoteTagUpdate(initial = "Tag1", updated = "Tag3")

        sync()
        sync()

        assertTags("Tag3")
    }

    @Test
    fun preserveLocalTagChanges() = runBlocking {
        // Trailing "" is the 207 for the PUT that pushes the local tag change on the second sync
        enqueueRemoteTagUpdate(initial = "Tag1", updated = "Tag3", "")

        sync()

        val caldavTask = caldavDao.getTaskByRemoteId(
            caldavDao.getCalendars().first().uuid!!, TASK_UID
        )!!
        val task = taskDao.fetch(caldavTask.task)!!
        val tag2 = TagData(name = "Tag2").let { it.copy(id = tagDataDao.insert(it)) }
        tagDao.applyTags(task, listOf(tag2))
        dirtyDao.setDirty(listOf(task.id))

        sync()

        assertTags("Tag2")
    }

    private fun enqueueRemoteTagUpdate(initial: String, updated: String, vararg additional: String) {
        enqueueAll(
            OC_SHARE_PROPFIND, REPORT_ETAG1, task("etag1", initial),
            propfind("http://sabre.io/ns/sync/2"), REPORT_ETAG2, task("etag2", updated),
            *additional
        )
    }

    private suspend fun assertTags(vararg expectedTags: String) {
        val calendar = caldavDao.getCalendars().first()
        val task = caldavDao.getTaskByRemoteId(calendar.uuid!!, TASK_UID)!!
        assertEquals(
            expectedTags.toSet(),
            tagDao.getTagsForTask(task.task).mapNotNull { it.name }.toSet()
        )
    }

    private fun enqueueAll(vararg responses: String) {
        responses.forEach {
            server.enqueue(
                MockResponse()
                    .setResponseCode(207)
                    .setHeader("Content-Type", "text/xml; charset=\"utf-8\"")
                    .setBody(it)
            )
        }
        server.enqueue(MockResponse().setResponseCode(500))
    }

    private fun propfind(ctag: String) =
        OC_SHARE_PROPFIND
            .replace("http://sabre.io/ns/sync/1", ctag)

    companion object {
        private const val TASK_UID = "3164728546640386952"

        private val OC_SHARE_PROPFIND = """
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

        private val OC_SHARE_REPORT = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/3164728546640386952.ics</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>&quot;43b3ffaac5131880e4dd07a79adba82a&quot;</d:getetag>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private val OC_SHARE_TASK = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/3164728546640386952.ics</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getcontenttype>text/calendar; charset=utf-8; component=vtodo</d:getcontenttype>
                            <d:getetag>&quot;43b3ffaac5131880e4dd07a79adba82a&quot;</d:getetag>
                            <cal:calendar-data>BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110500//EN
            BEGIN:VTODO
            DTSTAMP:20210223T154147Z
            UID:3164728546640386952
            CREATED:20210223T154134Z
            LAST-MODIFIED:20210223T154140Z
            SUMMARY:Test task
            PRIORITY:9
            END:VTODO
            END:VCALENDAR</cal:calendar-data>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <cal:schedule-tag />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private val REPORT_ETAG1 = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/3164728546640386952.ics</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>&quot;etag1&quot;</d:getetag>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private val REPORT_ETAG2 = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/3164728546640386952.ics</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getetag>&quot;etag2&quot;</d:getetag>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

        private fun task(etag: String, categories: String) = """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
                <d:response>
                    <d:href>/remote.php/dav/calendars/user1/test-shared/3164728546640386952.ics</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getcontenttype>text/calendar; charset=utf-8; component=vtodo</d:getcontenttype>
                            <d:getetag>&quot;$etag&quot;</d:getetag>
                            <cal:calendar-data>BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:+//IDN tasks.org//android-110500//EN
            BEGIN:VTODO
            DTSTAMP:20210223T154147Z
            UID:3164728546640386952
            CREATED:20210223T154134Z
            LAST-MODIFIED:20210223T154140Z
            SUMMARY:Test task
            PRIORITY:9
            CATEGORIES:$categories
            END:VTODO
            END:VCALENDAR</cal:calendar-data>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                    <d:propstat>
                        <d:prop>
                            <cal:schedule-tag />
                        </d:prop>
                        <d:status>HTTP/1.1 404 Not Found</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.trimIndent()

    }
}