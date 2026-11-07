@file:Suppress("ClassName")

package com.todoroo.astrid.service

import co.touchlab.kermit.Logger
import org.tasks.auth.AppAuthTokenMigration
import org.tasks.auth.OAuthTokenData
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.security.Encryption
import org.tasks.service.Upgrade
import kotlin.coroutines.cancellation.CancellationException

class Upgrade_15_15(
    private val caldavDao: CaldavDao,
    private val encryption: Encryption,
) : Upgrade {
    private val logger = Logger.withTag("Upgrade_15_15")

    override suspend fun run() = migrateAppAuthCredentials()

    suspend fun migrateAppAuthCredentials() {
        for (account in caldavDao.getAccounts(TYPE_MICROSOFT)) {
            if (account.password.isNullOrBlank()) {
                continue
            }
            try {
                val decrypted = encryption.decrypt(account.password)
                if (decrypted == null) {
                    logger.w { "Failed to decrypt credentials for account ${account.id}" }
                    continue
                }
                if (runCatching { OAuthTokenData.deserialize(decrypted) }.isSuccess) {
                    continue
                }
                val migrated = AppAuthTokenMigration.migrate(decrypted)
                if (migrated == null) {
                    logger.w { "Unrecognized credentials for account ${account.id}, sign in again" }
                    continue
                }
                val encrypted = encryption.encrypt(migrated.tokenData.serialize())
                if (encrypted == null) {
                    logger.w { "Failed to encrypt migrated credentials for account ${account.id}" }
                    continue
                }
                caldavDao.setPassword(account.id, encrypted)
                logger.i { "Migrated credentials for account ${account.id}" }
                moveOntoSubject(account, migrated.subject)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Failed to migrate credentials for account ${account.id}" }
            }
        }
    }

    private suspend fun moveOntoSubject(account: CaldavAccount, subject: String?) {
        if (subject == null || subject == account.username) {
            return
        }
        val existing = caldavDao.getAccount(TYPE_MICROSOFT, subject)
        if (existing != null && existing.id != account.id) {
            logger.w { "Account ${existing.id} already uses the subject of account ${account.id}" }
            return
        }
        caldavDao.setUsername(account.id, subject)
    }

    companion object {
        const val VERSION = 151500
    }
}
