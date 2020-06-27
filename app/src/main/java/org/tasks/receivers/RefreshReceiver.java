package org.tasks.receivers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDaoBlocking;
import com.todoroo.astrid.provider.Astrid2TaskProvider;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.provider.TasksContentProvider;
import timber.log.Timber;

@AndroidEntryPoint
public class RefreshReceiver extends InjectingJobIntentService {

  @Inject @ApplicationContext Context context;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskDaoBlocking taskDao;
  @Inject Preferences preferences;

  @Override
  protected void doWork(@NonNull Intent intent) {
    if (preferences.getBoolean(R.string.p_badges_enabled, true)) {
      Filter badgeFilter = defaultFilterProvider.getBadgeFilter();
      ShortcutBadger.applyCount(context, taskDao.count(badgeFilter));
    }

    try {
      ContentResolver cr = context.getContentResolver();
      cr.notifyChange(TasksContentProvider.CONTENT_URI, null);
      cr.notifyChange(Astrid2TaskProvider.CONTENT_URI, null);
    } catch (Exception e) {
      Timber.e(e);
    }
  }
}
