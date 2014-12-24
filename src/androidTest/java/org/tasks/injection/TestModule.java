package org.tasks.injection;

import android.content.Context;

import org.tasks.scheduling.BackupServiceTests;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDaoTests;
import com.todoroo.astrid.dao.TaskDaoTests;
import com.todoroo.astrid.gtasks.GtasksIndentActionTest;
import com.todoroo.astrid.gtasks.GtasksListServiceTest;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdaterTest;
import com.todoroo.astrid.gtasks.GtasksTaskMovingTest;
import com.todoroo.astrid.model.TaskTest;
import com.todoroo.astrid.provider.Astrid3ProviderTests;
import com.todoroo.astrid.reminders.ReminderServiceTest;
import com.todoroo.astrid.repeats.NewRepeatTests;
import com.todoroo.astrid.service.QuickAddMarkupTest;
import com.todoroo.astrid.service.TitleParserTest;
import com.todoroo.astrid.subtasks.SubtasksHelperTest;
import com.todoroo.astrid.subtasks.SubtasksMovingTest;
import com.todoroo.astrid.subtasks.SubtasksTestCase;
import com.todoroo.astrid.sync.NewSyncTestCase;
import com.todoroo.astrid.sync.SyncModelTest;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = {
        BackupServiceTests.class,
        MetadataDaoTests.class,
        TaskDaoTests.class,
        GtasksIndentActionTest.class,
        GtasksTaskListUpdaterTest.class,
        GtasksTaskMovingTest.class,
        Astrid3ProviderTests.class,
        NewRepeatTests.class,
        QuickAddMarkupTest.class,
        TitleParserTest.class,
        SubtasksTestCase.class,
        NewSyncTestCase.class,
        TaskTest.class,
        ReminderServiceTest.class,
        SubtasksHelperTest.class,
        SubtasksMovingTest.class,
        SyncModelTest.class,
        GtasksListServiceTest.class
})
public class TestModule {
    private Context context;

    public TestModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    public Database getDatabase() {
        return new Database(context) {
            @Override
            public String getName() {
                return "databasetest";
            }
        };
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }
}
