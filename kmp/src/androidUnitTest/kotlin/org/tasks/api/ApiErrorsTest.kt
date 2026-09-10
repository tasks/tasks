package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorsTest {

    @Test
    fun everyFailureAnAgentCanCauseHasItsOwnCategory() {
        assertEquals(
            ApiFailure.NotFound,
            ApiErrors.classify(ApiRowNotFound(TasksContract.Tags.PATH, 7L)),
        )
        assertEquals(
            ApiFailure.ReadOnly,
            ApiErrors.classify(UnsupportedOperationException("'Shared' is read-only")),
        )
        assertEquals(
            ApiFailure.BadRequest,
            ApiErrors.classify(IllegalArgumentException("title is required")),
        )
        assertEquals(
            ApiFailure.Remote,
            ApiErrors.classify(IllegalStateException("server said no")),
        )
        assertEquals(ApiFailure.Unknown, ApiErrors.classify(RuntimeException("boom")))
    }

    @Test
    fun anExplanationCarriesTheDetailAndWhatToDoAboutIt() {
        val readOnly = ApiErrors.explain(UnsupportedOperationException("'Shared' is read-only"))

        assertTrue(readOnly, readOnly.contains("'Shared' is read-only"))
        assertTrue(readOnly, readOnly.contains("rather than retrying"))
    }

    @Test
    fun aMalformedRequestIsNotWorthSendingAgain() {
        val bad = ApiErrors.explain(IllegalArgumentException("title is required"))

        assertTrue(bad, bad.contains("title is required"))
        assertTrue(bad, bad.contains("fail again"))
    }

    @Test
    fun aMissingRowNamesTheCollectionAndTheId() {
        val gone = ApiErrors.explain(ApiRowNotFound(TasksContract.Places.PATH, 9_999L))

        assertTrue(gone, gone.contains("places"))
        assertTrue(gone, gone.contains("9999"))
        assertTrue(gone, gone.contains("Nothing was written"))
    }

    @Test
    fun anUnexpectedFailureStillNamesItself() {
        assertEquals("IllegalAccessError: boom", ApiErrors.explain(IllegalAccessError("boom")))
    }
}
