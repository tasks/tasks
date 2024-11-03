package org.tasks.wear

import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.lifecycle.lifecycleScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.ProtoDataStoreHelper.protoDataStore
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.GrpcProto.Settings
import org.tasks.WearServiceGrpcKt
import org.tasks.analytics.Firebase
import org.tasks.billing.Inventory
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.filters.FilterProvider
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.tasklist.HeaderFormatter
import org.tasks.themes.ColorProvider
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearDataService : BaseGrpcDataService<WearServiceGrpcKt.WearServiceCoroutineImplBase>() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var headerFormatter: HeaderFormatter
    @Inject lateinit var firebase: Firebase
    @Inject lateinit var filterProvider: FilterProvider
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskCreator: TaskCreator

    override val registry: WearDataLayerRegistry by lazy {
        applicationContext.wearDataLayerRegistry(lifecycleScope)
    }

    private val settings: DataStore<Settings> by lazy {
        registry.protoDataStore(lifecycleScope)
    }

    override fun buildService(): WearServiceGrpcKt.WearServiceCoroutineImplBase {
        return WearService(
            taskDao = taskDao,
            appPreferences = preferences,
            taskCompleter = taskCompleter,
            headerFormatter = headerFormatter,
            settings = settings,
            firebase = firebase,
            filterProvider = filterProvider,
            inventory = inventory,
            colorProvider = colorProvider,
            defaultFilterProvider = defaultFilterProvider,
            taskCreator = taskCreator,
            is24HourTime = DateFormat.is24HourFormat(applicationContext),
        )
    }
}
