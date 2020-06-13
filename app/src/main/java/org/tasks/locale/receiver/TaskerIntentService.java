package org.tasks.locale.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.Notifier;
import org.tasks.injection.ApplicationContext;
import org.tasks.injection.InjectingJobIntentService;
import org.tasks.injection.ServiceComponent;
import org.tasks.locale.bundle.ListNotificationBundle;
import org.tasks.locale.bundle.TaskCreationBundle;
import org.tasks.preferences.DefaultFilterProvider;
import timber.log.Timber;

public class TaskerIntentService extends InjectingJobIntentService {

  @Inject @ApplicationContext Context context;
  @Inject Notifier notifier;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject TaskerTaskCreator taskerTaskCreator;

  @Override
  protected void doWork(@NonNull Intent intent) {
    final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

    if (null == bundle) {
      Timber.e("%s is missing", com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); // $NON-NLS-1$
      return;
    }

    if (ListNotificationBundle.isBundleValid(bundle)) {
      Filter filter =
          defaultFilterProvider.getFilterFromPreference(
              bundle.getString(ListNotificationBundle.BUNDLE_EXTRA_STRING_FILTER));
      notifier.triggerFilterNotification(filter);
    } else if (TaskCreationBundle.isBundleValid(bundle)) {
      taskerTaskCreator.handle(new TaskCreationBundle(bundle));
    } else {
      Timber.e("Invalid bundle: %s", bundle);
    }
  }

  @Override
  protected void inject(ServiceComponent component) {
    component.inject(this);
  }
}
