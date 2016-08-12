package org.tasks.injection;

import com.todoroo.astrid.gtasks.GtasksIndentActionTest;
import com.todoroo.astrid.gtasks.GtasksListServiceTest;
import com.todoroo.astrid.gtasks.GtasksMetadataServiceTest;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdaterTest;
import com.todoroo.astrid.gtasks.GtasksTaskMovingTest;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = TestModule.class)
public interface TestComponent extends BaseTestComponent {

    GtasksMetadataServiceTest.GtasksMetadataServiceTestComponent plus(GtasksMetadataServiceTest.GtasksMetadataServiceTestModule gtasksMetadataServiceTestModule);

    void inject(GtasksIndentActionTest gtasksIndentActionTest);

    void inject(GtasksTaskMovingTest gtasksTaskMovingTest);

    void inject(GtasksListServiceTest gtasksListServiceTest);

    void inject(GtasksTaskListUpdaterTest gtasksTaskListUpdaterTest);
}
