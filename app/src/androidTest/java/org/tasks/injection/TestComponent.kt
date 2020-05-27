package org.tasks.injection

import com.todoroo.astrid.adapter.*
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
    fun inject(tests: GtasksListServiceTest)
    fun inject(tests: ReminderServiceTest)
    fun inject(tests: TaskTest)
    fun inject(tests: TaskDaoTests)
    fun inject(tests: NewSyncTestCase)
    fun inject(tests: SubtasksMovingTest)
    fun inject(tests: SubtasksHelperTest)
    fun inject(tests: QuickAddMarkupTest)
    fun inject(tests: TitleParserTest)
    fun inject(tests: BackupServiceTests)
    fun inject(tests: AlarmJobServiceTest)
    fun inject(tests: RepeatTaskHelperTest)
    fun inject(tests: GtasksMetadataServiceTest)
    fun inject(tests: DeletionDaoTests)
    fun inject(tests: GoogleTaskDaoTests)
    fun inject(tests: TagDataDaoTest)
    fun inject(tests: CaldavDaoTests)
    fun inject(tests: TaskMoverTest)
    fun inject(tests: LocationDaoTest)
    fun inject(tests: GoogleTaskListDaoTest)
    fun inject(tests: CaldavTaskAdapterTest)
    fun inject(tests: ManualGoogleTaskQueryTest)
    fun inject(tests: CaldavDaoShiftTests)
    fun inject(tests: CaldavManualSortTaskAdapterTest)
    fun inject(tests: GoogleTaskManualSortAdapterTest)
    fun inject(tests: OfflineSubtaskTest)
    fun inject(tests: NonRecursiveQueryTest)
}
