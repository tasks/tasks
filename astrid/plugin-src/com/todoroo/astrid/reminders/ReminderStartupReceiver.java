package com.todoroo.astrid.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Service which handles jobs that need to be run when phone boots
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ReminderStartupReceiver extends BroadcastReceiver {

    static {
        AstridDependencyInjector.initialize();
    }

    // --- system startup

    @Autowired
    ExceptionService exceptionService;

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        try {
            ReminderService.getInstance().scheduleAllAlarms();
        } catch (Exception e) {
            DependencyInjectionService.getInstance().inject(this);
            exceptionService.reportError("reminder-startup", e); //$NON-NLS-1$
        }
    }
}
