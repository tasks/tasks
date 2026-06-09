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

class DesktopCertStore(
    private val dataDir: File,
    private val userDecisionRegistry: DesktopUserDecisionRegistry,
) : CertStore {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    private val systemKeyStore by lazy {
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
            init(null as KeyStore?)
            trustManagers
                .filterIsInstance<X509TrustManager>()
                .firstOrNull()
                ?: throw IllegalStateException("No X509TrustManager found")
        }
    }

    private val userKeyStore = KeyStore.getInstance("PKCS12")!!
    private val userKeyStoreFile = File(dataDir, "ssl${File.separator}user-certs.p12")

    private var untrustedCerts = HashSet<X509Certificate>()

    init {
        loadUserKeyStore()
    }

    @Synchronized
    override fun clearUserDecisions() {
        logger.info("Clearing user-(dis)trusted certificates")

        for (alias in userKeyStore.aliases())
            userKeyStore.deleteEntry(alias)
        saveUserKeyStore()

        untrustedCerts.clear()
    }

    override fun isTrusted(
        chain: Array<X509Certificate>,
        authType: String,
        trustSystemCerts: Boolean,
        appInForeground: Boolean?,
    ): Boolean {
        if (chain.isEmpty())
            throw IllegalArgumentException("Certificate chain must not be empty")
        val cert = chain[0]

        synchronized(this) {
            if (isTrustedByUser(cert))
                return true

            if (untrustedCerts.contains(cert))
                return false

            if (trustSystemCerts)
                try {
                    systemKeyStore.checkServerTrusted(chain, authType)
                    return true
                } catch (_: CertificateException) {
                    // not trusted by system, ask user
                }
        }

        if (appInForeground == null) {
            logger.log(Level.INFO, "Certificate not known and running in non-interactive mode, rejecting")
            return false
        }

        val trusted = userDecisionRegistry.check(cert, appInForeground)
        if (trusted) {
            setTrustedByUser(cert)
        } else {
            setUntrustedByUser(cert)
        }
        return trusted
    }

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

        val alias = userKeyStore.getCertificateAlias(cert)
        if (alias != null) {
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
        } catch (_: Exception) {
            logger.fine("No key store for trusted certificates (yet); creating in-memory key store.")
            try {
                userKeyStore.load(null, null)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Couldn't initialize in-memory key store", e)
            }
        }
    }

    @Synchronized
    private fun saveUserKeyStore() {
        try {
            userKeyStoreFile.parentFile?.mkdirs()
            FileOutputStream(userKeyStoreFile).use { userKeyStore.store(it, null) }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Couldn't save custom certificate key store", e)
        }
    }
}
