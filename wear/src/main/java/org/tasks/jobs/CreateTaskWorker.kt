package org.tasks.jobs

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.tasks.GrpcProto
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.presentation.RefreshTrigger
import org.tasks.presentation.phoneTargetNodeId
import timber.log.Timber
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalHorologistApi::class)
class CreateTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val queue = PendingTaskQueue.getInstance(applicationContext)
        val tasks = queue.getAll()
        if (tasks.isEmpty()) return Result.success()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return try {
            val registry = applicationContext.wearDataLayerRegistry(scope)
            val wearService: WearServiceGrpcKt.WearServiceCoroutineStub = registry.grpcClient(
                nodeId = applicationContext.phoneTargetNodeId(),
                coroutineScope = scope,
            ) {
                WearServiceGrpcKt.WearServiceCoroutineStub(it)
            }
            for (task in tasks) {
                wearService.saveTask(
                    GrpcProto.SaveTaskRequest.newBuilder()
                        .setTitle(task.title)
                        .apply {
                            task.filter?.takeIf { it.isNotBlank() }?.let { setFilter(it) }
                        }
                        .build()
                )
                queue.remove(task)
                Timber.d("Created task: ${task.title}")
            }
            RefreshTrigger.trigger()
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create tasks")
            Result.retry()
        } finally {
            scope.cancel()
        }
    }

    companion object {
        private const val WORK_NAME = "create_task"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    OneTimeWorkRequestBuilder<CreateTaskWorker>()
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            10,
                            TimeUnit.SECONDS,
                        )
                        .build()
                )
        }
    }
}
