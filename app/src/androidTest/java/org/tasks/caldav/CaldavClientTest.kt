package org.tasks.caldav

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.TestUtilities.newPreferences
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

@RunWith(AndroidJUnit4::class)
class CaldavClientTest {
    @Test
    fun dontCrashOnSpaceInUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CaldavClient(context, null, newPreferences(context), null)
                .forUrl("https://example.com/remote.php/a space/", "username", "password")
    }
}