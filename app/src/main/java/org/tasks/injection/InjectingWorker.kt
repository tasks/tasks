package org.tasks.injection

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.tasks.analytics.Firebase
import timber.log.Timber
import javax.inject.Inject

abstract class InjectingWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    @Inject lateinit var firebase: Firebase

    override fun doWork(): Result {
        Timber.d("%s.doWork()", javaClass.simpleName)
        val component = (applicationContext as InjectingApplication).component.plus(WorkModule())
        inject(component)
        return try {
            run()
        } catch (e: Exception) {
            firebase.reportException(e)
            Result.failure()
        }
    }

    protected abstract fun run(): Result

    protected abstract fun inject(component: JobComponent)
}