package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavDiscoveryClient
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.http.HttpException
import org.tasks.http.httpStatusMessage
import org.tasks.resources.getString
import org.tasks.security.Encryption
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_encryption_failed
import tasks.kmp.generated.resources.google_tasks_permission_not_granted
import tasks.kmp.generated.resources.microsoft_tasks_permission_not_granted

fun interface PurchaseClaim {
    suspend fun claimHeldPurchase(username: String, token: String): HeldPurchaseClaim
}

sealed interface HeldPurchaseClaim {
    data object Claimed : HeldPurchaseClaim

    data object NotClaimed : HeldPurchaseClaim

    data class CoveringAnotherAccount(val existingAccount: String) : HeldPurchaseClaim
}

suspend fun setupTasksAccount(
    oauthResult: OAuthResult,
    issuer: String,
    caldavUrl: String,
    caldavDao: CaldavDao,
    encryption: Encryption,
    provider: CaldavClientFactory,
    purchaseClaim: PurchaseClaim? = null,
): CaldavAccount {
    val idToken = oauthResult.idToken
        ?: throw IllegalStateException("id_token required for tasks.org account setup")
    val sub = idToken.sub
        ?: throw IllegalStateException("id_token missing sub claim")
    val username = "${issuer}_$sub"
    val tokenString = oauthResult.accessToken
    val password = encryption.encryptOrThrow(tokenString)
    suspend fun <T> probe(url: String, block: suspend (CaldavDiscoveryClient) -> T): T =
        paymentRequiredOn402 { provider.forUrl(url, username, tokenString).use { block(it) } }
    suspend fun homeSet(): String = probe(caldavUrl) { it.homeSet() }
    suspend fun stillThere(url: String): Boolean = try {
        probe(url) { it.checkAccess() }
        true
    } catch (e: HttpException) {
        if (e.code != 404) throw e
        Logger.i(TAG) { "The home set moved, looking for it again" }
        false
    }
    return tasksSetupLock.withLock {
        val existing = caldavDao.getAccount(CaldavAccount.TYPE_TASKS, username)
        val claim = purchaseClaim?.claimHeldPurchase(username, tokenString)
        if (claim is HeldPurchaseClaim.CoveringAnotherAccount) {
            if (existing == null) {
                throw PurchaseTokenInUseException(claim.existingAccount)
            }
            Logger.i(TAG) { "The subscription held here funds ${claim.existingAccount}, signing in again anyway" }
        }
        if (existing == null) {
            val account = CaldavAccount(
                accountType = CaldavAccount.TYPE_TASKS,
                uuid = UUIDHelper.newUUID(),
                username = username,
                password = password,
                url = homeSet(),
                name = idToken.email ?: idToken.login,
                serverType = CaldavAccount.SERVER_TASKS,
            )
            return account.copy(id = caldavDao.insert(account))
        }
        signInAgain(existing, password, caldavDao) {
            existing.url?.takeIf { it.isNotBlank() && stillThere(it) } ?: homeSet()
        }
    }
}

private suspend fun signInAgain(
    existing: CaldavAccount,
    password: String,
    caldavDao: CaldavDao,
    verify: suspend () -> String,
): CaldavAccount {
    suspend fun keepToken(error: String): CaldavAccount {
        caldavDao.setPasswordAndError(existing.id, password, error)
        return existing.copy(password = password, error = error)
    }
    val url = try {
        verify()
    } catch (e: CancellationException) {
        throw e
    } catch (e: PaymentRequiredException) {
        keepToken(CaldavAccount.ERROR_PAYMENT_REQUIRED)
        throw e
    } catch (e: PurchaseTokenInUseException) {
        keepToken("${CaldavAccount.ERROR_PURCHASE_TOKEN_IN_USE}${e.existingAccount}")
        throw e
    } catch (e: Exception) {
        if (e.isRejection()) {
            throw e
        }
        Logger.e(TAG, e) { "Could not check a returning account" }
        return keepToken(e.signInError())
    }
    caldavDao.setVerified(existing.id, url, password)
    return existing.copy(password = password, url = url, error = "")
}

private fun Exception.isRejection() = this is HttpException && code in REJECTIONS

suspend fun Throwable.signInError(): String = when (this) {
    is HttpException -> httpStatusMessage(code)
    is DisplayableException -> getString(resource)
    else -> message ?: toString()
}

private val REJECTIONS = setOf(401, 403, 404)

