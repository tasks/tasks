package org.tasks.ssl

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.bitfire.cert4android.DesktopUserDecisionRegistry
import at.bitfire.cert4android.TrustCertificateContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustCertificateDialog(userDecisionRegistry: DesktopUserDecisionRegistry) {
    val prompt by userDecisionRegistry.currentPrompt.collectAsState()
    val currentPrompt = prompt ?: return

    BasicAlertDialog(onDismissRequest = { userDecisionRegistry.onUserDecision(false) }) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            TrustCertificateContent(
                modifier = Modifier.padding(24.dp),
                appName = "Tasks.org",
                issuedFor = currentPrompt.issuedFor,
                issuedBy = currentPrompt.issuedBy,
                validFrom = currentPrompt.validFrom,
                validTo = currentPrompt.validTo,
                sha1 = currentPrompt.sha1,
                sha256 = currentPrompt.sha256,
                onAccept = { userDecisionRegistry.onUserDecision(true) },
                onReject = { userDecisionRegistry.onUserDecision(false) },
            )
        }
    }
}
