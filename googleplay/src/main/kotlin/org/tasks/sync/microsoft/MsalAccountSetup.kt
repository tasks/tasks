package org.tasks.sync.microsoft

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tasks.auth.microsoftSetupLock
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT

data class MsalSignIn(
    val username: String,
    val subject: String?,
)

internal data class MsalIdentity(
    val subject: String?,
    val username: String?,
)

internal fun <A> List<A>.findFor(
    username: String?,
    name: String?,
    identity: (A) -> MsalIdentity,
): A? {
    if (username == null) {
        return null
    }
    return firstOrNull { identity(it).subject == username }
        ?: firstOrNull { identity(it).username == username }
        ?: name?.let { firstOrNull { identity(it).username == name } }
}

internal fun List<MsalIdentity>.subjectsByUsername(): Map<String, String> =
    mapNotNull { identity ->
        val username = identity.username ?: return@mapNotNull null
        val subject = identity.subject ?: return@mapNotNull null
        username to subject
    }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.distinct().singleOrNull() }
        .filterValues { it != null }
        .mapValues { it.value!! }

suspend fun setupMsalMicrosoftAccount(
    signIn: MsalSignIn,
    caldavDao: CaldavDao,
    onAccountAdded: () -> Unit = {},
) {
    val added = withContext(NonCancellable) {
        microsoftSetupLock.withLock {
            val existing = caldavDao.findMicrosoftAccount(
                subject = signIn.subject ?: signIn.username,
                preferredUsername = signIn.username,
                email = null,
            )
            if (existing != null) {
                caldavDao.setMicrosoftReauth(
                    id = existing.id,
                    username = signIn.subject ?: existing.username,
                    name = signIn.username,
                    password = existing.password,
                )
                false
            } else {
                caldavDao.insert(
                    CaldavAccount(
                        uuid = UUIDHelper.newUUID(),
                        name = signIn.username,
                        username = signIn.subject ?: signIn.username,
                        accountType = TYPE_MICROSOFT,
                    )
                )
                true
            }
        }
    }
    if (added) {
        onAccountAdded()
    }
}
