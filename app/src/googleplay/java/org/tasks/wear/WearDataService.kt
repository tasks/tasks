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
import org.tasks.GrpcProto.Settings
import org.tasks.WearServiceGrpcKt
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearDataService : BaseGrpcDataService<WearServiceGrpcKt.WearServiceCoroutineImplBase>() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var headerFormatter: HeaderFormatter

    override val registry: WearDataLayerRegistry by lazy {
        applicationContext.wearDataLayerRegistry(lifecycleScope)
    }

    private val settings: DataStore<Settings> by lazy {
        registry.protoDataStore(lifecycleScope)
    }

    override fun buildService(): WearServiceGrpcKt.WearServiceCoroutineImplBase {
        return WearService(
            taskDao = taskDao,
            preferences = preferences,
            taskCompleter = taskCompleter,
            headerFormatter = headerFormatter,
            settings = settings,
        )
    }
}
