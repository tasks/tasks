package org.tasks.jobs

import android.content.Context
import androidx.work.WorkerParameters
import org.tasks.LocalBroadcastManager
import org.tasks.injection.ApplicationComponent
import org.tasks.scheduling.RefreshScheduler
import javax.inject.Inject

class RefreshWork(context: Context, workerParams: WorkerParameters) : RepeatingWorker(context, workerParams) {
    @Inject lateinit var refreshScheduler: RefreshScheduler
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    public override fun run(): Result {
        localBroadcastManager.broadcastRefresh()
        return Result.success()
    }

    override fun inject(component: ApplicationComponent) = component.inject(this)

    override fun scheduleNext() = refreshScheduler.scheduleNext()
}