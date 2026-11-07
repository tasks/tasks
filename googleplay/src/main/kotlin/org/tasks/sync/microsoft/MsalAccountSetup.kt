package org.tasks.sync.microsoft

import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT

suspend fun setupMsalMicrosoftAccount(
    email: String,
    caldavDao: CaldavDao,
    onAccountAdded: () -> Unit = {},
) {
    val existing = caldavDao.getAccount(TYPE_MICROSOFT, email)
    if (existing != null) {
        caldavDao.update(existing.copy(error = null))
        return
    }
    caldavDao.insert(
        CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            name = email,
            username = email,
            accountType = TYPE_MICROSOFT,
        )
    )
    onAccountAdded()
}
