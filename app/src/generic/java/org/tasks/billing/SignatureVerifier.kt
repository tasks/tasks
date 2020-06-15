package org.tasks.billing

import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class SignatureVerifier @Inject constructor() {
    fun verifySignature(purchase: org.tasks.billing.Purchase): Boolean {
        return true
    }
}