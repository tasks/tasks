package org.tasks.auth

import org.tasks.http.HttpException

class PaymentRequiredException : Exception("Payment required")

suspend fun <T> paymentRequiredOn402(block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        throw if (e.code == 402) PaymentRequiredException() else e
    }
