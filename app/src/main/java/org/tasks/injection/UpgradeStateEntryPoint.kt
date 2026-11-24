package org.tasks.injection

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.tasks.service.UpgradeState

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpgradeStateEntryPoint {
    val upgradeState: UpgradeState
}

val Context.upgradeState: UpgradeState
    get() = EntryPointAccessors
        .fromApplication(applicationContext, UpgradeStateEntryPoint::class.java)
        .upgradeState
