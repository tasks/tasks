package org.tasks.injection;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDaoTests;
import com.todoroo.astrid.model.TaskTest;
import com.todoroo.astrid.reminders.ReminderServiceTest;
import com.todoroo.astrid.repeats.NewRepeatTests;
import com.todoroo.astrid.service.QuickAddMarkupTest;
import com.todoroo.astrid.service.TitleParserTest;
import com.todoroo.astrid.subtasks.SubtasksHelperTest;
import com.todoroo.astrid.subtasks.SubtasksTestCase;
import com.todoroo.astrid.sync.NewSyncTestCase;
import com.todoroo.astrid.alarms.AlarmJobServiceTest;
import org.tasks.data.DeletionDaoTests;
import com.todoroo.astrid.repeats.RepeatTaskHelperTest;
import dagger.Component;
import org.tasks.jobs.BackupServiceTests;

@ApplicationScope
@Component(modules = TestModule.class)
public interface TestComponent {

  Database getDatabase();

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

  void inject(AlarmJobServiceTest alarmJobServiceTest);

  void inject(DeletionDaoTests deletionDaoTests);

  void inject(RepeatTaskHelperTest repeatTaskHelperTest);

}
