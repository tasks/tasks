package org.tasks.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.compose.resources.StringResource
import org.tasks.analytics.Analytics
import org.tasks.analytics.AnalyticsEvents
import org.tasks.analytics.Constants
import org.tasks.caldav.CaldavClientFactory
import org.tasks.caldav.CaldavDiscoveryClient
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.tasksOrgAccount
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.PushTokenManager
import org.tasks.http.HttpException
import org.tasks.http.httpStatusMessage
import org.tasks.http.isConnectionFailure
import org.tasks.http.isReadTimeout
import org.tasks.resources.getString
import org.tasks.security.Encryption
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.error_encryption_failed
import tasks.kmp.generated.resources.google_tasks_permission_not_granted
import tasks.kmp.generated.resources.microsoft_tasks_permission_not_granted
import tasks.kmp.generated.resources.network_error
import tasks.kmp.generated.resources.wrong_account

class WrongAccountException(
    val existingAccount: String?,
    val provider: OAuthProvider,
) : Exception("Signed in with a different tasks.org account")

class SignInInterruptedException(cause: CancellationException) : Exception(null, cause)

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
    purchaseClaim: PurchaseClaim?,
    pushTokenManager: PushTokenManager,
    analytics: Analytics,
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
    var added = false
    val account = tasksSetupLock.withLockNonCancellable {
        val existing = caldavDao.getAccount(CaldavAccount.TYPE_TASKS, username)
        if (existing == null) {
            caldavDao.tasksOrgAccount()?.let { other ->
                throw WrongAccountException(
                    existingAccount = other.name?.takeIf { it.isNotBlank() },
                    provider = other.tasksOrgProvider,
                )
            }
        }
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
            added = true
            return@withLockNonCancellable account.copy(id = caldavDao.insert(account))
        }
        signInAgain(existing, password, caldavDao) {
            existing.url?.takeIf { it.isNotBlank() && stillThere(it) } ?: homeSet()
        }
    }
    if (account.error.isNullOrBlank()) {
        pushTokenManager.registerTokenForAccount(account)
    }
    if (added) {
        analytics.logEvent(
            AnalyticsEvents.SYNC_ADD_ACCOUNT,
            AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_TASKS_ORG,
        )
    }
    return account
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
    } catch (e: PaymentRequiredException) {
        keepToken(CaldavAccount.ERROR_PAYMENT_REQUIRED)
        throw e
    } catch (e: PurchaseTokenInUseException) {
        keepToken("${CaldavAccount.ERROR_PURCHASE_TOKEN_IN_USE}${e.existingAccount}")
        throw e
    } catch (e: CancellationException) {
        keepToken("")
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

suspend fun Throwable.signInError(): String = when {
    this is HttpException -> httpStatusMessage(code)
    this is DisplayableException -> getString(resource)
    isConnectionFailure() -> getString(Res.string.network_error)
    isReadTimeout() -> getString(Res.string.wrong_account)
    else -> message?.takeIf { it.isNotBlank() }
        ?: getString(if (this is IOException) Res.string.network_error else Res.string.wrong_account)
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

suspend fun setupGoogleTasksAccount(
    accountName: String,
    caldavDao: CaldavDao,
    analytics: Analytics,
) {
    val added = googleTasksSetupLock.withLockNonCancellable {
        val existing = caldavDao.getAccounts(listOf(CaldavAccount.TYPE_GOOGLE_TASKS))
            .find { it.username.equals(accountName, ignoreCase = true) }
        if (existing != null) {
            caldavDao.update(existing.copy(error = ""))
            caldavDao.resetLastSync(existing.uuid ?: accountName)
            false
        } else {
            caldavDao.insert(
                CaldavAccount(
                    accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
                    uuid = accountName,
                    name = accountName,
                    username = accountName,
                )
            )
            true
        }
    }
    if (added) {
        analytics.logEvent(
            AnalyticsEvents.SYNC_ADD_ACCOUNT,
            AnalyticsEvents.PARAM_TYPE to Constants.SYNC_TYPE_GOOGLE_TASKS,
        )
    }
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
    val added = lock.withLockNonCancellable {
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
        analytics.logEvent(
            AnalyticsEvents.SYNC_ADD_ACCOUNT,
            AnalyticsEvents.PARAM_TYPE to syncType,
        )
    }
}

private suspend fun <T> Mutex.withLockNonCancellable(block: suspend () -> T): T =
    withLock {
        withContext(NonCancellable) {
            try {
                block()
            } catch (e: CancellationException) {
                throw SignInInterruptedException(e)
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
        throw DisplayableException(message)
    } else {
        Logger.i(TAG) { "Granted scopes: $grantedScopes" }
    }
}

private suspend fun Encryption.encryptOrThrow(text: String): String =
    encrypt(text) ?: throw DisplayableException(Res.string.error_encryption_failed)

private val tasksSetupLock = Mutex()

val microsoftSetupLock = Mutex()

private val googleTasksSetupLock = Mutex()

private const val TAG = "AccountSetup"
