package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.injection.ApplicationComponent
import javax.inject.Inject

class MidnightRefreshWork(context: Context, workerParams: WorkerParameters) : RepeatingWorker(context, workerParams) {
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    override fun run(): Result {
        localBroadcastManager.broadcastRefresh()
        return Result.success()
    }

    override fun scheduleNext() = workManager.scheduleMidnightRefresh()

    override fun inject(component: ApplicationComponent) = component.inject(this)
}