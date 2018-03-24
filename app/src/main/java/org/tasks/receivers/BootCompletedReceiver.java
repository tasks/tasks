package org.tasks.receivers;

import android.content.Context;
import android.content.Intent;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;
import timber.log.Timber;

public class BootCompletedReceiver extends InjectingBroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
      return;
    }

    Timber.d("onReceive(context, %s)", intent);
  }

  @Override
  protected void inject(BroadcastComponent component) {
    component.inject(this);
  }
}
