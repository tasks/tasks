package org.tasks.injection;

import android.app.Application;
import android.content.Context;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.backup.TasksXmlExporter;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.QuickAddBar;

import org.tasks.widget.ScrollableViewsFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Module(
        injects = {
                ScrollableViewsFactory.class,
                QuickAddBar.class,
                EditTitleControlSet.class,
                FilesControlSet.class,
                TagsControlSet.class,
                AlarmControlSet.class,
                FilterAdapter.class,
                TimerFilterExposer.class,
                GCalControlSet.class,
                TimerActionControlSet.class,
                CustomFilterExposer.class,
                GtasksFilterExposer.class,
                TagFilterExposer.class,
                TasksXmlExporter.class,
                TasksXmlImporter.class
        }
)
public class TasksModule {

    private final Context context;

    public static TasksModule newTasksModule(Application application) {
        return new TasksModule(application.getApplicationContext());
    }

    TasksModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }

    @Qualifier
    @Target({FIELD, PARAMETER, METHOD})
    @Documented
    @Retention(RUNTIME)
    public @interface ForApplication {
    }
}
