package org.tasks.injection;

import com.todoroo.astrid.alarms.AlarmJobServiceTest;
import com.todoroo.astrid.dao.TaskDaoTests;
import com.todoroo.astrid.gtasks.GtasksIndentActionTest;
import com.todoroo.astrid.gtasks.GtasksListServiceTest;
import com.todoroo.astrid.gtasks.GtasksMetadataServiceTest;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdaterTest;
import com.todoroo.astrid.gtasks.GtasksTaskMovingTest;
import com.todoroo.astrid.model.TaskTest;
import com.todoroo.astrid.reminders.ReminderServiceTest;
import com.todoroo.astrid.repeats.NewRepeatTests;
import com.todoroo.astrid.repeats.RepeatTaskHelperTest;
import com.todoroo.astrid.service.QuickAddMarkupTest;
import com.todoroo.astrid.service.TitleParserTest;
import com.todoroo.astrid.subtasks.SubtasksHelperTest;
import com.todoroo.astrid.subtasks.SubtasksTestCase;
import com.todoroo.astrid.sync.NewSyncTestCase;

import org.tasks.jobs.BackupServiceTests;

import dagger.Component;

@ApplicationScope
@Component(modules = TestModule.class)
public interface TestComponent {

    GtasksMetadataServiceTest.GtasksMetadataServiceTestComponent plus(GtasksMetadataServiceTest.GtasksMetadataServiceTestModule gtasksMetadataServiceTestModule);

    void inject(GtasksIndentActionTest gtasksIndentActionTest);

    void inject(GtasksTaskMovingTest gtasksTaskMovingTest);

    void inject(GtasksListServiceTest gtasksListServiceTest);

    void inject(GtasksTaskListUpdaterTest gtasksTaskListUpdaterTest);

    void inject(ReminderServiceTest reminderServiceTest);

    void inject(TaskTest taskTest);

    void inject(TaskDaoTests taskDaoTests);

    void inject(NewSyncTestCase newSyncTestCase);

    void inject(SubtasksTestCase subtasksTestCase);

    void inject(SubtasksHelperTest subtasksHelperTest);

    void inject(QuickAddMarkupTest quickAddMarkupTest);

    void inject(TitleParserTest titleParserTest);

    void inject(NewRepeatTests newRepeatTests);

    void inject(BackupServiceTests backupServiceTests);

    void inject(AlarmJobServiceTest alarmServiceTest);

    void inject(RepeatTaskHelperTest repeatTaskHelperTest);
}
