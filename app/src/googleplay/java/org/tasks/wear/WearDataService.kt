package org.tasks.wear

import androidx.datastore.core.DataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.GrpcProto.LastUpdate
import org.tasks.LocalBroadcastManager
import org.tasks.WearServiceGrpcKt
import org.tasks.preferences.Preferences
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearDataService : BaseGrpcDataService<WearServiceGrpcKt.WearServiceCoroutineImplBase>() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private val lastUpdate: DataStore<LastUpdate> by lazy {
        registry.protoDataStore<LastUpdate>(lifecycleScope)
    }

    override val registry: WearDataLayerRegistry by lazy {
        WearDataLayerRegistry.fromContext(
            application = applicationContext,
            coroutineScope = lifecycleScope,
        ).apply {
            registerSerializer(TasksSerializer)
            registerSerializer(LastUpdateSerializer)
        }
    }

    override fun buildService(): WearServiceGrpcKt.WearServiceCoroutineImplBase {
        return WearService(
            taskDao = taskDao,
            preferences = preferences,
            taskCompleter = taskCompleter,
            lastUpdate = lastUpdate,
            localBroadcastManager = localBroadcastManager,
        )
    }
}
