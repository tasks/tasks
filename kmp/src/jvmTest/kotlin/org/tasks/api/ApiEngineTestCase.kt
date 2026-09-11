package org.tasks.api

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.data.MergedGeofence
import org.tasks.data.TaskMover
import org.tasks.data.TaskSaver
import org.tasks.data.UUIDHelper
import org.tasks.data.dao.CaldavDao
import org.tasks.data.db.Database
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.preferences.AppPreferences
import org.tasks.reminders.Random
import org.tasks.service.TaskCleanup
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter

abstract class ApiEngineTestCase {
    protected val db: Database = Room
        .inMemoryDatabaseBuilder<Database>()
        .setDriver(BundledSQLiteDriver())
        .addCallback(Database.CALLBACK)
        .build()

    protected val caldavDao: CaldavDao by lazy { db.caldavDao() }

    protected var listId: Long = 0
    private lateinit var listUuid: String

    protected val appPreferences: AppPreferences = mock {
        onBlocking { addTasksToTop() } doReturn false
        onBlocking { locationUpdateIntervalMinutes() } doReturn 15
        onBlocking { isDefaultDueTimeEnabled() } doReturn false
        onBlocking { defaultAlarms() } doReturn emptyList()
        onBlocking { defaultDueTime() } doReturn 0
        onBlocking { isCurrentlyQuietHours() } doReturn false
    }

    private val locationService: LocationService by lazy {
        object : LocationService {
            override val locationDao = db.locationDao()
            override val appPreferences = this@ApiEngineTestCase.appPreferences
            override suspend fun currentLocation(): MapPosition? = null
            override fun addGeofences(geofence: MergedGeofence) = Unit
            override fun removeGeofences(place: Place) = Unit
        }
    }

    private val taskSaver: TaskSaver by lazy {
        TaskSaver(
            taskDao = db.taskDao(),
            refreshBroadcaster = mock(),
            notifier = mock(),
            locationService = locationService,
            timerPlugin = mock(),
            backgroundWork = mock(),
            caldavDao = caldavDao,
        )
    }

    private val alarmService: AlarmService by lazy {
        AlarmService(
            alarmDao = db.alarmDao(),
            taskDao = db.taskDao(),
            dirtyDao = db.dirtyDao(),
            refreshBroadcaster = mock(),
            notifier = mock(),
            alarmCalculator = AlarmCalculator(Random()),
            preferences = appPreferences,
        )
    }

    private val taskCompleter: TaskCompleter by lazy {
        TaskCompleter(
            taskDao = db.taskDao(),
            taskSaver = taskSaver,
            notifier = mock(),
            refreshBroadcaster = mock(),
            repeatTaskHelper = RepeatTaskHelper(
                calendarHelper = mock(),
                alarmService = alarmService,
                taskSaver = taskSaver,
            ),
            caldavDao = caldavDao,
            calendarHelper = mock(),
            completionDao = db.completionDao(),
            soundPlayer = mock(),
        )
    }

    private val taskDeleter: TaskDeleter by lazy {
        TaskDeleter(
            deletionDao = db.deletionDao(),
            taskDao = db.taskDao(),
            caldavDao = caldavDao,
            refreshBroadcaster = mock(),
            vtodoCache = mock(),
            tasksPreferences = mock(),
            taskCleanup = object : TaskCleanup {
                override suspend fun cleanup(tasks: List<Long>) = db.deletionDao().purgeDeleted()
            },
        )
    }

    private val taskMover: TaskMover by lazy {
        TaskMover(
            taskDao = db.taskDao(),
            caldavDao = caldavDao,
            googleTaskDao = db.googleTaskDao(),
            dirtyDao = db.dirtyDao(),
            appPreferences = appPreferences,
            refreshBroadcaster = mock(),
            taskDeleter = taskDeleter,
        )
    }

    private val taskFactory = object : ApiTaskFactory {
        override suspend fun defaultList(): CaldavFilter = filterFor(listUuid)

        override suspend fun create(
            title: String,
            list: CaldavFilter,
            configure: (Task) -> Unit,
        ): Task {
            val task = Task(title = title, remoteId = UUIDHelper.newUUID())
            configure(task)
            db.taskDao().createNew(task)
            caldavDao.insert(
                task = task,
                caldavTask = CaldavTask(task = task.id, calendar = list.uuid),
                addToTop = false,
            )
            taskSaver.save(task, null)
            return task
        }
    }

    protected val writer: ApiWriter by lazy {
        ApiWriter(
            apiDao = db.apiDao(),
            taskDao = db.taskDao(),
            caldavDao = caldavDao,
            tagDao = db.tagDao(),
            tagDataDao = db.tagDataDao(),
            alarmDao = db.alarmDao(),
            locationDao = db.locationDao(),
            taskFactory = taskFactory,
            taskSaver = taskSaver,
            taskCompleter = taskCompleter,
            taskMover = taskMover,
            taskDeleter = taskDeleter,
            alarmService = alarmService,
            locationService = locationService,
            listManager = ApiListManager(
                caldavDao = caldavDao,
                taskDeleter = taskDeleter,
                caldavClientProvider = mock(),
                etebaseClientProvider = mock(),
                microsoftClientProvider = mock(),
                gtasksInvoker = { mock() },
            ),
        )
    }

    protected val engine: ApiQueryEngine by lazy { ApiQueryEngine(db) }

    @Before
    fun seed() = runBlocking {
        val account = CaldavAccount(uuid = UUIDHelper.newUUID(), accountType = CaldavAccount.TYPE_LOCAL, name = "Local")
        caldavDao.insert(account)
        val calendar = CaldavCalendar(uuid = UUIDHelper.newUUID(), account = account.uuid, name = "My tasks")
        caldavDao.insert(calendar)
        listUuid = calendar.uuid!!
        listId = caldavDao.getCalendarByUuid(listUuid)!!.id
    }

    @After
    fun closeDb() = db.close()

    protected suspend fun accountId(): Long = engine.findAccounts(AccountQuery()).rows.single().id

    protected suspend fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        dueAllDay: Boolean? = null,
    ): Long = engine.createTasks(
        writer,
        listOf(TaskWrite(title = title, notes = notes, due = dueDate, dueAllDay = dueAllDay)),
    ).ids.single()

    private suspend fun filterFor(uid: String): CaldavFilter {
        val calendar = caldavDao.getCalendar(uid)!!
        return CaldavFilter(calendar = calendar, account = caldavDao.getAccountByUuid(calendar.account!!)!!)
    }
}
