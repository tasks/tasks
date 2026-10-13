package org.tasks.caldav

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.tasks.DatabaseTest
import org.tasks.InMemoryDataStore
import org.tasks.analytics.AnalyticsEvents.SYNC_NOT_FOUND
import org.tasks.analytics.Reporting
import org.tasks.data.entity.CaldavAccount
import org.tasks.preferences.TasksPreferences

class CaldavSyncErrorTest : DatabaseTest() {
    private val server = failFastServer()
    private val caldavDao = db.caldavDao()
    private val preferences = TasksPreferences(InMemoryDataStore())
    private val encryption = testEncryption()
    private val reporting = mock<Reporting>()
    private val synchronizer = CaldavSynchronizer(
        caldavDao = caldavDao,
        dirtyDao = db.dirtyDao(),
        refreshBroadcaster = mock(),
        taskDeleter = mock(),
        reporting = reporting,
        provider = testClientProvider(preferences, encryption),
        iCal = mock(),
        principalDao = db.principalDao(),
        vtodoCache = mock(),
        accountDataRepository = mock(),
        tagMetadataSync = mock { on { isPrimary(any()) } doReturn false },
    )
    private lateinit var account: CaldavAccount

    @Before
    fun setUp() = runBlocking {
        server.start()
        account = CaldavAccount(
            uuid = "account",
            username = "user",
            password = encryption.encrypt("password"),
            url = server.url("/dav/").toString(),
        ).let { it.copy(id = caldavDao.insert(it)) }
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `a propfind the server refuses is shown but not reported`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(405))

        synchronizer.sync(account, hasPro = true)

        assertEquals("HTTP 405 - Method Not Allowed", caldavDao.getAccountByUuid("account")!!.error)
        verify(reporting, never()).reportException(any(), any())
    }

    @Test
    fun `a request the server rejects as malformed is reported`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))

        synchronizer.sync(account, hasPro = true)

        assertEquals("HTTP 400 - Bad Request", caldavDao.getAccountByUuid("account")!!.error)
        verify(reporting).reportException(any(), any())
    }

    @Test
    fun `a home set the server no longer has is logged but not reported`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        synchronizer.sync(account, hasPro = true)

        assertEquals("HTTP 404 - Not Found", caldavDao.getAccountByUuid("account")!!.error)
        verify(reporting, never()).reportException(any(), any())
        verifyBlocking(reporting) { logEventOncePerDay(eq(SYNC_NOT_FOUND), eq("account"), anyVararg()) }
    }

    @Test
    fun `an unnamed status still names the code`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(499))

        synchronizer.sync(account, hasPro = true)

        assertEquals("HTTP 499", caldavDao.getAccountByUuid("account")!!.error)
    }

    @Test
    fun `payment and terms statuses keep driving their screens`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(402))

        synchronizer.sync(account, hasPro = true)

        assertTrue(caldavDao.getAccountByUuid("account")!!.isPaymentRequired())

        server.enqueue(MockResponse().setResponseCode(451))

        synchronizer.sync(account, hasPro = true)

        assertTrue(caldavDao.getAccountByUuid("account")!!.isTosRequired())
    }
}
