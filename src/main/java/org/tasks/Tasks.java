package org.tasks;

import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.TeslaUnreadReceiver;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

@SuppressWarnings("UnusedDeclaration")
public class Tasks extends InjectingApplication {

    @Inject StartupService startupService;
    @Inject TaskDao taskDao;
    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject TaskService taskService;
    @Inject TagService tagService;
    @Inject Broadcaster broadcaster;
    @Inject Preferences preferences;
    @Inject Tracker tracker;
    @Inject FlavorSetup flavorSetup;
    @Inject BuildSetup buildSetup;
    @Inject ThemeCache themeCache;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        tracker.setTrackingEnabled(preferences.isTrackingEnabled());

        buildSetup.setup();
        flavorSetup.setup();

        teslaUnreadReceiver.setEnabled(preferences.getBoolean(R.string.p_tesla_unread_enabled, false));

        themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0)).setDefaultNightMode();

        startupService.onStartupApplication();
    }

    @Override
    protected void inject(ApplicationComponent component) {
        component.inject(this);
    }
}
