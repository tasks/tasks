package org.tasks.billing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class SignatureVerifier @Inject constructor(@ApplicationContext context: Context) {
    private val billingKey: String = context.getString(R.string.gp_key)

    fun verifySignature(purchase: Purchase): Boolean {
        return try {
            Security.verifyPurchase(
                    billingKey, purchase.originalJson, purchase.signature)
        } catch (e: IOException) {
            Timber.e(e)
            false
        }
    }
}