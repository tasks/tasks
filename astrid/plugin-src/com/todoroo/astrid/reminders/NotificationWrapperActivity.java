package com.todoroo.astrid.reminders;

import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.AstridWrapperActivity;
import com.todoroo.astrid.service.ThemeService;

public class NotificationWrapperActivity extends AstridWrapperActivity {

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.AstridWrapperActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_wrapper_activity);
    }

}