suspend fun setupMicrosoftAccount(
    oauthResult: OAuthResult,
    caldavDao: CaldavDao,
    encryption: Encryption,
    analytics: Analytics,
) {
    val email = oauthResult.idToken?.email
    val preferredUsername = oauthResult.idToken?.preferredUsername
    val identifier = preferredUsername
        ?: email
        ?: throw Exception("No username in Microsoft OAuth response")
    val subject = oauthResult.idToken?.sub
        ?: throw Exception("No subject in Microsoft OAuth response")
    requireGrantedScope(
        grantedScopes = oauthResult.grantedScopes,
        message = Res.string.microsoft_tasks_permission_not_granted,
    ) { it.substringAfterLast('/').equals(MICROSOFT_TASKS_SCOPE, ignoreCase = true) }
    setupOAuthAccount(
        oauthResult = oauthResult,
        lock = microsoftSetupLock,
        syncType = Constants.SYNC_TYPE_MICROSOFT,
        encryption = encryption,
        analytics = analytics,
        findExisting = { caldavDao.findMicrosoftAccount(subject, preferredUsername, email) },
        reauthorize = { existing, password ->
            caldavDao.setMicrosoftReauth(
                id = existing.id,
                username = subject,
                name = identifier,
                password = password,
            )
        },
        insert = { password ->
            caldavDao.insert(
                CaldavAccount(
                    accountType = CaldavAccount.TYPE_MICROSOFT,
                    uuid = UUIDHelper.newUUID(),
                    name = identifier,
                    username = subject,
                    password = password,
                )
            )
        },
    )
}

suspend fun setupGoogleTasksAccount(
    oauthResult: OAuthResult,
    caldavDao: CaldavDao,
    encryption: Encryption,
    analytics: Analytics,
) {
    val email = oauthResult.idToken?.email
        ?: throw Exception("No email in Google Tasks OAuth response")
    requireGrantedScope(
        grantedScopes = oauthResult.grantedScopes,
        message = Res.string.google_tasks_permission_not_granted,
    ) { it == GOOGLE_TASKS_SCOPE }
    setupOAuthAccount(
        oauthResult = oauthResult,
        lock = googleTasksSetupLock,
        syncType = Constants.SYNC_TYPE_GOOGLE_TASKS,
        encryption = encryption,
        analytics = analytics,
        findExisting = { caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, email) },
        reauthorize = { existing, password ->
            caldavDao.setPasswordAndError(existing.id, password, "")
        },
        insert = { password ->
            caldavDao.insert(
                CaldavAccount(
                    accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
                    uuid = email,
                    name = email,
                    username = email,
                    password = password,
                )
            )
        },
    )
}

private suspend fun setupOAuthAccount(
    oauthResult: OAuthResult,
    lock: Mutex,
    syncType: String,
    encryption: Encryption,
    analytics: Analytics,
    findExisting: suspend () -> CaldavAccount?,
    reauthorize: suspend (existing: CaldavAccount, password: String) -> Unit,
    insert: suspend (password: String) -> Unit,
) {
    val refreshToken = oauthResult.refreshToken
        ?: throw Exception("No refresh_token - consent may not have been granted")
    val password = encryption.encryptOrThrow(oauthResult.toOAuthTokenData(refreshToken).serialize())
    val added = lock.withLock {
        val existing = findExisting()
        if (existing != null) {
            reauthorize(existing, password)
            false
        } else {
            insert(password)
            true
        }
    }
    if (added) {
        try {
            analytics.logEvent(
                AnalyticsEvents.SYNC_ADD_ACCOUNT,
                AnalyticsEvents.PARAM_TYPE to syncType,
            )
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to report added account" }
        }
    }
}

private suspend fun requireGrantedScope(
    grantedScopes: String?,
    message: StringResource,
    isRequiredScope: (String) -> Boolean,
) {
    if (grantedScopes == null) {
        Logger.w(TAG) { "No scope field in token response - cannot verify granted scopes" }
    } else if (grantedScopes.split(' ').none(isRequiredScope)) {
        Logger.e(TAG) { "Tasks scope missing. Granted scopes: $grantedScopes" }
        throw Exception(getString(message))
    } else {
        Logger.i(TAG) { "Granted scopes: $grantedScopes" }
    }
}

private suspend fun Encryption.encryptOrThrow(text: String): String =
    encrypt(text) ?: throw Exception(getString(Res.string.error_encryption_failed))

private val tasksSetupLock = Mutex()

val microsoftSetupLock = Mutex()

private val googleTasksSetupLock = Mutex()

private const val TAG = "AccountSetup"
