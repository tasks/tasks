/*
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tasks.billing

import android.util.Base64
import co.touchlab.kermit.Logger
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

private const val KEY_FACTORY_ALGORITHM = "RSA"
private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

class PlaySignatureVerifier(
    private val billingKey: String = PLAY_PUBLIC_KEY,
) : PlayPurchaseVerifier {
    private val logger = Logger.withTag("PlaySignatureVerifier")

    private val publicKey: PublicKey? by lazy {
        try {
            KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
                .generatePublic(X509EncodedKeySpec(Base64.decode(billingKey, Base64.DEFAULT)))
        } catch (e: InvalidKeySpecException) {
            logger.e(e) { "Invalid billing key" }
            null
        } catch (e: IllegalArgumentException) {
            logger.e(e) { "Invalid billing key" }
            null
        }
    }

    override fun verify(purchase: PlayPurchase): Boolean {
        if (purchase.originalJson.isEmpty() || purchase.signature.isEmpty() || billingKey.isEmpty()) {
            logger.w { "Purchase verification failed: missing data" }
            return false
        }
        val key = publicKey ?: return false
        val signatureBytes = try {
            Base64.decode(purchase.signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            logger.w { "Base64 decoding failed" }
            return false
        }
        return try {
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(key)
                update(purchase.originalJson.toByteArray())
                verify(signatureBytes).also {
                    if (!it) logger.w { "Signature verification failed for ${purchase.sku}" }
                }
            }
        } catch (e: InvalidKeyException) {
            logger.w { "Invalid key specification" }
            false
        } catch (e: SignatureException) {
            logger.w { "Signature exception" }
            false
        }
    }

    companion object {
        const val PLAY_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk8mXRE3dDXwtinUILCEzKjov2rxs3kZbLRzNrcjFWXpG9OEsUzRGLzqEN+WwibVuMRpZLj/+IxbU2sJWq/M0q+90rOhmXn46ZPeNyr77IqX2pWKIAWpzBoWq/mshRwtm9m1FIiGdBNlXrhSE7u3TGB5FuEuuSqKWvWzxeqQ7fHmlM04Lqrh1mN3FaMne8rWv+DWVHDbLrtnXBuC36glOAj17HxrzaE2v6Pv7Df3QefJ3rM1+0fAp/5jNInaP0qHAlG8WTbUmDShQ5kG3urbv3HLByyx6TSqhmNudXUK/6TusvIj50OptAG7x+UFYf956dD3diXhY3yoICvyFWx1sNwIDAQAB"
    }
}
