package org.tasks.todoist

import android.content.Context
import android.graphics.Color
import at.bitfire.ical4android.ICalendar.Companion.prodId
import com.todoroo.astrid.service.TaskDeleter
import dagger.hilt.android.qualifiers.ApplicationContext
import net.fortuna.ical4j.model.property.ProdId
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import javax.inject.Inject
import org.tasks.todoist.TodoistClient.TodoistCollection
import org.tasks.todoist.TodoistClient.TodoistItem

class TodoistSynchronizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val caldavDao: CaldavDao,
    private val localBroadcastManager: LocalBroadcastManager,
    private val taskDeleter: TaskDeleter,
    private val inventory: Inventory,
    private val clientProvider: TodoistClientProvider,
    private val iCal: iCalendar,
    private val vtodoCache: VtodoCache,
) {
    companion object {
        init {
            prodId = ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN")
        }
    }

    suspend fun sync(account: CaldavAccount) {
        Timber.d("Synchronizing $account")
        Thread.currentThread().contextClassLoader = context.classLoader

        if (!inventory.hasPro) {
            setError(account, context.getString(R.string.requires_pro_subscription))
            return
        }
        if (isNullOrEmpty(account.password)) {
            setError(account, context.getString(R.string.password_required))
            return
        }
        try {
            synchronize(account)
        } catch (e: Exception) {
            setError(account, e)
        }
    }

    private suspend fun synchronize(account: CaldavAccount) {
        // Stubbed implementation for Todoist API
        setError(account, "")
    }

    private suspend fun setError(account: CaldavAccount, e: Throwable) =
            setError(account, e.message)

    private suspend fun setError(account: CaldavAccount, message: String?) {
        account.error = message
        caldavDao.update(account)
        localBroadcastManager.broadcastRefreshList()
        if (!isNullOrEmpty(message)) {
            Timber.e(message)
        }
    }

    private suspend fun fetchChanges(
        account: CaldavAccount,
        client: TodoistClient,
        caldavCalendar: CaldavCalendar,
        collection: TodoistCollection
    ) {
        // Stubbed implementation for Todoist API
    }

    private suspend fun pushLocalChanges(
        account: CaldavAccount,
        client: TodoistClient,
        caldavCalendar: CaldavCalendar,
        collection: TodoistCollection
    ) {
        // Stubbed implementation for Todoist API
    }

    private suspend fun applyEntries(
        account: CaldavAccount,
        caldavCalendar: CaldavCalendar,
        items: List<TodoistItem>,
        stoken: String? = null,
        isLocalChange: Boolean = false
    ) {
        // Stubbed implementation for Todoist API
    }
}
