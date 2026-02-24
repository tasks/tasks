package org.tasks.extensions

import android.content.Context
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalHorologistApi::class)
fun Context.wearDataLayerRegistry(scope: CoroutineScope) =
    WearDataLayerRegistry.fromContext(
        application = applicationContext,
        coroutineScope = scope,
    )
