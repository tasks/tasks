package org.tasks.preferences.fragments

import dagger.hilt.android.lifecycle.HiltViewModel
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.NavigationDrawerViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationDrawerHiltViewModel @Inject constructor(
    tasksPreferences: TasksPreferences,
) : NavigationDrawerViewModel(
    tasksPreferences = tasksPreferences,
)
