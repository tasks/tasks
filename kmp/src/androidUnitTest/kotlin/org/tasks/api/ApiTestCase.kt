package org.tasks.api

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PermissionInfo
import android.database.Cursor
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import androidx.room.Room
import com.todoroo.astrid.alarms.AlarmCalculator
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.repeats.RepeatTaskHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.tasks.analytics.Analytics
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
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@RunWith(RobolectricTestRunner::class)
abstract class ApiTestCase {
    protected val db: Database = Room
        .inMemoryDatabaseBuilder<Database>(RuntimeEnvironment.getApplication())
        .addCallback(Database.CALLBACK)
        .allowMainThreadQueries()
        .build()

    protected val caldavDao: CaldavDao by lazy { db.caldavDao() }

    protected lateinit var resolver: ContentResolver
    protected var listId: Long = 0
    private lateinit var listUuid: String

    private val context: Context get() = RuntimeEnvironment.getApplication()

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
            appPreferences = appPreferences,
            refreshBroadcaster = mock(),
            taskDeleter = taskDeleter,
        )
    }

    private val appPreferences: AppPreferences = mock {
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
            override val appPreferences = this@ApiTestCase.appPreferences
            override suspend fun currentLocation(): MapPosition? = null
            override fun addGeofences(geofence: MergedGeofence) = Unit
            override fun removeGeofences(place: Place) = Unit
        }
    }

    protected val taskFactory = TestTaskFactory()

    private val writer: ApiWriter by lazy {
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

    private val dependencies by lazy {
        object : TasksApiProvider.Dependencies {
            override val database = db
            override val queryEngine = ApiQueryEngine(db)
            override val writer = this@ApiTestCase.writer
            override val analytics: Analytics = mock()
        }
    }

    inner class TestTaskFactory : ApiTaskFactory {
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

    @Before
    fun setUp() {
        resolver = context.contentResolver
        declarePermissions(context.packageName)
        runBlockingTest {
            val account = CaldavAccount(
                uuid = UUIDHelper.newUUID(),
                accountType = CaldavAccount.TYPE_LOCAL,
                name = "Local",
            )
            caldavDao.insert(account)
            val calendar = CaldavCalendar(
                uuid = UUIDHelper.newUUID(),
                account = account.uuid,
                name = "My tasks",
            )
            caldavDao.insert(calendar)
            listUuid = calendar.uuid!!
            listId = caldavDao.getCalendarByUuid(listUuid)!!.id
        }
        Robolectric
            .buildContentProvider(TestApiProvider::class.java)
            .create(TasksContract.AUTHORITY)
            .get()
            .install(dependencies)
    }

    @After
    fun tearDown() {
        TestApiProvider.installed = null
        db.close()
    }

    class TestApiProvider : TasksApiProvider() {
        override val dependencies: Dependencies
            get() = installed ?: error("Dependencies not installed")

        fun install(dependencies: Dependencies) {
            installed = dependencies
        }

        companion object {
            internal var installed: Dependencies? = null
        }
    }

    @Suppress("DEPRECATION")
    protected fun declarePermissions(owner: String) {
        val packageManager = shadowOf(context.packageManager)
        listOf(TasksContract.PERMISSION_READ, TasksContract.PERMISSION_WRITE).forEach {
            packageManager.addPermissionInfo(
                PermissionInfo().apply {
                    name = it
                    packageName = owner
                }
            )
        }
    }

    protected fun <T> runBlockingTest(block: suspend () -> T): T = runBlocking { block() }

    protected fun day(days: Int): Long = currentTimeMillis() + days * 24L * 60 * 60 * 1000

    protected suspend fun filterFor(uid: String): CaldavFilter {
        val calendar = caldavDao.getCalendar(uid)!!
        return CaldavFilter(calendar = calendar, account = caldavDao.getAccountByUuid(calendar.account!!)!!)
    }

    protected fun uri(path: String, query: String = ""): Uri =
        "${TasksContract.CONTENT_URI}/$path$query".toUri()

    protected fun itemUri(path: String, id: Long): Uri =
        ContentUris.withAppendedId(uri(path), id)

    protected fun query(
        path: String,
        query: String = "",
        projection: Array<String>? = null,
    ): Cursor = resolver.query(uri(path, query), projection, null, null, null)!!

    protected fun insert(path: String, vararg values: Pair<String, Any?>): Long =
        ContentUris.parseId(resolver.insert(uri(path), contentValuesOf(*values))!!)

    protected fun update(
        path: String,
        id: Long,
        vararg values: Pair<String, Any?>,
        query: String = "",
    ): Int {
        val target = if (query.isEmpty()) itemUri(path, id) else "${itemUri(path, id)}$query".toUri()
        return resolver.update(target, contentValuesOf(*values), null, null)
    }

    protected fun delete(path: String, id: Long): Int = resolver.delete(itemUri(path, id), null, null)

    protected fun newTask(
        title: String = "task",
        vararg values: Pair<String, Any?>,
    ): Long = insert(TasksContract.Tasks.PATH, TasksContract.Tasks.TITLE to title, *values)

    protected fun Cursor.string(column: String): String =
        use { it.moveToFirst(); it.getString(it.getColumnIndexOrThrow(column)) }

    protected fun Cursor.long(column: String): Long =
        use { it.moveToFirst(); it.getLong(it.getColumnIndexOrThrow(column)) }

    protected fun Cursor.int(column: String): Int =
        use { it.moveToFirst(); it.getInt(it.getColumnIndexOrThrow(column)) }

    protected fun Cursor.longs(column: String): List<Long> = use { c ->
        val index = c.getColumnIndexOrThrow(column)
        buildList { while (c.moveToNext()) add(c.getLong(index)) }
    }

    protected fun Cursor.strings(column: String): List<String> = use { c ->
        val index = c.getColumnIndexOrThrow(column)
        buildList { while (c.moveToNext()) add(c.getString(index)) }
    }

    protected fun Cursor.total(): Int =
        use { it.extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT, -1) }

    protected fun Cursor.rows(): Int = use { it.count }

    protected suspend fun readOnlyList(): Long = newList("Shared", CaldavCalendar.ACCESS_READ_ONLY)

    protected fun setAccountError(error: String) = runBlockingTest {
        caldavDao.update(account().apply { this.error = error })
    }

    protected suspend fun account(): CaldavAccount =
        caldavDao.getAccountByUuid(caldavDao.getCalendarById(listId)!!.account!!)!!

    protected suspend fun newList(
        name: String,
        access: Int = CaldavCalendar.ACCESS_OWNER,
    ): Long {
        val calendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            account = account().uuid,
            name = name,
            access = access,
        )
        caldavDao.insert(calendar)
        return caldavDao.getCalendarByUuid(calendar.uuid!!)!!.id
    }

    protected suspend fun remoteList(): Long {
        val account = CaldavAccount(
            uuid = UUIDHelper.newUUID(),
            accountType = CaldavAccount.TYPE_CALDAV,
            name = "Remote",
            url = "https://example.com/",
            username = "user",
        )
        caldavDao.insert(account)
        val calendar = CaldavCalendar(
            uuid = UUIDHelper.newUUID(),
            account = account.uuid,
            name = "Remote list",
            url = "https://example.com/remote/",
        )
        caldavDao.insert(calendar)
        return caldavDao.getCalendarByUuid(calendar.uuid!!)!!.id
    }
}
