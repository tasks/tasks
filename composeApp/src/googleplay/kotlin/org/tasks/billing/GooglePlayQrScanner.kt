package org.tasks.billing

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlayQrScanner(private val context: Context) : QrScanner {
    override suspend fun scan(): String? {
        val scanner = GmsBarcodeScanning.getClient(context)
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    if (cont.isActive) cont.resume(barcode.rawValue)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
                .addOnCanceledListener {
                    if (cont.isActive) cont.resume(null)
                }

        }
    }
}
