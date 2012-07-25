/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.todoroo.astrid.api.R;

/**
 * Exception handling utility class - reports and logs errors
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ExceptionService {

    @Autowired
    public ErrorReporter[] errorReporters;

    public ExceptionService() {
        DependencyInjectionService.getInstance().inject(this);
        if(errorReporters == null) {
            errorReporters = new ErrorReporter[] {
                    new AndroidLogReporter()
            };
        }
    }

    /**
     * Report the error via registered error handlers
     *
     * @param name Internal error name. Not displayed to user
     * @param error Exception encountered. Message will be displayed to user
     */
    public void reportError(String name, Throwable error) {
        if(errorReporters == null)
            return;

        for(ErrorReporter reporter : errorReporters) {
            try {
                reporter.handleError(name, error);
            } catch (Exception e) {
                Log.e("astrid-exception-service", "Exception handling error", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /**
     * Display error dialog if context is activity and report error
     *
     * @param context Application Context
     * @param name Internal error name. Not displayed to user
     * @param error Exception encountered. Message will be displayed to user
     */
    public void displayAndReportError(final Context context, String name, Throwable error) {
        if(context instanceof Activity) {
            final String messageToDisplay;

            // pretty up the message when displaying to user
            if(error == null)
                messageToDisplay = context.getString(R.string.DLG_error_generic);
            else
                messageToDisplay = context.getString(R.string.DLG_error, error);

            ((Activity)context).runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        new AlertDialog.Builder(context)
                        .setTitle(R.string.DLG_error_title)
                        .setMessage(messageToDisplay)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                    } catch (Exception e) {
                        // suppress errors during dialog creation
                    }
                }
            });
        }

        reportError(name, error);
    }

    /**
     * Error reporter interface
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public interface ErrorReporter {
        public void handleError(String name, Throwable error);
    }

    /**
     * AndroidLogReporter reports errors to LogCat
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class AndroidLogReporter implements ErrorReporter {

        /**
         * Report the error to the logs
         *
         * @param name
         * @param error
         */
        public void handleError(String name, Throwable error) {
            String tag = null;
            if(ContextManager.getContext() != null) {
                PackageManager pm = ContextManager.getContext().getPackageManager();
                try {
                    String appName = pm.getApplicationInfo(ContextManager.getContext().
                            getPackageName(), 0).loadLabel(pm).toString();
                    tag = appName + "-" +  name; //$NON-NLS-1$
                } catch (NameNotFoundException e) {
                    // give up
                }
            }

            if(tag == null)
                tag = "unknown-" + name; //$NON-NLS-1$

            if(error == null)
                Log.e(tag, "Exception: " + name); //$NON-NLS-1$
            else
                Log.e(tag, error.toString(), error);
        }
    }

    /**
     * Uncaught exception handler uses the exception utilities class to
     * report errors
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class TodorooUncaughtExceptionHandler implements UncaughtExceptionHandler {
        private final UncaughtExceptionHandler defaultUEH;

        @Autowired
        protected ExceptionService exceptionService;

        public TodorooUncaughtExceptionHandler() {
            defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            DependencyInjectionService.getInstance().inject(this);
        }

        public void uncaughtException(Thread thread, Throwable ex) {
            if(exceptionService != null)
                exceptionService.reportError("uncaught", ex); //$NON-NLS-1$
            defaultUEH.uncaughtException(thread, ex);
        }
    }

}

