package org.tasks.injection;

import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class InjectingAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        ((Injector) context.getApplicationContext())
                .getObjectGraph()
                .plus(new BroadcastModule())
                .inject(this);

        super.onReceive(context, intent);
    }
}
