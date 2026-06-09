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

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import tasks.cert4android.generated.resources.Res
import tasks.cert4android.generated.resources.trust_certificate_press_back_to_reject
import tasks.cert4android.generated.resources.trust_certificate_reset_info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec.SHA1
import java.security.spec.MGF1ParameterSpec.SHA256
import java.text.DateFormat
import java.util.logging.Level
import java.util.logging.Logger

class TrustCertificateActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CERTIFICATE = "certificate"
        const val EXTRA_TRUSTED = "trusted"

        fun rawCertFromIntent(intent: Intent): ByteArray =
            intent.getByteArrayExtra(EXTRA_CERTIFICATE) ?: throw IllegalArgumentException("EXTRA_CERTIFICATE required")
    }

    private val model by viewModels<Model>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        processIntent(intent)
        addOnNewIntentListener { newIntent ->
            processIntent(newIntent)
        }

        enableEdgeToEdge()

        setContent {
            ThemeManager.theme {
                MainLayout(
                    onRegisterDecision = { trusted -> model.registerDecision(trusted) },
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun processIntent(intent: Intent) {
        // process certificate
        model.parseCertificate(rawCertFromIntent(intent))

        // process EXTRA_TRUSTED, if available
        if (intent.hasExtra(EXTRA_TRUSTED)) {
            val trusted = intent.getBooleanExtra(EXTRA_TRUSTED, false)
            model.registerDecision(trusted)
        }
    }

    @Composable
    fun MainLayout(
        onRegisterDecision: (Boolean) -> Unit = {},
        onFinish: () -> Unit = {}
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val uiState = model.uiState
        LaunchedEffect(uiState.decided) {
            if (uiState.decided)
                onFinish()
        }

        var backPressedCounter by remember { mutableIntStateOf(0) }
        BackHandler {
            val newBackPressedCounter = backPressedCounter + 1
            when (newBackPressedCounter) {
                0 -> { /* back button not pressed yet */ }
                1 ->
                    scope.launch {
                        snackbarHostState.showSnackbar(getString(Res.string.trust_certificate_press_back_to_reject))
                    }
                else ->
                    onRegisterDecision(false)
            }
            backPressedCounter = newBackPressedCounter
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.padding(16.dp)
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TrustCertificateContent(
                    appName = applicationInfo.loadLabel(packageManager).toString(),
                    issuedFor = uiState.issuedFor,
                    issuedBy = uiState.issuedBy,
                    validFrom = uiState.validFrom,
                    validTo = uiState.validTo,
                    sha1 = uiState.sha1,
                    sha256 = uiState.sha256,
                    onAccept = { onRegisterDecision(true) },
                    onReject = { onRegisterDecision(false) },
                )
                Text(
                    text = stringResource(Res.string.trust_certificate_reset_info),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
        }
    }

    data class UiState(
        val issuedFor: String? = null,
        val issuedBy: String? = null,
        val validFrom: String? = null,
        val validTo: String? = null,
        val sha1: String? = null,
        val sha256: String? = null,

        val decided: Boolean = false
    )

    class Model(application: Application) : AndroidViewModel(application) {

        private val logger
            get() = Logger.getLogger(javaClass.name)

        private var cert: X509Certificate? = null

        var uiState by mutableStateOf(UiState())
            private set

        fun parseCertificate(rawCert: ByteArray) = viewModelScope.launch(Dispatchers.Default) {
            val certFactory = CertificateFactory.getInstance("X.509")!!
            (certFactory.generateCertificate(ByteArrayInputStream(rawCert)) as? X509Certificate)?.let { cert ->
                this@Model.cert = cert

                try {
                    val subject = cert.subjectAlternativeNames?.let { altNames ->
                        val sb = StringBuilder()
                        for (altName in altNames) {
                            val name = altName[1]
                            if (name is String)
                                sb.append("[").append(altName[0]).append("]").append(name).append(" ")
                        }
                        sb.toString()
                    } ?: /* use CN if alternative names are not available */ cert.subjectDN.name

                    val timeFormatter = DateFormat.getDateInstance(DateFormat.LONG)
                    Snapshot.withMutableSnapshot {      // thread-safe update of UI state
                        uiState = uiState.copy(
                            issuedFor = subject,
                            issuedBy = cert.issuerDN.toString(),
                            validFrom = timeFormatter.format(cert.notBefore),
                            validTo = timeFormatter.format(cert.notAfter),
                            sha1 = "SHA1: " + CertUtils.fingerprint(cert, SHA1.digestAlgorithm),
                            sha256 = "SHA256: " + CertUtils.fingerprint(cert, SHA256.digestAlgorithm),
                        )
                    }
                } catch (e: CertificateParsingException) {
                    logger.log(Level.WARNING, "Couldn't parse certificate", e)
                }
            }
        }

        fun registerDecision(trusted: Boolean) {
            // notify user decision registry
            cert?.let {
                UserDecisionRegistry.getInstance(getApplication()).onUserDecision(it, trusted)

                // notify UI that the case has been decided (causes Activity to finish)
                uiState = uiState.copy(decided = true)
            }
        }
    }
}
