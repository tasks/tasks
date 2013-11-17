package org.tasks.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViewsService;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ScrollableWidgetUpdateService extends RemoteViewsService {

    public static final String QUERY_ID = "org.tasks.widget.query_id";
    public static final String IS_DARK_THEME = "org.tasks.widget.is_dark_theme";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        if (intent == null) {
            return null;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        String queryId = extras.getString(QUERY_ID);
        boolean isDarkTheme = extras.getBoolean(IS_DARK_THEME);
        return new ScrollableViewsFactory(this, queryId, isDarkTheme);
    }
}
