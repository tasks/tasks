package org.tasks.smsgateway

import android.content.Context
import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Sends a short text to the user's carrier email-to-SMS gateway address
 * (e.g. 5551234567@carrier-gateway) over SMTP whenever a task reminder fires.
 * If the phone is offline, the message is queued and sent when a network returns.
 */
object SmsGateway {
    const val PREFS = "sms_gateway"
    const val KEY_ENABLED = "enabled"
    const val KEY_GATEWAY = "gateway_address"
    const val KEY_HOST = "smtp_host"
    const val KEY_PORT = "smtp_port"
    const val KEY_USER = "smtp_user"
    const val KEY_PASSWORD = "smtp_password"
    const val KEY_FROM = "from_address"
    private const val KEY_TEXT = "text"
    private const val MAX_CHARS = 150

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfigured(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ENABLED, false) &&
            !p.getString(KEY_GATEWAY, "").isNullOrBlank() &&
            !p.getString(KEY_HOST, "").isNullOrBlank() &&
            !p.getString(KEY_USER, "").isNullOrBlank() &&
            !p.getString(KEY_PASSWORD, "").isNullOrBlank()
    }

    /** Try to send right now; if that fails (offline, server error) queue a retry. */
    suspend fun sendOrQueue(context: Context, text: String) {
        if (!isConfigured(context)) return
        try {
            withContext(Dispatchers.IO) { send(context, text) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SMS gateway send failed; queueing retry")
            enqueueRetry(context, text)
        }
    }

    private fun enqueueRetry(context: Context, text: String) {
        val request = OneTimeWorkRequestBuilder<SmsGatewayWork>()
            .setInputData(workDataOf(KEY_TEXT to text))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueue(request)
    }

    /** Blocking. Call off the main thread. Throws on failure. */
    fun send(context: Context, text: String) {
        val p = prefs(context)
        val host = p.getString(KEY_HOST, "").orEmpty().trim()
        val port = p.getString(KEY_PORT, "587").orEmpty().trim().toIntOrNull() ?: 587
        val user = p.getString(KEY_USER, "").orEmpty().trim()
        val pass = p.getString(KEY_PASSWORD, "").orEmpty()
        val gateway = p.getString(KEY_GATEWAY, "").orEmpty().trim()
        val from = p.getString(KEY_FROM, "").orEmpty().trim().ifBlank { user }

        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")
            if (port == 465) {
                put("mail.smtp.ssl.enable", "true")
            } else {
                put("mail.smtp.starttls.enable", "true")
            }
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.writetimeout", "15000")
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
        })
        val body = text.replace(Regex("\\s+"), " ").trim().take(MAX_CHARS)
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(from))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(gateway))
            // Deliberately no subject: some carrier gateways prepend it to the text.
            setText(body)
            sentDate = Date()
        }
        Transport.send(message)
    }
}

class SmsGatewayWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val text = inputData.getString("text") ?: return Result.failure()
        return try {
            withContext(Dispatchers.IO) { SmsGateway.send(applicationContext, text) }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SMS gateway retry failed (attempt $runAttemptCount)")
            if (runAttemptCount < 20) Result.retry() else Result.failure()
        }
    }
}
