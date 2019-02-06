package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.ServiceComponent;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

public class RefreshReceiver extends InjectingJobIntentService {

  @Inject @ForApplication Context context;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskDao taskDao;
  @Inject Preferences preferences;

  @Override
  protected void doWork(@Nonnull Intent intent) {
    if (preferences.getBoolean(R.string.p_badges_enabled, true)) {
      Filter badgeFilter = defaultFilterProvider.getBadgeFilter();
      ShortcutBadger.applyCount(context, taskDao.count(badgeFilter));
    }

    Astrid2TaskProvider.notifyDatabaseModification(context);
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
