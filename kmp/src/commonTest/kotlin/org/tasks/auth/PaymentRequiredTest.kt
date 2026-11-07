package org.tasks.auth

import kotlinx.coroutines.test.runTest
import org.tasks.http.HttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentRequiredTest {
    @Test
    fun aPaymentRequiredResponseMeansPaymentRequired() = runTest {
        assertFailsWith<PaymentRequiredException> {
            paymentRequiredOn402 { throw HttpException(402, "Payment Required") }
        }
    }

    @Test
    fun otherHttpErrorsPassThrough() = runTest {
        val failure = assertFailsWith<HttpException> {
            paymentRequiredOn402 { throw HttpException(500, "Server Error") }
        }

        assertEquals(500, failure.code)
    }
}
