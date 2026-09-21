package org.tasks.activities

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.activities.TagSettingsActivity.Companion.EXTRA_TAG_DATA
import org.tasks.analytics.Reporting
import org.tasks.billing.PurchaseState
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.metadata.TagMetadataSync
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData
import org.tasks.sync.SyncAdapters
import org.tasks.viewmodel.TagSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class TagSettingsHiltViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tagDataDao: TagDataDao,
    refreshBroadcaster: RefreshBroadcaster,
    reporting: Reporting,
    purchaseState: PurchaseState,
    tagMetadataSync: TagMetadataSync,
    syncAdapters: SyncAdapters,
    @ApplicationContext context: Context,
) : TagSettingsViewModel(
    tagDataDao = tagDataDao,
    refreshBroadcaster = refreshBroadcaster,
    reporting = reporting,
    purchaseState = purchaseState,
    tagMetadataSync = tagMetadataSync,
    syncAdapters = syncAdapters,
    isDark = context.resources.getBoolean(R.bool.is_dark),
    hasColorWheel = true,
    tagData = savedStateHandle.get<TagData>(EXTRA_TAG_DATA) ?: TagData(),
)
