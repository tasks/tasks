package org.tasks.caldav

import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class CaldavClientTest : InjectingTestCase() {

    @Inject lateinit var clientProvider: CaldavClientProvider

    @Test
    fun dontCrashOnSpaceInUrl(): Unit = runBlocking {
        clientProvider.forUrl("https://example.com/remote.php/a space/", "username", "password")
    }
}