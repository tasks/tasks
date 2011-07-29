package com.localytics.android;

import android.util.Log;

/**
 * Exception handler for background threads used by the Localytics library.
 * <p>
 * Analytics are secondary to any other functions performed by an app, which means that analytics should never cause an app to
 * crash. This handler therefore suppresses all uncaught exceptions from the Localytics library.
 */
/* package */final class ExceptionHandler implements Thread.UncaughtExceptionHandler
{
    public void uncaughtException(final Thread thread, final Throwable throwable)
    {
        /*
         * Wrap all the work done by the exception handler in a try-catch. It would be ironic if this exception handler itself
         * caused the parent process to crash.
         */
        try
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Localytics library threw an uncaught exception", throwable); //$NON-NLS-1$
            }

            // TODO: Upload uncaught exceptions so that we can fix them
        }
        catch (final Exception e)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Exception handler threw an exception", e); //$NON-NLS-1$
            }
        }
    }
}