/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.cert4android

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class CustomCertStore internal constructor(
    private val context: Context,
    private val userTimeout: Long = 60000L
): CertStore {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    /** system default TrustStore */
    private val systemKeyStore by lazy {
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(null as KeyStore?) // null means "use system default trust store"
            trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull()
                ?: throw IllegalStateException("No X509TrustManager found")
        }
    }

    /** custom TrustStore */
    @VisibleForTesting
    internal val userKeyStore = KeyStore.getInstance(KeyStore.getDefaultType())!!
    private val userKeyStoreFile = File(context.getDir(KEYSTORE_DIR, Context.MODE_PRIVATE), KEYSTORE_NAME)

    /** in-memory store for untrusted certs */
    @VisibleForTesting
    internal var untrustedCerts = HashSet<X509Certificate>()

    init {
        loadUserKeyStore()
    }

    @Synchronized
    override fun clearUserDecisions() {
        logger.info("Clearing user-(dis)trusted certificates")

        for (alias in userKeyStore.aliases())
            userKeyStore.deleteEntry(alias)
        saveUserKeyStore()

        // clear untrusted certs
        untrustedCerts.clear()
    }

    /**
     * Determines whether a certificate chain is trusted.
     */
    override fun isTrusted(chain: Array<X509Certificate>, authType: String, trustSystemCerts: Boolean, appInForeground: Boolean?): Boolean {
        if (chain.isEmpty())
            throw IllegalArgumentException("Certificate chain must not be empty")
        val cert = chain[0]

        synchronized(this) {
            if (isTrustedByUser(cert))
                // explicitly accepted by user
                return true

            // explicitly rejected by user
            if (untrustedCerts.contains(cert))
                return false

            // check system certs, if applicable
            if (trustSystemCerts)
                try {
                    systemKeyStore.checkServerTrusted(chain, authType)

                    // trusted by system
                    return true
                } catch (_: CertificateException) {
                    // not trusted by system, ask user
                }
        }

        if (appInForeground == null) {
            logger.log(Level.INFO, "Certificate not known and running in non-interactive mode, rejecting")
            return false
        }

        return runBlocking {
            val ui = UserDecisionRegistry.getInstance(context)

            try {
                withTimeout(userTimeout) {
                    ui.check(cert, appInForeground)
                }
            } catch (_: TimeoutCancellationException) {
                logger.log(Level.WARNING, "User timeout while waiting for certificate decision, rejecting")
                false
            }
        }
    }

    /**
     * Determines whether a certificate has been explicitly accepted by the user. In this case,
     * we can ignore an invalid host name for that certificate.
     */
    @Synchronized
    override fun isTrustedByUser(cert: X509Certificate): Boolean =
        userKeyStore.getCertificateAlias(cert) != null

    @Synchronized
    override fun setTrustedByUser(cert: X509Certificate) {
        val alias = CertUtils.getTag(cert)
        logger.info("Trusted by user: ${cert.subjectDN.name} ($alias)")

        userKeyStore.setCertificateEntry(alias, cert)
        saveUserKeyStore()

        untrustedCerts -= cert
    }

    @Synchronized
    override fun setUntrustedByUser(cert: X509Certificate) {
        logger.info("Distrusted by user: ${cert.subjectDN.name}")

        // find certificate
        val alias = userKeyStore.getCertificateAlias(cert)
        if (alias != null) {
            // and delete, if applicable
            userKeyStore.deleteEntry(alias)
            saveUserKeyStore()
        }

        untrustedCerts += cert
    }


    @Synchronized
    private fun loadUserKeyStore() {
        try {
            FileInputStream(userKeyStoreFile).use {
                userKeyStore.load(it, null)
                logger.fine("Loaded ${userKeyStore.size()} trusted certificate(s)")
            }
        } catch(_: Exception) {
            logger.fine("No key store for trusted certificates (yet); creating in-memory key store.")
            try {
                userKeyStore.load(null, null)
            } catch(e: Exception) {
                logger.log(Level.SEVERE, "Couldn't initialize in-memory key store", e)
            }
        }
    }

    @Synchronized
    private fun saveUserKeyStore() {
        try {
            FileOutputStream(userKeyStoreFile).use { userKeyStore.store(it, null) }
        } catch(e: Exception) {
            logger.log(Level.SEVERE, "Couldn't save custom certificate key store", e)
        }
    }

    companion object {

        private const val KEYSTORE_DIR = "KeyStore"
        private const val KEYSTORE_NAME = "KeyStore.bks"

        @SuppressLint("StaticFieldLeak")    // we only store the applicationContext, so this is safe
        private var instance: CustomCertStore? = null

        @Synchronized
        fun getInstance(context: Context): CustomCertStore {
            instance?.let {
                return it
            }

            val newInstance = CustomCertStore(context.applicationContext)
            instance = newInstance
            return newInstance
        }

    }

}