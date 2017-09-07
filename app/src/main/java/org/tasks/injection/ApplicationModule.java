package org.tasks.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import org.tasks.ErrorReportingSingleThreadExecutor;
import org.tasks.analytics.Tracker;
import org.tasks.db.AppDatabase;
import org.tasks.locale.Locale;
import org.tasks.notifications.NotificationDao;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.WidgetCheckBoxes;

import java.util.concurrent.Executor;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

import static org.tasks.ui.CheckBoxes.newCheckBoxes;
import static org.tasks.ui.WidgetCheckBoxes.newWidgetCheckBoxes;

@Module
public class ApplicationModule {
    private final Context context;

    public ApplicationModule(Context context) {
        this.context = context;
    }

    @Provides
    public Locale getLocale() {
        return Locale.getInstance(context);
    }

    @Provides
    @ForApplication
    public Context getApplicationContext() {
        return context;
    }

    @Provides
    @ApplicationScope
    @Named("iab-executor")
    public Executor getIabExecutor(Tracker tracker) {
        return new ErrorReportingSingleThreadExecutor("iab-executor", tracker);
    }

    @Provides
    @ApplicationScope
    public CheckBoxes getCheckBoxes() {
        return newCheckBoxes(context);
    }

    @Provides
    @ApplicationScope
    public WidgetCheckBoxes getWidgetCheckBoxes(CheckBoxes checkBoxes) {
        return newWidgetCheckBoxes(checkBoxes);
    }

    @Provides
    @ApplicationScope
    public AppDatabase getAppDatabase() {
        return Room.databaseBuilder(context, AppDatabase.class, "app-database").build();
    }

    @Provides
    public NotificationDao getNotificationDao(AppDatabase appDatabase) {
        return appDatabase.notificationDao();
    }
}
