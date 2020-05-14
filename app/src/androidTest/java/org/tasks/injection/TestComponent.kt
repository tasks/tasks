package org.tasks.injection

import com.todoroo.astrid.adapter.CaldavTaskAdapterTest
import com.todoroo.astrid.alarms.AlarmJobServiceTest
import com.todoroo.astrid.dao.TaskDaoTests
import com.todoroo.astrid.gtasks.GtasksListServiceTest
import com.todoroo.astrid.gtasks.GtasksMetadataServiceTest
import com.todoroo.astrid.model.TaskTest
import com.todoroo.astrid.reminders.ReminderServiceTest
import com.todoroo.astrid.repeats.RepeatTaskHelperTest
import com.todoroo.astrid.service.QuickAddMarkupTest
import com.todoroo.astrid.service.TaskMoverTest
import com.todoroo.astrid.service.TitleParserTest
import com.todoroo.astrid.subtasks.SubtasksHelperTest
import com.todoroo.astrid.subtasks.SubtasksMovingTest
import com.todoroo.astrid.sync.NewSyncTestCase
import dagger.Component
import org.tasks.data.*
import org.tasks.jobs.BackupServiceTests

@ApplicationScope
@Component(modules = [TestModule::class])
interface TestComponent : ApplicationComponent {
    fun inject(gtasksListServiceTest: GtasksListServiceTest)
    fun inject(reminderServiceTest: ReminderServiceTest)
    fun inject(taskTest: TaskTest)
    fun inject(taskDaoTests: TaskDaoTests)
    fun inject(newSyncTestCase: NewSyncTestCase)
    fun inject(subtasksTestCase: SubtasksMovingTest)
    fun inject(subtasksHelperTest: SubtasksHelperTest)
    fun inject(quickAddMarkupTest: QuickAddMarkupTest)
    fun inject(titleParserTest: TitleParserTest)
    fun inject(backupServiceTests: BackupServiceTests)
    fun inject(alarmServiceTest: AlarmJobServiceTest)
    fun inject(repeatTaskHelperTest: RepeatTaskHelperTest)
    fun inject(gtasksMetadataServiceTest: GtasksMetadataServiceTest)
    fun inject(deletionDaoTests: DeletionDaoTests)
    fun inject(googleTaskDaoTests: GoogleTaskDaoTests)
    fun inject(tagDataDaoTest: TagDataDaoTest)
    fun inject(caldavDaoTests: CaldavDaoTests)
    fun inject(taskMoverTest: TaskMoverTest)
    fun inject(locationDaoTest: LocationDaoTest)
    fun inject(googleTaskListDaoTest: GoogleTaskListDaoTest)
    fun inject(caldavTaskAdapterTest: CaldavTaskAdapterTest)
}