package org.tasks.injection;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public abstract class InjectingAppWidgetProvider extends AppWidgetProvider {

  @Override
  public void onReceive(Context context, Intent intent) {
    inject(
        ((InjectingApplication) context.getApplicationContext())
            .getComponent()
            .plus(new BroadcastModule()));

    super.onReceive(context, intent);
  }

  protected abstract void inject(BroadcastComponent component);
}
