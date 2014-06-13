package org.tasks.injection;

import android.app.Service;
import android.content.Context;

import com.todoroo.astrid.backup.BackupService;
import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.reminders.ReminderSchedulingService;
import com.todoroo.astrid.widget.WidgetUpdateService;

import org.tasks.widget.ScrollableWidgetUpdateService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = {
        GtasksBackgroundService.class,
        ReminderSchedulingService.class,
        ScrollableWidgetUpdateService.class,
        WidgetUpdateService.class,
        BackupService.class
})
public class ServiceModule {

    private final Context context;

    public ServiceModule(Service service) {
        context = service.getApplicationContext();
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getContext() {
        return context;
    }
}
