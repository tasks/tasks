package org.tasks.locale.receiver

import android.content.Context
import android.os.Bundle
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.tasks.Notifier
import org.tasks.analytics.Firebase
import org.tasks.injection.BaseWorker
import org.tasks.locale.bundle.ListNotificationBundle
import org.tasks.locale.bundle.TaskCreationBundle
import org.tasks.preferences.DefaultFilterProvider
import timber.log.Timber

@HiltWorker
class TaskerWork @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    firebase: Firebase,
    private val notifier: Notifier,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val taskerTaskCreator: TaskerTaskCreator,
) : BaseWorker(context, workerParams, firebase) {

    override suspend fun run(): Result {
        val bundle = inputData.toBundle()
        when {
            ListNotificationBundle.isBundleValid(bundle) ->
                notifier.triggerFilterNotification(defaultFilterProvider.getFilterFromPreference(
                        bundle.getString(ListNotificationBundle.BUNDLE_EXTRA_STRING_FILTER)))
            TaskCreationBundle.isBundleValid(bundle) ->
                taskerTaskCreator.handle(TaskCreationBundle(bundle))
            else -> Timber.e("Invalid bundle: $bundle")
        }
        return Result.success()
    }

    companion object {
        fun enqueueWork(context: Context, bundle: Bundle) {
            val data = try {
                bundle.toData()
            } catch (e: IllegalStateException) {
                Timber.e(e)
                return
            }
            androidx.work.WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequest.Builder(TaskerWork::class.java)
                    .setInputData(data)
                    .build()
            )
        }

        private fun Bundle.toData(): Data {
            val builder = Data.Builder()
            for (key in keySet()) {
                when (val value = @Suppress("DEPRECATION") get(key)) {
                    is String -> builder.putString(key, value)
                    is Int -> builder.putInt(key, value)
                    is Long -> builder.putLong(key, value)
                    is Boolean -> builder.putBoolean(key, value)
                    else -> Timber.w("Dropping $key=$value")
                }
            }
            return builder.build()
        }

        private fun Data.toBundle() = Bundle().apply {
            keyValueMap.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
    }
}
