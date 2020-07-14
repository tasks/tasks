package org.tasks.caldav

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.DebugNetworkInterceptor
import org.tasks.TestUtilities.newPreferences
import org.tasks.security.KeyStoreEncryption

@RunWith(AndroidJUnit4::class)
class CaldavClientTest {
    @Test
    fun dontCrashOnSpaceInUrl() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            CaldavClient(context, KeyStoreEncryption(), newPreferences(context), DebugNetworkInterceptor(ApplicationProvider.getApplicationContext()))
                    .forUrl("https://example.com/remote.php/a space/", "username", "password")
        }
    }
}