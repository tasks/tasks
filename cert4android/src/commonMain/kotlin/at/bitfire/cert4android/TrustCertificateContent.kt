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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tasks.cert4android.generated.resources.Res
import tasks.cert4android.generated.resources.trust_certificate_accept
import tasks.cert4android.generated.resources.trust_certificate_fingerprint_verified
import tasks.cert4android.generated.resources.trust_certificate_fingerprints
import tasks.cert4android.generated.resources.trust_certificate_issued_by
import tasks.cert4android.generated.resources.trust_certificate_issued_for
import tasks.cert4android.generated.resources.trust_certificate_reject
import tasks.cert4android.generated.resources.trust_certificate_unknown_certificate_found
import tasks.cert4android.generated.resources.trust_certificate_validity_period
import tasks.cert4android.generated.resources.trust_certificate_validity_period_value
import tasks.cert4android.generated.resources.trust_certificate_x509_certificate_details

/**
 * Shared composable that displays certificate details and accept/reject buttons.
 * Can be used by both Android (in an Activity/Fragment) and desktop (in a Dialog).
 *
 * @param appName  the name of the consuming application, shown in the trust prompt
 */
@Composable
fun TrustCertificateContent(
    appName: String,
    issuedFor: String?,
    issuedBy: String?,
    validFrom: String?,
    validTo: String?,
    sha1: String?,
    sha256: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.trust_certificate_unknown_certificate_found, appName),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            ) {
                    Text(
                        text = stringResource(Res.string.trust_certificate_x509_certificate_details),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    )

                    if (issuedFor != null)
                        CertInfoField(
                            stringResource(Res.string.trust_certificate_issued_for),
                            issuedFor,
                        )
                    if (issuedBy != null)
                        CertInfoField(
                            stringResource(Res.string.trust_certificate_issued_by),
                            issuedBy,
                        )
                    if (validFrom != null && validTo != null)
                        CertInfoField(
                            stringResource(Res.string.trust_certificate_validity_period),
                            stringResource(
                                Res.string.trust_certificate_validity_period_value,
                                validFrom,
                                validTo,
                            ),
                        )

                    if (sha1 != null || sha256 != null) {
                        Text(
                            text = stringResource(Res.string.trust_certificate_fingerprints).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (sha1 != null)
                            Text(
                                text = sha1,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp, top = 4.dp),
                            )
                        if (sha256 != null)
                            Text(
                                text = sha256,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp, top = 4.dp),
                            )
                    }
                }
            }


        var fingerprintVerified by rememberSaveable { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Checkbox(
                checked = fingerprintVerified,
                onCheckedChange = { fingerprintVerified = it },
            )
            Text(
                text = stringResource(Res.string.trust_certificate_fingerprint_verified),
                modifier = Modifier
                    .clickable { fingerprintVerified = !fingerprintVerified }
                    .weight(1f)
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                enabled = fingerprintVerified,
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) { Text(stringResource(Res.string.trust_certificate_accept).uppercase()) }
            TextButton(
                onClick = onReject,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.trust_certificate_reject).uppercase()) }
        }
    }
}

@Composable
private fun CertInfoField(label: String, value: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    )
}

@Preview
@Composable
private fun TrustCertificateContentPreview() {
    MaterialTheme {
        TrustCertificateContent(
            appName = "cert4android",
            issuedFor = "[2]example.com [2]*.example.com",
            issuedBy = "CN=Let's Encrypt Authority X3, O=Let's Encrypt, C=US",
            validFrom = "Jan 1, 2025",
            validTo = "Apr 1, 2025",
            sha1 = "SHA1: AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01",
            sha256 = "SHA256: AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89",
            onAccept = {},
            onReject = {},
        )
    }
}
