package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.injection.ApplicationScope
import org.tasks.preferences.Preferences
import org.tasks.viewmodel.NavigationDrawerViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerHiltViewModel @Inject constructor(
    preferences: Preferences,
    refreshBroadcaster: RefreshBroadcaster,
    @ApplicationScope persistenceScope: CoroutineScope,
) : NavigationDrawerViewModel(
    appPreferences = preferences,
    refreshBroadcaster = refreshBroadcaster,
    persistenceScope = persistenceScope,
)
