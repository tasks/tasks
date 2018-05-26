package org.tasks.injection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class InjectingBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    inject(
        ((InjectingApplication) context.getApplicationContext())
            .getComponent()
            .plus(new BroadcastModule()));
  }

  protected abstract void inject(BroadcastComponent component);
}
