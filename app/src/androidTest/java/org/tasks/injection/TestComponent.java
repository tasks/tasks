package org.tasks.injection;

import com.todoroo.astrid.alarms.AlarmJobServiceTest;
import com.todoroo.astrid.dao.TaskDaoTests;
import com.todoroo.astrid.gtasks.GtasksListServiceTest;
import com.todoroo.astrid.gtasks.GtasksMetadataServiceTest;
import com.todoroo.astrid.model.TaskTest;
import com.todoroo.astrid.reminders.ReminderServiceTest;
import com.todoroo.astrid.repeats.RepeatTaskHelperTest;
import com.todoroo.astrid.service.QuickAddMarkupTest;
import com.todoroo.astrid.service.TitleParserTest;
import com.todoroo.astrid.subtasks.SubtasksHelperTest;
import com.todoroo.astrid.subtasks.SubtasksMovingTest;
import com.todoroo.astrid.sync.NewSyncTestCase;
import dagger.Component;
import org.tasks.data.DeletionDaoTests;
import org.tasks.data.GoogleTaskDaoTests;
import org.tasks.jobs.BackupServiceTests;

@ApplicationScope
@Component(modules = TestModule.class)
public interface TestComponent extends ApplicationComponent {

  void inject(GtasksListServiceTest gtasksListServiceTest);

  void inject(ReminderServiceTest reminderServiceTest);

  void inject(TaskTest taskTest);

  void inject(TaskDaoTests taskDaoTests);

  void inject(NewSyncTestCase newSyncTestCase);

  void inject(SubtasksMovingTest subtasksTestCase);

  void inject(SubtasksHelperTest subtasksHelperTest);

  void inject(QuickAddMarkupTest quickAddMarkupTest);

  void inject(TitleParserTest titleParserTest);

  void inject(BackupServiceTests backupServiceTests);

  void inject(AlarmJobServiceTest alarmServiceTest);

  void inject(RepeatTaskHelperTest repeatTaskHelperTest);

  void inject(GtasksMetadataServiceTest gtasksMetadataServiceTest);

  void inject(DeletionDaoTests deletionDaoTests);

  void inject(GoogleTaskDaoTests googleTaskDaoTests);
}
