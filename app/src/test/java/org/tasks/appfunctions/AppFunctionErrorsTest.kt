package org.tasks.appfunctions

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeniedException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.tasks.api.ApiErrors
import org.tasks.api.ApiRowNotFound
import org.tasks.api.TasksContract.Places

@RunWith(RobolectricTestRunner::class)
class AppFunctionErrorsTest {

    @Test
    fun aMalformedCallIsTheCallersFaultRatherThanTheApps() {
        val error = IllegalArgumentException("title is required")

        val mapped = error.toAppFunctionException()

        assertTrue(mapped.toString(), mapped is AppFunctionInvalidArgumentException)
        assertEquals(ApiErrors.explain(error), mapped.message)
    }

    @Test
    fun aReadOnlyListIsRefusedRatherThanReportedAsBroken() {
        val mapped = UnsupportedOperationException("'Shared' is read-only").toAppFunctionException()

        assertTrue(mapped.toString(), mapped is AppFunctionDeniedException)
        assertTrue(mapped.message.orEmpty().contains("read-only"))
    }

    @Test
    fun aRowThatIsNotThereIsNotFound() {
        val mapped = ApiRowNotFound(Places.PATH, 9_999L).toAppFunctionException()

        assertTrue(mapped.toString(), mapped is AppFunctionElementNotFoundException)
    }

    @Test
    fun aSyncFailureAndAnUnexpectedOneBothLandOnTheApp() {
        assertTrue(IllegalStateException("server said no").toAppFunctionException() is AppFunctionAppUnknownException)
        assertTrue(RuntimeException("boom").toAppFunctionException() is AppFunctionAppUnknownException)
    }
}
