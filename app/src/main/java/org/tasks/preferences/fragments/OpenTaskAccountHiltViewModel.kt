package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.data.dao.CaldavDao
import org.tasks.viewmodel.OpenTaskAccountViewModel
import javax.inject.Inject

@HiltViewModel
class OpenTaskAccountHiltViewModel @Inject constructor(
    caldavDao: CaldavDao,
) : OpenTaskAccountViewModel(
    caldavDao = caldavDao,
)
