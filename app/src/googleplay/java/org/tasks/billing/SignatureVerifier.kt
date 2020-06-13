package org.tasks.billing

import android.content.Context
import org.tasks.R
import org.tasks.injection.ApplicationContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

internal class SignatureVerifier @Inject constructor(@ApplicationContext context: Context) {
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