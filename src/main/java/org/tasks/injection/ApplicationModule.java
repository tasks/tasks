package org.tasks.injection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.tasks.ErrorReportingSingleThreadExecutor;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.locale.LocaleUtils;
import org.tasks.themes.ThemeCache;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String language = prefs.getString(context.getString(R.string.p_language), null);
        LocaleUtils.setLocale(language);
        this.context = LocaleUtils.createConfigurationContext(context.getApplicationContext());
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

    @Provides
    @Singleton
    public ThemeCache getThemeCache() {
        return new ThemeCache(context);
    }
}
