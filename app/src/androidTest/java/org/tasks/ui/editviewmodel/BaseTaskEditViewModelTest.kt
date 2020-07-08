package org.tasks.ui.editviewmodel

import android.content.Context
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.Database
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.tasks.calendars.CalendarEventProvider
import org.tasks.injection.InjectingTestCase
import org.tasks.location.GeofenceApi
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.PermissivePermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditViewModel
import javax.inject.Inject

open class BaseTaskEditViewModelTest : InjectingTestCase() {
    @ApplicationContext @Inject lateinit var context: Context
    @Inject lateinit var db: Database
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var taskDeleter: TaskDeleter
    @Inject lateinit var timerPlugin: TimerPlugin
    @Inject lateinit var calendarEventProvider: CalendarEventProvider
    @Inject lateinit var gCalHelper: GCalHelper
    @Inject lateinit var taskMover: TaskMover
    @Inject lateinit var geofenceApi: GeofenceApi
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var alarmService: AlarmService

    protected lateinit var viewModel: TaskEditViewModel

    @Before
    override fun setUp() {
        super.setUp()
        viewModel = TaskEditViewModel(
                context,
                taskDao,
                taskDeleter,
                timerPlugin,
                PermissivePermissionChecker(context),
                calendarEventProvider,
                gCalHelper,
                taskMover,
                db.locationDao,
                geofenceApi,
                db.tagDao,
                db.tagDataDao,
                preferences,
                defaultFilterProvider,
                db.googleTaskDao,
                db.caldavDao,
                taskCompleter,
                db.alarmDao,
                alarmService)
    }

    protected fun save(): Boolean = runBlocking(Dispatchers.Main) {
        viewModel.save()
    }
}