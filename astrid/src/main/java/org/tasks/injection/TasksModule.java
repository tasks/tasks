package org.tasks.injection;

import android.app.Application;
import android.content.Context;

import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.ui.QuickAddBar;

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

@Module(injects = {
        QuickAddBar.class,
        TimerFilterExposer.class,
        CustomFilterExposer.class,
        GtasksFilterExposer.class,
        TagFilterExposer.class
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
