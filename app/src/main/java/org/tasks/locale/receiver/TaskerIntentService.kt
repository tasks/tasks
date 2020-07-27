package org.tasks.locale.receiver

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.Notifier
import org.tasks.injection.InjectingJobIntentService
import org.tasks.locale.bundle.ListNotificationBundle
import org.tasks.locale.bundle.TaskCreationBundle
import org.tasks.preferences.DefaultFilterProvider
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TaskerIntentService : InjectingJobIntentService() {
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskerTaskCreator: TaskerTaskCreator

    override suspend fun doWork(intent: Intent) {
        val bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE)
        when {
            bundle == null ->
                Timber.e("${com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE} is missing")
            ListNotificationBundle.isBundleValid(bundle) ->
                notifier.triggerFilterNotification(defaultFilterProvider.getFilterFromPreference(
                        bundle.getString(ListNotificationBundle.BUNDLE_EXTRA_STRING_FILTER)))
            TaskCreationBundle.isBundleValid(bundle) ->
                taskerTaskCreator.handle(TaskCreationBundle(bundle))
            else -> Timber.e("Invalid bundle: $bundle")
        }
    }
}