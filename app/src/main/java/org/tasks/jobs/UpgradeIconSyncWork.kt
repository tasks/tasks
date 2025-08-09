package org.tasks.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.analytics.Firebase
import org.tasks.caldav.CaldavClientProvider
import org.tasks.caldav.property.CalendarIcon
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.BaseWorker
import timber.log.Timber

@HiltWorker
class UpgradeIconSyncWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val clientProvider: CaldavClientProvider,
    private val caldavDao: CaldavDao,
) : BaseWorker(context, workerParams, firebase) {
    override suspend fun run(): Result {
        var response = Result.success()
        caldavDao
            .getAccounts(CaldavAccount.TYPE_TASKS, CaldavAccount.TYPE_CALDAV)
            .forEach { account ->
                Timber.d("Uploading icons for $account")
                val caldavClient = clientProvider.forAccount(account)
                caldavClient.calendars().forEach { remote ->
                    val url = remote.href
                    val calendar = caldavDao
                        .getCalendarByUrl(account.uuid!!, url.toString())
                        ?.takeIf { !it.readOnly() && it.icon?.isNotBlank() == true }
                        ?: run {
                            Timber.d("No icon set for $url")
                            return@forEach
                        }
                    val icon = remote[CalendarIcon::class.java]?.icon
                    if (icon?.isNotBlank() == true) {
                        Timber.d("Remote icon already set for $url")
                        return@forEach
                    }
                    Timber.d("Uploading icon to ${calendar.icon} for $url")
                    caldavClient.updateIcon(
                        url = url,
                        icon = calendar.icon,
                        onFailure = { response = Result.retry() }
                    )
                }
            }
        return response
    }
}
