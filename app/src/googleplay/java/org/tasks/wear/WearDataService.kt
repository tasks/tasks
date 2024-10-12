package org.tasks.wear

import androidx.lifecycle.lifecycleScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.WearServiceGrpcKt
import org.tasks.preferences.Preferences
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearDataService : BaseGrpcDataService<WearServiceGrpcKt.WearServiceCoroutineImplBase>() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCompleter: TaskCompleter

    override val registry: WearDataLayerRegistry by lazy {
        WearDataLayerRegistry.fromContext(
            application = applicationContext,
            coroutineScope = lifecycleScope,
        ).apply {
            registerSerializer(TasksSerializer)
        }
    }

    override fun buildService(): WearServiceGrpcKt.WearServiceCoroutineImplBase {
        return WearService(
            taskDao = taskDao,
            preferences = preferences,
            taskCompleter = taskCompleter,
        )
    }
}
