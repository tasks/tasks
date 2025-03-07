package org.tasks.caldav

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import javax.inject.Inject

@HiltAndroidTest
class CaldavClientTest : InjectingTestCase() {

    @Inject lateinit var clientProvider: CaldavClientProvider

    @Test
    fun dontCrashOnSpaceInUrl(): Unit = runBlocking {
        clientProvider.forUrl("https://example.com/remote.php/a space/", "username", "password")
    }
}