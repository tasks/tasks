package org.tasks.injection;

import android.content.Context;

import org.tasks.ErrorReportingSingleThreadExecutor;
import org.tasks.analytics.Tracker;
import org.tasks.ui.CheckBoxes;
import org.tasks.ui.WidgetCheckBoxes;

import java.util.concurrent.Executor;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.tasks.ui.CheckBoxes.newCheckBoxes;
import static org.tasks.ui.WidgetCheckBoxes.newWidgetCheckBoxes;

@Module
public class ApplicationModule {
    private Context context;

    public ApplicationModule(Context context) {
        this.context = context.getApplicationContext();
    }

    @Provides
    @ForApplication
    public Context getApplicationContext() {
        return context;
    }

    @Provides
    @Singleton
    @Named("iab-executor")
    public Executor getIabExecutor(Tracker tracker) {
        return new ErrorReportingSingleThreadExecutor("iab-executor", tracker);
    }

    @Provides
    @Singleton
    public CheckBoxes getCheckBoxes() {
        return newCheckBoxes(context);
    }

    @Provides
    @Singleton
    public WidgetCheckBoxes getWidgetCheckBoxes(CheckBoxes checkBoxes) {
        return newWidgetCheckBoxes(checkBoxes);
    }
}
