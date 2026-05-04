package org.tasks.wear

import androidx.lifecycle.lifecycleScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.grpc.server.BaseGrpcDataService
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.WearServiceGrpcKt
import org.tasks.analytics.Analytics
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.BuildConfig
import org.tasks.watch.WatchServiceLogic
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearDataService : BaseGrpcDataService<WearServiceGrpcKt.WearServiceCoroutineImplBase>() {

    @Inject lateinit var watchServiceLogic: WatchServiceLogic
    @Inject lateinit var analytics: Analytics

    override val registry: WearDataLayerRegistry by lazy {
        applicationContext.wearDataLayerRegistry(lifecycleScope)
    }

    override fun buildService(): WearServiceGrpcKt.WearServiceCoroutineImplBase {
        return WearService(
            logic = watchServiceLogic,
            analytics = analytics,
            versionCode = BuildConfig.VERSION_CODE,
        )
    }
}
