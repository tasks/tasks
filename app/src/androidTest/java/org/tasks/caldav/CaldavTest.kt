package org.tasks.caldav

import com.todoroo.astrid.dao.TaskDao
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.tasks.R
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
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

    @Before
    override fun setUp() {
        super.setUp()

        preferences.setBoolean(R.string.p_debug_pro, true)
        server.start()
    }

    @After
    fun after() = server.shutdown()

    protected fun enqueue(vararg responses: String) {
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

    companion object {
        init {
            CaldavSynchronizer.registerFactories()
        }
    }
}