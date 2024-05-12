package org.tasks.caldav

import com.todoroo.astrid.dao.TaskDao
import junit.framework.Assert.assertFalse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.Timeout
import org.tasks.R
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.dao.CaldavDao
import org.tasks.injection.InjectingTestCase
import org.tasks.preferences.Preferences
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

abstract class CaldavTest : InjectingTestCase() {
    @Inject lateinit var synchronizer: CaldavSynchronizer
    @Inject lateinit var encryption: KeyStoreEncryption
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var taskDao: TaskDao
    protected val server = MockWebServer()
    protected lateinit var account: CaldavAccount

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

    @Before
    override fun setUp() {
        super.setUp()

        preferences.setBoolean(R.string.p_debug_pro, true)
        server.start()
        headers.clear()
    }

    @After
    fun after() = server.shutdown()

    protected suspend fun sync(account: CaldavAccount = this.account) {
        synchronizer.sync(account)

        assertFalse(caldavDao.getAccountByUuid(account.uuid!!)!!.hasError)
    }

    val headers = HashMap<String, String>()

    protected fun enqueue(vararg responses: String) {
        responses.forEach {
            server.enqueue(
                MockResponse()
                    .setResponseCode(207)
                    .setHeader("Content-Type", "text/xml; charset=\"utf-8\"")
                    .apply { this@CaldavTest.headers.forEach { (k, v) -> setHeader(k, v) } }
                    .setBody(it))
        }
        server.enqueue(MockResponse().setResponseCode(500))
    }

    companion object {
        init {
            CaldavSynchronizer.registerFactories()
        }
    }
}