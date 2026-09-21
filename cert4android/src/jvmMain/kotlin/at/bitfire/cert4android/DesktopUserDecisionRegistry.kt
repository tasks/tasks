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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec.SHA1
import java.security.spec.MGF1ParameterSpec.SHA256
import java.text.DateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

class DesktopUserDecisionRegistry(
    private val userTimeout: Long = 60_000L
) {

    private val logger
        get() = Logger.getLogger(javaClass.name)

    data class PendingCertPrompt(
        val cert: X509Certificate,
        val issuedFor: String,
        val issuedBy: String,
        val validFrom: String,
        val validTo: String,
        val sha256: String,
        val sha1: String,
    )

    private val _currentPrompt = MutableStateFlow<PendingCertPrompt?>(null)
    val currentPrompt: StateFlow<PendingCertPrompt?> = _currentPrompt.asStateFlow()

    private val pendingDecisions = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    fun check(cert: X509Certificate, appInForeground: Boolean): Boolean {
        if (!appInForeground) {
            logger.warning("App not in foreground, rejecting certificate")
            return false
        }

        val fingerprint = CertUtils.getTag(cert)
        val deferred = pendingDecisions.computeIfAbsent(fingerprint) { _ ->
            val d = CompletableDeferred<Boolean>()
            val timeFormatter = DateFormat.getDateInstance(DateFormat.LONG)
            val prompt = PendingCertPrompt(
                cert = cert,
                issuedFor = cert.subjectAlternativeNames?.let { altNames ->
                    altNames.mapNotNull { altName ->
                        (altName[1] as? String)?.let { "[${altName[0]}]$it" }
                    }.joinToString(" ")
                } ?: cert.subjectDN.name,
                issuedBy = cert.issuerDN.toString(),
                validFrom = timeFormatter.format(cert.notBefore),
                validTo = timeFormatter.format(cert.notAfter),
                sha256 = "SHA256: " + CertUtils.fingerprint(cert, SHA256.digestAlgorithm),
                sha1 = "SHA1: " + CertUtils.fingerprint(cert, SHA1.digestAlgorithm),
            )
            _currentPrompt.value = prompt
            d
        }

        return try {
            runBlocking {
                withTimeout(userTimeout) {
                    deferred.await()
                }
            }
        } catch (_: TimeoutCancellationException) {
            logger.log(Level.WARNING, "User timeout while waiting for certificate decision, rejecting")
            pendingDecisions.remove(fingerprint)
            _currentPrompt.value = null
            false
        }
    }

    fun onUserDecision(trusted: Boolean) {
        val prompt = _currentPrompt.value ?: return
        val fingerprint = CertUtils.getTag(prompt.cert)
        _currentPrompt.value = null
        pendingDecisions.remove(fingerprint)?.complete(trusted)
    }
}
