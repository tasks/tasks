package org.tasks.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tasks.R
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.date_shortcut_hour

@RunWith(RobolectricTestRunner::class)
class RobolectricSmokeTest {
    @Test
    fun androidResourcesAndManifestAreAvailable() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("org.tasks", context.packageName)
        assertTrue(context.getString(R.string.TAd_actionEditTask).isNotBlank())
        assertTrue(context.resources.getQuantityString(R.plurals.task_count, 2, 2).isNotBlank())
    }

    @Test
    fun composeResourcesResolve() = runBlocking {
        assertTrue(getString(Res.string.date_shortcut_hour).isNotBlank())
    }
}
