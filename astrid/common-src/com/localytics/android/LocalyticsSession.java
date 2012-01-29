// @formatter:off
/*
 * LocalyticsSession.java Copyright (C) 2011 Char Software Inc., DBA Localytics This code is provided under the Localytics
 * Modified BSD License. A copy of this license has been distributed in a file called LICENSE with this source code. Please visit
 * www.localytics.com for more information.
 */
// @formatter:on

package com.localytics.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest.permission;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.localytics.android.JsonObjects.BlobHeader;
import com.localytics.android.LocalyticsProvider.ApiKeysDbColumns;
import com.localytics.android.LocalyticsProvider.AttributesDbColumns;
import com.localytics.android.LocalyticsProvider.EventHistoryDbColumns;
import com.localytics.android.LocalyticsProvider.EventsDbColumns;
import com.localytics.android.LocalyticsProvider.SessionsDbColumns;
import com.localytics.android.LocalyticsProvider.UploadBlobEventsDbColumns;
import com.localytics.android.LocalyticsProvider.UploadBlobsDbColumns;

/**
 * This class manages creating, collecting, and uploading a Localytics session. Please see the following guides for information on
 * how to best use this library, sample code, and other useful information:
 * <ul>
 * <li><a href="http://wiki.localytics.com/index.php?title=Developer's_Integration_Guide">Main Developer's Integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_2_Minute_Integration">Android 2 minute integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_Integration_Guide">Android Integration Guide</a></li>
 * </ul>
 * <p>
 * Permissions required:
 * <ul>
 * <li>{@link permission#INTERNET}</li> - Necessary to upload data to the webservice.</li>
 * </ul>
 * Permissions recommended:
 * <ul>
 * <li>{@link permission#ACCESS_WIFI_STATE}</li> - Without this users connecting via Wi-Fi will show up as having a connection
 * type of 'unknown' on the webservice</li>
 * </ul>
 * <p>
 * This library will create a database called "com.android.localytics.sqlite" within the host application's
 * {@link Context#getDatabasePath(String)} directory. For security, this file directory will be created
 * {@link Context#MODE_PRIVATE}. The host application must not modify this database file. If the host application implements a
 * backup/restore mechanism, such as {@code android.app.backup.BackupManager}, the host application should not worry about backing
 * up the data in the Localytics database.
 * </p>
 * <p>
 * This library is thread-safe but is not multi-process safe. Unless the application explicitly uses different process attributes
 * in the Android Manifest, this is not an issue.
 * </p>
 * <strong>Best Practices</strong>
 * <ul>
 * <li>Instantiate and open a {@link LocalyticsSession} object in {@code Activity#onCreate(Bundle)}. This will cause every new
 * Activity displayed to reconnect to any running session.</li>
 * <li>Consider also performing {@link #upload()} in {@code Activity#onCreate(Bundle)}. This makes it more likely for the upload
 * to complete before the Activity is finished, and also causes the upload to start before the user has a chance to begin any data
 * intensive actions of his own.</li>
 * <li>Close the session in {@code Activity#onPause()}. Based on the Activity lifecycle documentation, this is the last
 * terminating method which is guaranteed to be called. The final call to {@link #close()} is the only one considered, so don't
 * worry about Activity re-entrance.</li>
 * <li>Do not call any {@link LocalyticsSession} methods inside a loop. Instead, calls such as {@link #tagEvent(String)} should
 * follow user actions. This limits the amount of data which is stored and uploaded.</li>
 * </ul>
 * <p>
 * This class is thread-safe.
 *
 * @version 2.0
 */
public final class LocalyticsSession
{
    /*
     * DESIGN NOTES
     *
     * The LocalyticsSession stores all of its state as a SQLite database in the parent application's private database storage
     * directory.
     *
     * Every action performed within (open, close, opt-in, opt-out, customer events) are all treated as events by the library.
     * Events are given a package prefix to ensure a namespace without collisions. Events internal to the library are flagged with
     * the Localytics package name, while events from the customer's code are flagged with the customer's package name. There's no
     * need to worry about the customer changing the package name and disrupting the naming convention, as changing the package
     * name means that a new user is created in Android and the app with a new package name gets its own storage directory.
     *
     *
     * MULTI-THREADING
     *
     * The LocalyticsSession stores all of its state as a SQLite database in the parent application's private database storage
     * directory. Disk access is slow and can block the UI in Android, so the LocalyticsSession object is a wrapper around a pair
     * of Handler objects, with each Handler object running on its own separate thread.
     *
     * All requests made of the LocalyticsSession are passed along to the mSessionHandler object, which does most of the work. The
     * mSessionHandler will pass off upload requests to the mUploadHandler, to prevent the mSessionHandler from being blocked by
     * network traffic.
     *
     * If an upload request is made, the mSessionHandler will set a flag that an upload is in progress (this flag is important for
     * thread-safety of the session data stored on disk). Then the upload request is passed to the mUploadHandler's queue. If a
     * second upload request is made while the first one is underway, the mSessionHandler notifies the mUploadHandler, which will
     * notify the mSessionHandler to retry that upload request when the first upload is completed.
     *
     * Although each LocalyticsSession object will have its own unique instance of mSessionHandler, thread-safety is handled by
     * using a single sSessionHandlerThread.
     */

    /**
     * Format string for events
     */
    /* package */static final String EVENT_FORMAT = "%s:%s"; //$NON-NLS-1$

    /**
     * Open event
     */
    /* package */static final String OPEN_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "open"); //$NON-NLS-1$

    /**
     * Close event
     */
    /* package */static final String CLOSE_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "close"); //$NON-NLS-1$

    /**
     * Opt-in event
     */
    /* package */static final String OPT_IN_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "opt_in"); //$NON-NLS-1$

    /**
     * Opt-out event
     */
    /* package */static final String OPT_OUT_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "opt_out"); //$NON-NLS-1$

    /**
     * Flow event
     */
    /* package */static final String FLOW_EVENT = String.format(EVENT_FORMAT, Constants.LOCALYTICS_PACKAGE_NAME, "flow"); //$NON-NLS-1$

    /**
     * Background thread used for all Localytics session processing. This thread is shared across all instances of
     * LocalyticsSession within a process.
     */
    /*
     * By using the class name for the HandlerThread, obfuscation through Proguard is more effective: if Proguard changes the
     * class name, the thread name also changes.
     */
    private static final HandlerThread sSessionHandlerThread = getHandlerThread(SessionHandler.class.getSimpleName());

    /**
     * Background thread used for all Localytics upload processing. This thread is shared across all instances of
     * LocalyticsSession within a process.
     */
    /*
     * By using the class name for the HandlerThread, obfuscation through Proguard is more effective: if Proguard changes the
     * class name, the thread name also changes.
     */
    private static final HandlerThread sUploadHandlerThread = getHandlerThread(UploadHandler.class.getSimpleName());

    /**
     * Helper to obtain a new {@link HandlerThread}.
     *
     * @param name to give to the HandlerThread. Useful for debugging, as the thread name is shown in DDMS.
     * @return HandlerThread whose {@link HandlerThread#start()} method has already been called.
     */
    private static HandlerThread getHandlerThread(final String name)
    {
        final HandlerThread thread = new HandlerThread(name, android.os.Process.THREAD_PRIORITY_BACKGROUND);

        thread.start();

        /*
         * The exception handler needs to be set after start() is called. If it is set before, sometime's the HandlerThread's
         * looper is null. This appears to be a bug in Android.
         */
        thread.setUncaughtExceptionHandler(new ExceptionHandler());

        return thread;
    }

    /**
     * Handler object where all session requests of this instance of LocalyticsSession are handed off to.
     * <p>
     * This Handler is the key thread synchronization point for all work inside the LocalyticsSession.
     * <p>
     * This handler runs on {@link #sSessionHandlerThread}.
     */
    private final Handler mSessionHandler;

    /**
     * Application context
     */
    private final Context mContext;

    /**
     * Localytics application key
     */
    private final String mLocalyticsKey;

    /**
     * Keeps track of which Localytics clients are currently uploading, in order to allow only one upload for a given key at a
     * time.
     * <p>
     * This field can only be read/written to from the {@link #sSessionHandlerThread}. This invariant is maintained by only
     * accessing this field from within the {@link #mSessionHandler}.
     */
    private static Map<String, Boolean> sIsUploadingMap = new HashMap<String, Boolean>();

    /**
     * Constructs a new {@link LocalyticsSession} object.
     *
     * @param context The context used to access resources on behalf of the app. It is recommended to use
     *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining references to
     *            {@code Activity} instances. Cannot be null.
     * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
     * @throws IllegalArgumentException if {@code context} is null
     * @throws IllegalArgumentException if {@code key} is null or empty
     */
    public LocalyticsSession(final Context context, final String key)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
        }
        if (TextUtils.isEmpty(key))
        {
            throw new IllegalArgumentException("key cannot be null or empty"); //$NON-NLS-1$
        }

        /*
         * Get the application context to avoid having the Localytics object holding onto an Activity object. Using application
         * context is very important to prevent the customer from giving the library multiple different contexts with different
         * package names, which would corrupt the events in the database.
         *
         * Although RenamingDelegatingContext is part of the Android SDK, the class isn't present in the ClassLoader unless the
         * process is being run as a unit test. For that reason, comparing class names is necessary instead of doing instanceof.
         *
         * Note that getting the application context may have unpredictable results for apps sharing a process running Android 2.1
         * and earlier. See <http://code.google.com/p/android/issues/detail?id=4469> for details.
         */
        mContext = !(context.getClass().getName().equals("android.test.RenamingDelegatingContext")) && Constants.CURRENT_API_LEVEL >= 8 ? context.getApplicationContext() : context; //$NON-NLS-1$
        mLocalyticsKey = key;

        mSessionHandler = new SessionHandler(mContext, mLocalyticsKey, sSessionHandlerThread.getLooper());

        /*
         * Complete Handler initialization on a background thread. Note that this is not generally a good best practice, as the
         * LocalyticsSession object (and its child objects) should be fully initialized by the time the constructor returns.
         * However this implementation is safe, as the Handler will process this initialization message before any other message.
         */
        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_INIT));
    }

    /**
     * Sets the Localytics opt-out state for this application. This call is not necessary and is provided for people who wish to
     * allow their users the ability to opt out of data collection. It can be called at any time. Passing true causes all further
     * data collection to stop, and an opt-out event to be sent to the server so the user's data is removed from the charts. <br>
     * There are very serious implications to the quality of your data when providing an opt out option. For example, users who
     * have opted out will appear as never returning, causing your new/returning chart to skew. <br>
     * If two instances of the same application are running, and one is opted in and the second opts out, the first will also
     * become opted out, and neither will collect any more data. <br>
     * If a session was started while the app was opted out, the session open event has already been lost. For this reason, all
     * sessions started while opted out will not collect data even after the user opts back in or else it taints the comparisons
     * of session lengths and other metrics.
     *
     * @param isOptedOut True if the user should be be opted out and have all his Localytics data deleted.
     */
    public void setOptOut(final boolean isOptedOut)
    {
        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_OPT_OUT, isOptedOut ? 1 : 0, 0));
    }

    /**
     * Opens the Localytics session. The session time as presented on the website is the time between the first <code>open</code>
     * and the final <code>close</code> so it is recommended to open the session as early as possible, and close it at the last
     * moment. The session must be opened before {@link #tagEvent(String)} or {@link #tagEvent(String, Map)} can be called, so
     * this call should be placed in {@code Activity#onCreate(Bundle)}.
     * <p>
     * If for any reason this is called more than once without an intervening call to {@link #close()}, subsequent calls to open
     * will be ignored.
     * <p>
     * For applications with multiple Activities, every Activity should call <code>open</code> in <code>onCreate</code>. This will
     * cause each Activity to reconnect to the currently running session.
     */
    public void open()
    {
        mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_OPEN);
    }

    /**
     * Closes the Localytics session. This should be done when the application or activity is ending. Because of the way the
     * Android lifecycle works, this call could end up in a place which gets called multiple times (such as <code>onPause</code>
     * which is the recommended location). This is fine because only the last close is processed by the server. <br>
     * Closing does not cause the session to stop collecting data. This is a result of the application life cycle. It is possible
     * for onPause to be called long before the application is actually ready to close the session.
     */
    public void close()
    {
        mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_CLOSE);
    }

    /**
     * Allows a session to tag a particular event as having occurred. For example, if a view has three buttons, it might make
     * sense to tag each button click with the name of the button which was clicked. For another example, in a game with many
     * levels it might be valuable to create a new tag every time the user gets to a new level in order to determine how far the
     * average user is progressing in the game. <br>
     * <strong>Tagging Best Practices</strong>
     * <ul>
     * <li>DO NOT use tags to record personally identifiable information.</li>
     * <li>The best way to use tags is to create all the tag strings as predefined constants and only use those. This is more
     * efficient and removes the risk of collecting personal information.</li>
     * <li>Do not set tags inside loops or any other place which gets called frequently. This can cause a lot of data to be stored
     * and uploaded.</li>
     * </ul>
     * <br>
     *
     * @param event The name of the event which occurred. Cannot be null or empty string.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     */
    public void tagEvent(final String event)
    {
        tagEvent(event, null);
    }

    /**
     * Allows a session to tag a particular event as having occurred, and optionally attach a collection of attributes to it. For
     * example, if a view has three buttons, it might make sense to tag each button with the name of the button which was clicked.
     * For another example, in a game with many levels it might be valuable to create a new tag every time the user gets to a new
     * level in order to determine how far the average user is progressing in the game. <br>
     * <strong>Tagging Best Practices</strong>
     * <ul>
     * <li>DO NOT use tags to record personally identifiable information.</li>
     * <li>The best way to use tags is to create all the tag strings as predefined constants and only use those. This is more
     * efficient and removes the risk of collecting personal information.</li>
     * <li>Do not set tags inside loops or any other place which gets called frequently. This can cause a lot of data to be stored
     * and uploaded.</li>
     * </ul>
     * <br>
     *
     * @param event The name of the event which occurred.
     * @param attributes The collection of attributes for this particular event. If this parameter is null or empty, then calling
     *            this method has the same effect as calling {@link #tagEvent(String)}. This parameter may not contain null or
     *            empty keys or values.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     * @throws IllegalArgumentException if {@code attributes} contains null keys, empty keys, null values, or empty values.
     */
    public void tagEvent(final String event, final Map<String, String> attributes)
    {
        if (Constants.ENABLE_PARAMETER_CHECKING)
        {
            if (null == event)
            {
                throw new IllegalArgumentException("event cannot be null"); //$NON-NLS-1$
            }

            if (0 == event.length())
            {
                throw new IllegalArgumentException("event cannot be empty"); //$NON-NLS-1$
            }

            if (null != attributes)
            {
                /*
                 * Calling this with empty attributes is a smell that indicates a possible programming error on the part of the
                 * caller
                 */
                if (attributes.isEmpty())
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.i(Constants.LOG_TAG, "attributes is empty.  Did the caller make an error?"); //$NON-NLS-1$
                    }
                }

                for (final Entry<String, String> entry : attributes.entrySet())
                {
                    final String key = entry.getKey();
                    final String value = entry.getValue();

                    if (null == key)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null keys"); //$NON-NLS-1$
                    }
                    if (null == value)
                    {
                        throw new IllegalArgumentException("attributes cannot contain null values"); //$NON-NLS-1$
                    }
                    if (0 == key.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty keys"); //$NON-NLS-1$
                    }
                    if (0 == value.length())
                    {
                        throw new IllegalArgumentException("attributes cannot contain empty values"); //$NON-NLS-1$
                    }
                }
            }
        }

        final String eventString = String.format(EVENT_FORMAT, mContext.getPackageName(), event);

        if (null == attributes)
        {
            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Pair<String, Map<String, String>>(eventString, null)));
        }
        else
        {
            /*
             * Note: it is important to make a copy of the map, to ensure that a client can't modify the map after this method is
             * called. A TreeMap is used to ensure that the order that the attributes are written is deterministic. For example,
             * if the maximum number of attributes is exceeded the entries that occur later alphabetically will be skipped
             * consistently.
             */
            mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_EVENT, new Pair<String, Map<String, String>>(
                                                                                                                                              eventString,
                                                                                                                                              new TreeMap<String, String>(
                                                                                                                                                                          attributes))));
        }
    }

    /**
     * Note: This implementation will perform duplicate suppression on two identical screen events that occur in a row within a
     * single session. For example, in the set of screens {"Screen 1", "Screen 1"} the second screen would be suppressed. However
     * in the set {"Screen 1", "Screen 2", "Screen 1"}, no duplicate suppression would occur.
     *
     * @param screen Name of the screen that was entered. Cannot be null or the empty string.
     * @throws IllegalArgumentException if {@code event} is null.
     * @throws IllegalArgumentException if {@code event} is empty.
     */
    public void tagScreen(final String screen)
    {
        if (null == screen)
        {
            throw new IllegalArgumentException("event cannot be null"); //$NON-NLS-1$
        }

        if (0 == screen.length())
        {
            throw new IllegalArgumentException("event cannot be empty"); //$NON-NLS-1$
        }

        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_TAG_SCREEN, screen));
    }

    /**
     * Initiates an upload of any Localytics data for this session's API key. This should be done early in the process life in
     * order to guarantee as much time as possible for slow connections to complete. It is necessary to do this even if the user
     * has opted out because this is how the opt out is transported to the webservice.
     */
    public void upload()
    {
        mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, null));
    }

    /*
     * This is useful, but not necessarily needed for the public API. If so desired, someone can uncomment this out.
     */
    // /**
    // * Initiates an upload of any Localytics data for this session's API key. This should be done early in the process life in
    // * order to guarantee as much time as possible for slow connections to complete. It is necessary to do this even if the user
    // * has opted out because this is how the opt out is transported to the webservice.
    // *
    // * @param callback a Runnable to execute when the upload completes. A typical use case would be to notify the caller that
    // the
    // * upload has completed. This runnable will be executed on an undefined thread, so the caller should anticipate
    // * this runnable NOT executing on the main thread or the thread that calls {@link #upload}. This parameter may be
    // * null.
    // */
    // public void upload(final Runnable callback)
    // {
    // mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, callback));
    // }

    /**
     * Sorts an int value into a set of regular intervals as defined by the minimum, maximum, and step size. Both the min and max
     * values are inclusive, and in the instance where (max - min + 1) is not evenly divisible by step size, the method guarantees
     * only the minimum and the step size to be accurate to specification, with the new maximum will be moved to the next regular
     * step.
     *
     * @param actualValue The int value to be sorted.
     * @param minValue The int value representing the inclusive minimum interval.
     * @param maxValue The int value representing the inclusive maximum interval.
     * @param step The int value representing the increment of each interval.
     * @return a ranged attribute suitable for passing as the argument to {@link #tagEvent(String)} or
     *         {@link #tagEvent(String, Map)}.
     */
    public static String createRangedAttribute(final int actualValue, final int minValue, final int maxValue, final int step)
    {
        // Confirm there is at least one bucket
        if (step < 1)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "Step must not be less than zero.  Returning null."); //$NON-NLS-1$
            }
            return null;
        }
        if (minValue >= maxValue)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "maxValue must not be less than minValue.  Returning null."); //$NON-NLS-1$
            }
            return null;
        }

        // Determine the number of steps, rounding up using int math
        final int stepQuantity = (maxValue - minValue + step) / step;
        final int[] steps = new int[stepQuantity + 1];
        for (int currentStep = 0; currentStep <= stepQuantity; currentStep++)
        {
            steps[currentStep] = minValue + (currentStep) * step;
        }
        return createRangedAttribute(actualValue, steps);
    }

    /**
     * Sorts an int value into a predefined, pre-sorted set of intervals, returning a string representing the new expected value.
     * The array must be sorted in ascending order, with the first element representing the inclusive lower bound and the last
     * element representing the exclusive upper bound. For instance, the array [0,1,3,10] will provide the following buckets: less
     * than 0, 0, 1-2, 3-9, 10 or greater.
     *
     * @param actualValue The int value to be bucketed.
     * @param steps The sorted int array representing the bucketing intervals.
     * @return String representation of {@code actualValue} that has been bucketed into the range provided by {@code steps}.
     * @throws IllegalArgumentException if {@code steps} is null.
     * @throws IllegalArgumentException if {@code steps} has length 0.
     */
    @SuppressWarnings("nls")
    public static String createRangedAttribute(final int actualValue, final int[] steps)
    {
        if (null == steps)
        {
            throw new IllegalArgumentException("steps cannot be null"); //$NON-NLS-1$
        }

        if (steps.length == 0)
        {
            throw new IllegalArgumentException("steps length must be greater than 0"); //$NON-NLS-1$
        }

        String bucket = null;

        // if less than smallest value
        if (actualValue < steps[0])
        {
            bucket = "less than " + steps[0];
        }
        // if greater than largest value
        else if (actualValue >= steps[steps.length - 1])
        {
            bucket = steps[steps.length - 1] + " and above";
        }
        else
        {
            // binarySearch returns the index of the value, or (-(insertion point) - 1) if not found
            int bucketIndex = Arrays.binarySearch(steps, actualValue);
            if (bucketIndex < 0)
            {
                // if the index wasn't found, then we want the value before the insertion point as the lower end
                // the special case where the insertion point is 0 is covered above, so we don't have to worry about it here
                bucketIndex = (-bucketIndex) - 2;
            }
            if (steps[bucketIndex] == (steps[bucketIndex + 1] - 1))
            {
                bucket = Integer.toString(steps[bucketIndex]);
            }
            else
            {
                bucket = steps[bucketIndex] + "-" + (steps[bucketIndex + 1] - 1); //$NON-NLS-1$
            }
        }
        return bucket;
    }

    /**
     * Helper class to handle session-related work on the {@link LocalyticsSession#sSessionHandlerThread}.
     */
    /* package */static final class SessionHandler extends Handler
    {
        /**
         * Empty handler message to initialize the callback.
         * <p>
         * This message must be sent before any other messages.
         */
        public static final int MESSAGE_INIT = 0;

        /**
         * Empty handler message to open a localytics session
         */
        public static final int MESSAGE_OPEN = 1;

        /**
         * Empty handler message to close a localytics session
         */
        public static final int MESSAGE_CLOSE = 2;

        /**
         * Handler message to tag an event.
         * <p>
         * {@link Message#obj} is a {@link Pair} instance. This object cannot be null.
         */
        public static final int MESSAGE_TAG_EVENT = 3;

        /**
         * Handler message to upload all data collected so far
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_UPLOAD = 4;

        /**
         * Empty Handler message indicating that a previous upload attempt was completed.
         */
        public static final int MESSAGE_UPLOAD_COMPLETE = 5;

        /**
         * Handler message indicating an opt-out choice.
         * <p>
         * {@link Message#arg1} == 1 for true (opt out). 0 means opt-in.
         */
        public static final int MESSAGE_OPT_OUT = 6;

        /**
         * Handler message indicating a tag screen event
         * <p>
         * {@link Message#obj} is a string representing the screen visited.
         */
        public static final int MESSAGE_TAG_SCREEN = 7;

        /**
         * Sort order for the upload blobs.
         * <p>
         * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
         */
        private static final String UPLOAD_BLOBS_EVENTS_SORT_ORDER = String.format("CAST(%s AS TEXT)", UploadBlobEventsDbColumns.EVENTS_KEY_REF); //$NON-NLS-1$

        /**
         * Sort order for the events.
         * <p>
         * This is a workaround for Android bug 3707 <http://code.google.com/p/android/issues/detail?id=3707>.
         */
        private static final String EVENTS_SORT_ORDER = String.format("CAST(%s as TEXT)", EventsDbColumns._ID); //$NON-NLS-1$

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * Localytics database
         */
        private LocalyticsProvider mProvider;

        /**
         * The Localytics API key for the session.
         */
        private final String mApiKey;

        /**
         * {@link ApiKeysDbColumns#_ID} for the {@link LocalyticsSession#mLocalyticsKey}.
         */
        private long mApiKeyId;

        /**
         * {@link SessionsDbColumns#_ID} for the session.
         */
        private long mSessionId;

        /**
         * Flag variable indicating whether {@link #MESSAGE_OPEN} has been received yet.
         */
        private boolean mIsSessionOpen = false;

        /**
         * Flag variable indicating whether the user has opted out of data collection.
         */
        private boolean mIsOptedOut = false;

        /**
         * Handler object where all upload of this instance of LocalyticsSession are handed off to.
         * <p>
         * This handler runs on {@link #sUploadHandlerThread}.
         */
        private Handler mUploadHandler;

        /**
         * Constructs a new Handler that runs on the given looper.
         *
         * @param context The context used to access resources on behalf of the app. It is recommended to use
         *            {@link Context#getApplicationContext()} to avoid the potential memory leak incurred by maintaining
         *            references to {@code Activity} instances. Cannot be null.
         * @param key The key unique for each application generated at www.localytics.com. Cannot be null or empty.
         * @param looper to run the Handler on. Cannot be null.
         * @throws IllegalArgumentException if {@code context} is null
         * @throws IllegalArgumentException if {@code key} is null or empty
         */
        public SessionHandler(final Context context, final String key, final Looper looper)
        {
            super(looper);

            if (Constants.ENABLE_PARAMETER_CHECKING)
            {
                if (context == null)
                {
                    throw new IllegalArgumentException("context cannot be null"); //$NON-NLS-1$
                }
                if (TextUtils.isEmpty(key))
                {
                    throw new IllegalArgumentException("key cannot be null or empty"); //$NON-NLS-1$
                }
            }

            mContext = context;
            mApiKey = key;
        }

        @Override
        public void handleMessage(final Message msg)
        {
            switch (msg.what)
            {
                case MESSAGE_INIT:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, "Handler received MESSAGE_INIT"); //$NON-NLS-1$
                    }

                    init();

                    break;
                }
                case MESSAGE_OPT_OUT:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, "Handler received MESSAGE_OPT_OUT"); //$NON-NLS-1$
                    }

                    final boolean isOptingOut = msg.arg1 == 0 ? false : true;

                    SessionHandler.this.optOut(isOptingOut);

                    break;
                }
                case MESSAGE_OPEN:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, "Handler received MESSAGE_OPEN"); //$NON-NLS-1$
                    }

                    SessionHandler.this.open(false);

                    break;
                }
                case MESSAGE_CLOSE:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Handler received MESSAGE_CLOSE"); //$NON-NLS-1$
                    }

                    SessionHandler.this.close();

                    break;
                }
                case MESSAGE_TAG_EVENT:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Handler received MESSAGE_TAG"); //$NON-NLS-1$
                    }

                    @SuppressWarnings("unchecked")
                    final Pair<String, Map<String, String>> pair = (Pair<String, Map<String, String>>) msg.obj;
                    final String event = pair.first;
                    final Map<String, String> attributes = pair.second;

                    SessionHandler.this.tagEvent(event, attributes);

                    break;
                }
                case MESSAGE_TAG_SCREEN:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Handler received MESSAGE_SCREEN"); //$NON-NLS-1$
                    }

                    final String screen = (String) msg.obj;

                    SessionHandler.this.tagScreen(screen);

                    break;
                }
                case MESSAGE_UPLOAD:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "SessionHandler received MESSAGE_UPLOAD"); //$NON-NLS-1$
                    }

                    /*
                     * Note that callback may be null
                     */
                    final Runnable callback = (Runnable) msg.obj;

                    SessionHandler.this.upload(callback);

                    break;
                }
                case MESSAGE_UPLOAD_COMPLETE:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Handler received MESSAGE_UPLOAD_COMPLETE"); //$NON-NLS-1$
                    }

                    sIsUploadingMap.put(mApiKey, Boolean.FALSE);

                    break;
                }
                default:
                {
                    /*
                     * This should never happen
                     */
                    throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                }
            }
        }

        /**
         * Initialize the handler post construction.
         * <p>
         * This method must only be called once.
         * <p>
         * Note: This method is a private implementation detail. It is only made public for unit testing purposes. The public
         * interface is to send {@link #MESSAGE_INIT} to the Handler.
         *
         * @see #MESSAGE_INIT
         */
        public void init()
        {
            mProvider = LocalyticsProvider.getInstance(mContext, mApiKey);

            /*
             * Check whether this session key is opted out
             */
            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(ApiKeysDbColumns.TABLE_NAME, new String[]
                    {
                        ApiKeysDbColumns._ID,
                        ApiKeysDbColumns.OPT_OUT }, String.format("%s = ?", ApiKeysDbColumns.API_KEY), new String[] //$NON-NLS-1$
                    { mApiKey }, null);

                if (cursor.moveToFirst())
                {
                    // API key was previously created
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Loading details for API key %s", mApiKey)); //$NON-NLS-1$
                    }

                    mApiKeyId = cursor.getLong(cursor.getColumnIndexOrThrow(ApiKeysDbColumns._ID));
                    mIsOptedOut = cursor.getInt(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.OPT_OUT)) != 0;
                }
                else
                {
                    // perform first-time initialization of API key
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.v(Constants.LOG_TAG, String.format("Performing first-time initialization for new API key %s", mApiKey)); //$NON-NLS-1$
                    }

                    final ContentValues values = new ContentValues();
                    values.put(ApiKeysDbColumns.API_KEY, mApiKey);
                    values.put(ApiKeysDbColumns.UUID, UUID.randomUUID().toString());
                    values.put(ApiKeysDbColumns.OPT_OUT, Boolean.FALSE);
                    values.put(ApiKeysDbColumns.CREATED_TIME, Long.valueOf(System.currentTimeMillis()));

                    mApiKeyId = mProvider.insert(ApiKeysDbColumns.TABLE_NAME, values);
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            if (!sIsUploadingMap.containsKey(mApiKey))
            {
                sIsUploadingMap.put(mApiKey, Boolean.FALSE);
            }

            /*
             * Perform lazy initialization of the UploadHandler
             */
            mUploadHandler = new UploadHandler(mContext, this, mApiKey, sUploadHandlerThread.getLooper());
        }

        /**
         * Set the opt-in/out-out state for all sessions using the current API key.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_OPT_OUT} to the Handler.
         *
         * @param isOptingOut true if the user is opting out. False if the user is opting back in.
         * @see #MESSAGE_OPT_OUT
         */
        /* package */void optOut(final boolean isOptingOut)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("Prior opt-out state is %b, requested opt-out state is %b", Boolean.valueOf(mIsOptedOut), Boolean.valueOf(isOptingOut))); //$NON-NLS-1$
            }

            // Do nothing if opt-out is unchanged
            if (mIsOptedOut == isOptingOut)
            {
                return;
            }

            mProvider.runBatchTransaction(new Runnable()
            {
                @Override
                public void run()
                {
                    final ContentValues values = new ContentValues();
                    values.put(ApiKeysDbColumns.OPT_OUT, Boolean.valueOf(isOptingOut));
                    mProvider.update(ApiKeysDbColumns.TABLE_NAME, values, String.format("%s = ?", ApiKeysDbColumns._ID), new String[] { Long.toString(mApiKeyId) }); //$NON-NLS-1$

                    if (!mIsSessionOpen)
                    {
                        /*
                         * Force a session to contain the opt event
                         */
                        open(true);
                        tagEvent(isOptingOut ? OPT_OUT_EVENT : OPT_IN_EVENT, null);
                        close();
                    }
                    else
                    {
                        tagEvent(isOptingOut ? OPT_OUT_EVENT : OPT_IN_EVENT, null);
                    }
                }
            });

            /*
             * Update the in-memory representation. It is important for the in-memory representation to be updated after the
             * on-disk representation, just in case the database update fails.
             */
            mIsOptedOut = isOptingOut;
        }

        /**
         * Open a session. While this method should only be called once without an intervening call to {@link #close()}, nothing
         * bad will happen if it is called multiple times.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_OPEN} to the Handler.
         *
         * @param ignoreLimits true to ignore limits on the number of sessions. False to enforce limits.
         * @see #MESSAGE_OPEN
         */
        /* package */void open(final boolean ignoreLimits)
        {
            if (mIsSessionOpen)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Session was already open"); //$NON-NLS-1$
                }

                return;
            }

            if (mIsOptedOut)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.d(Constants.LOG_TAG, "Data collection is opted out"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * There are two cases: 1. New session and 2. Re-connect to old session. There are two ways to reconnect to an old
             * session. One is by the age of the close event, and the other is by the age of the open event.
             */

            long closeEventId = -1; // sentinel value

            {
                Cursor eventsCursor = null;
                Cursor blob_eventsCursor = null;
                try
                {
                    eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                        { EventsDbColumns._ID }, String.format("%s = ? AND %s >= ?", EventsDbColumns.EVENT_NAME, EventsDbColumns.WALL_TIME), new String[] { CLOSE_EVENT, Long.toString(System.currentTimeMillis() - Constants.SESSION_EXPIRATION) }, EVENTS_SORT_ORDER); //$NON-NLS-1$
                    blob_eventsCursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                        { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                    final int idColumn = eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID);
                    final CursorJoiner joiner = new CursorJoiner(eventsCursor, new String[]
                        { EventsDbColumns._ID }, blob_eventsCursor, new String[]
                        { UploadBlobEventsDbColumns.EVENTS_KEY_REF });

                    for (final CursorJoiner.Result joinerResult : joiner)
                    {
                        switch (joinerResult)
                        {
                            case LEFT:
                            {

                                if (-1 != closeEventId)
                                {
                                    /*
                                     * This should never happen
                                     */
                                    if (Constants.IS_LOGGABLE)
                                    {
                                        Log.w(Constants.LOG_TAG, "There were multiple close events within SESSION_EXPIRATION"); //$NON-NLS-1$
                                    }

                                    long newClose = eventsCursor.getLong(eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID));
                                    if (newClose > closeEventId)
                                    {
                                        closeEventId = newClose;
                                    }
                                }

                                if (-1 == closeEventId)
                                {
                                    closeEventId = eventsCursor.getLong(idColumn);
                                }

                                break;
                            }
                            case BOTH:
                                break;
                            case RIGHT:
                                break;
                        }
                    }
                    /*
                     * Verify that the session hasn't already been flagged for upload. That could happen if
                     */
                }
                finally
                {
                    if (eventsCursor != null)
                    {
                        eventsCursor.close();
                    }
                    if (blob_eventsCursor != null)
                    {
                        blob_eventsCursor.close();
                    }
                }
            }

            if (-1 != closeEventId)
            {
                Log.v(Constants.LOG_TAG, "Opening old closed session and reconnecting"); //$NON-NLS-1$
                mIsSessionOpen = true;

                openClosedSession(closeEventId);
            }
            else
            {
                Cursor sessionsCursor = null;
                try
                {
                    sessionsCursor = mProvider.query(SessionsDbColumns.TABLE_NAME, new String[]
                        {
                            SessionsDbColumns._ID,
                            SessionsDbColumns.SESSION_START_WALL_TIME }, null, null, SessionsDbColumns._ID);

                    if (sessionsCursor.moveToLast())
                    {
                        if (sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME)) >= System.currentTimeMillis()
                                - Constants.SESSION_EXPIRATION)
                        {
                            // reconnect
                            Log.v(Constants.LOG_TAG, "Opening old unclosed session and reconnecting"); //$NON-NLS-1$
                            mIsSessionOpen = true;
                            mSessionId = sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns._ID));
                            return;
                        }

                        // delete empties
                        Cursor eventsCursor = null;
                        try
                        {
                            String sessionId = Long.toString(sessionsCursor.getLong(sessionsCursor.getColumnIndexOrThrow(SessionsDbColumns._ID)));
                            eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                                { EventsDbColumns._ID }, String.format("%s = ?", EventsDbColumns.SESSION_KEY_REF), new String[] //$NON-NLS-1$
                                { sessionId }, null);

                            if (eventsCursor.getCount() == 0)
                            {
                                mProvider.delete(SessionsDbColumns.TABLE_NAME, String.format("%s = ?", SessionsDbColumns._ID), new String[] { sessionId }); //$NON-NLS-1$
                            }
                        }
                        finally
                        {
                            if (null != eventsCursor)
                            {
                                eventsCursor.close();
                                eventsCursor = null;
                            }
                        }
                    }
                }
                finally
                {
                    if (null != sessionsCursor)
                    {
                        sessionsCursor.close();
                        sessionsCursor = null;
                    }
                }

                /*
                 * Check that the maximum number of sessions hasn't been exceeded
                 */
                if (!ignoreLimits)
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = mProvider.query(SessionsDbColumns.TABLE_NAME, new String[]
                            { SessionsDbColumns._ID }, null, null, null);

                        if (cursor.getCount() >= Constants.MAX_NUM_SESSIONS)
                        {
                            if (Constants.IS_LOGGABLE)
                            {
                                Log.w(Constants.LOG_TAG, "Maximum number of sessions are already on disk--not writing any new sessions until old sessions are cleared out.  Try calling upload() to store more sessions."); //$NON-NLS-1$
                            }
                            return;
                        }
                    }
                    finally
                    {
                        if (cursor != null)
                        {
                            cursor.close();
                            cursor = null;
                        }
                    }
                }

                Log.v(Constants.LOG_TAG, "Opening new session"); //$NON-NLS-1$
                mIsSessionOpen = true;

                openNewSession();
            }
        }

        /**
         * Opens a new session. This is a helper method to {@link #open(boolean)}.
         *
         * @effects Updates the database by creating a new entry in the {@link SessionsDbColumns} table.
         */
        private void openNewSession()
        {
            final TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

            final ContentValues values = new ContentValues();
            values.put(SessionsDbColumns.API_KEY_REF, Long.valueOf(mApiKeyId));
            values.put(SessionsDbColumns.SESSION_START_WALL_TIME, Long.valueOf(System.currentTimeMillis()));
            values.put(SessionsDbColumns.UUID, UUID.randomUUID().toString());
            values.put(SessionsDbColumns.APP_VERSION, DatapointHelper.getAppVersion(mContext));
            values.put(SessionsDbColumns.ANDROID_SDK, Integer.valueOf(Constants.CURRENT_API_LEVEL));
            values.put(SessionsDbColumns.ANDROID_VERSION, VERSION.RELEASE);

            // Try and get the deviceId. If it is unavailable (or invalid) use the installation ID instead.
            String deviceId = DatapointHelper.getAndroidIdHashOrNull(mContext);
            if (deviceId == null)
            {
                Cursor cursor = null;
                try
                {
                    cursor = mProvider.query(ApiKeysDbColumns.TABLE_NAME, null, String.format("%s = ?", ApiKeysDbColumns.API_KEY), new String[] { mApiKey }, null); //$NON-NLS-1$
                    if (cursor.moveToFirst())
                    {
                        deviceId = cursor.getString(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.UUID));
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                        cursor = null;
                    }
                }
            }

            values.put(SessionsDbColumns.DEVICE_ANDROID_ID_HASH, deviceId);
            values.put(SessionsDbColumns.DEVICE_COUNTRY, telephonyManager.getSimCountryIso());
            values.put(SessionsDbColumns.DEVICE_MANUFACTURER, DatapointHelper.getManufacturer());
            values.put(SessionsDbColumns.DEVICE_MODEL, Build.MODEL);
            values.put(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH, DatapointHelper.getSerialNumberHashOrNull());
            values.put(SessionsDbColumns.DEVICE_TELEPHONY_ID, DatapointHelper.getTelephonyDeviceIdOrNull(mContext));
            values.put(SessionsDbColumns.DEVICE_TELEPHONY_ID_HASH, DatapointHelper.getTelephonyDeviceIdHashOrNull(mContext));
            values.put(SessionsDbColumns.LOCALE_COUNTRY, Locale.getDefault().getCountry());
            values.put(SessionsDbColumns.LOCALE_LANGUAGE, Locale.getDefault().getLanguage());
            values.put(SessionsDbColumns.LOCALYTICS_LIBRARY_VERSION, Constants.LOCALYTICS_CLIENT_LIBRARY_VERSION);

            values.putNull(SessionsDbColumns.LATITUDE);
            values.putNull(SessionsDbColumns.LONGITUDE);
            values.put(SessionsDbColumns.NETWORK_CARRIER, telephonyManager.getNetworkOperatorName());
            values.put(SessionsDbColumns.NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso());
            values.put(SessionsDbColumns.NETWORK_TYPE, DatapointHelper.getNetworkType(mContext, telephonyManager));

            mProvider.runBatchTransaction(new Runnable()
            {
                @Override
                public void run()
                {
                    mSessionId = mProvider.insert(SessionsDbColumns.TABLE_NAME, values);
                    if (mSessionId == -1)
                    {
                        throw new RuntimeException("session insert failed"); //$NON-NLS-1$
                    }

                    tagEvent(OPEN_EVENT, null);
                }

            });

            /*
             * This is placed here so that the DatapointHelper has a chance to retrieve the old UUID before it is deleted.
             */
            LocalyticsProvider.deleteOldFiles(mContext);
        }

        /**
         * Reopens a previous session. This is a helper method to {@link #open(boolean)}.
         *
         * @param closeEventId The last close event which is to be deleted so that the old session can be reopened
         * @effects Updates the database by deleting the last close event and sets {@link #mSessionId} to the session id of the
         *          last close event
         */
        private void openClosedSession(final long closeEventId)
        {
            Cursor eventCursor = null;
            try
            {
                eventCursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                    { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(closeEventId) }, null); //$NON-NLS-1$

                if (eventCursor.moveToFirst())
                {
                    mSessionId = eventCursor.getLong(eventCursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));

                    mProvider.delete(EventsDbColumns.TABLE_NAME, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(closeEventId) }); //$NON-NLS-1$
                }
                else
                {
                    /*
                     * This should never happen
                     */

                    if (Constants.IS_LOGGABLE)
                    {
                        Log.e(Constants.LOG_TAG, "Event no longer exists"); //$NON-NLS-1$
                    }

                    openNewSession();
                }
            }
            finally
            {
                if (eventCursor != null)
                {
                    eventCursor.close();
                }
            }
        }

        /**
         * Close a session. While this method should only be called after {@link #open(boolean)}, nothing bad will happen if it is
         * called and {@link #open(boolean)} wasn't called. Similarly, nothing bad will happen if close is called multiple times.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_CLOSE} to the Handler.
         *
         * @see #MESSAGE_OPEN
         */
        /* package */void close()
        {
            if (!mIsSessionOpen) // do nothing if session is not open
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Session was not open, so close is not possible."); //$NON-NLS-1$
                }
                return;
            }

            tagEvent(CLOSE_EVENT, null);

            mIsSessionOpen = false;
        }

        /**
         * Tag an event in a session. While this method shouldn't be called unless {@link #open(boolean)} is called first, this
         * method will simply do nothing if {@link #open(boolean)} hasn't been called.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_TAG_EVENT} to the Handler.
         *
         * @param event The name of the event which occurred.
         * @param attributes The collection of attributes for this particular event. If this parameter is null, then calling this
         *            method has the same effect as calling {@link #tagEvent(String)}.
         * @see #MESSAGE_TAG_EVENT
         */
        /* package */void tagEvent(final String event, final Map<String, String> attributes)
        {
            if (!mIsSessionOpen)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Tag not written because the session was not open"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * First insert the event
             */
            final long eventId;
            {
                final ContentValues values = new ContentValues();
                values.put(EventsDbColumns.SESSION_KEY_REF, Long.valueOf(mSessionId));
                values.put(EventsDbColumns.UUID, UUID.randomUUID().toString());
                values.put(EventsDbColumns.EVENT_NAME, event);
                values.put(EventsDbColumns.REAL_TIME, Long.valueOf(SystemClock.elapsedRealtime()));
                values.put(EventsDbColumns.WALL_TIME, Long.valueOf(System.currentTimeMillis()));

                /*
                 * Special case for open event: keep the start time in sync with the start time put into the sessions table.
                 */
                if (OPEN_EVENT.equals(event))
                {
                    Cursor cursor = null;
                    try
                    {
                        cursor = mProvider.query(SessionsDbColumns.TABLE_NAME, new String[]
                            { SessionsDbColumns.SESSION_START_WALL_TIME }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(mSessionId) }, null); //$NON-NLS-1$

                        if (cursor.moveToFirst())
                        {
                            values.put(EventsDbColumns.WALL_TIME, Long.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME))));
                        }
                        else
                        {
                            // this should never happen
                            throw new RuntimeException("Session didn't exist"); //$NON-NLS-1$
                        }
                    }
                    finally
                    {
                        if (null != cursor)
                        {
                            cursor.close();
                        }
                    }
                }

                eventId = mProvider.insert(EventsDbColumns.TABLE_NAME, values);

                if (-1 == eventId)
                {
                    throw new RuntimeException("Inserting event failed"); //$NON-NLS-1$
                }
            }

            /*
             * If attributes exist, insert them as well
             */
            if (null != attributes)
            {
                int count = 0;
                for (final Entry<String, String> entry : attributes.entrySet())
                {
                    /*
                     * Note: the attributes that are skipped are deterministic, because the map is actually an instance of
                     * TreeMap.
                     */
                    count++;
                    if (count > Constants.MAX_NUM_ATTRIBUTES)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, String.format("Map contains %s keys while the maximum number of attributes is %s.  Some attributes were not written.  Consider reducing the number of attributes.", Integer.valueOf(attributes.size()), Integer.valueOf(Constants.MAX_NUM_ATTRIBUTES))); //$NON-NLS-1$
                        }
                        break;
                    }

                    final ContentValues values = new ContentValues();
                    values.put(AttributesDbColumns.EVENTS_KEY_REF, Long.valueOf(eventId));
                    values.put(AttributesDbColumns.ATTRIBUTE_KEY, entry.getKey());
                    values.put(AttributesDbColumns.ATTRIBUTE_VALUE, entry.getValue());

                    final long id = mProvider.insert(AttributesDbColumns.TABLE_NAME, values);

                    if (-1 == id)
                    {
                        throw new RuntimeException("Inserting attribute failed"); //$NON-NLS-1$
                    }
                }
            }

            /*
             * Insert the event into the history, only for application events
             */
            if (!OPEN_EVENT.equals(event) && !CLOSE_EVENT.equals(event) && !OPT_IN_EVENT.equals(event) && !OPT_OUT_EVENT.equals(event) && !FLOW_EVENT.equals(event))
            {
                final ContentValues values = new ContentValues();
                values.put(EventHistoryDbColumns.NAME, event.substring(mContext.getPackageName().length() + 1, event.length()));
                values.put(EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_EVENT));
                values.put(EventHistoryDbColumns.SESSION_KEY_REF, Long.valueOf(mSessionId));
                values.putNull(EventHistoryDbColumns.PROCESSED_IN_BLOB);
                mProvider.insert(EventHistoryDbColumns.TABLE_NAME, values);

                conditionallyAddFlowEvent();
            }
        }

        /**
         * Tag a screen in a session. While this method shouldn't be called unless {@link #open(boolean)} is called first, this
         * method will simply do nothing if {@link #open(boolean)} hasn't been called.
         * <p>
         * This method performs duplicate suppression, preventing multiple screens with the same value in a row within a given
         * session.
         * <p>
         * This method must only be called after {@link #init()} is called.
         * <p>
         * Note: This method is a private implementation detail. It is only made public for unit testing purposes. The public
         * interface is to send {@link #MESSAGE_TAG_SCREEN} to the Handler.
         *
         * @param screen The name of the screen which occurred. Cannot be null or empty.
         * @see #MESSAGE_TAG_SCREEN
         */
        /* package */void tagScreen(final String screen)
        {
            if (!mIsSessionOpen)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Tag not written because the session was not open"); //$NON-NLS-1$
                }
                return;
            }

            /*
             * Do duplicate suppression
             */
            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(EventHistoryDbColumns.TABLE_NAME, new String[]
                    { EventHistoryDbColumns.NAME }, String.format("%s = ? AND %s = ?", EventHistoryDbColumns.TYPE, EventHistoryDbColumns.SESSION_KEY_REF), new String[] { Integer.toString(EventHistoryDbColumns.TYPE_SCREEN), Long.toString(mSessionId) }, String.format("%s DESC", EventHistoryDbColumns._ID)); //$NON-NLS-1$ //$NON-NLS-2$

                if (cursor.moveToFirst())
                {
                    if (screen.equals(cursor.getString(cursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME))))
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.v(Constants.LOG_TAG, String.format("Suppressed duplicate screen %s", screen)); //$NON-NLS-1$
                        }
                        return;
                    }
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            /*
             * Write the screen to the database
             */
            final ContentValues values = new ContentValues();
            values.put(EventHistoryDbColumns.NAME, screen);
            values.put(EventHistoryDbColumns.TYPE, Integer.valueOf(EventHistoryDbColumns.TYPE_SCREEN));
            values.put(EventHistoryDbColumns.SESSION_KEY_REF, Long.valueOf(mSessionId));
            values.putNull(EventHistoryDbColumns.PROCESSED_IN_BLOB);
            mProvider.insert(EventHistoryDbColumns.TABLE_NAME, values);

            conditionallyAddFlowEvent();
        }

        /**
         * Conditionally adds a flow event if no flow event exists in the current upload blob.
         */
        private void conditionallyAddFlowEvent()
        {
            /*
             * Creating a flow "event" is required to act as a placeholder so that the uploader will know that an upload needs to
             * occur. A flow event should only be created if there isn't already a flow event that hasn't been associated with an
             * upload blob.
             */
            boolean foundUnassociatedFlowEvent = false;

            Cursor eventsCursor = null;
            Cursor blob_eventsCursor = null;
            try
            {
                eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                    { EventsDbColumns._ID }, String.format("%s = ?", EventsDbColumns.EVENT_NAME), new String[] //$NON-NLS-1$
                    { FLOW_EVENT }, EVENTS_SORT_ORDER);

                blob_eventsCursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                    { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                final CursorJoiner joiner = new CursorJoiner(eventsCursor, new String[]
                    { EventsDbColumns._ID }, blob_eventsCursor, new String[]
                    { UploadBlobEventsDbColumns.EVENTS_KEY_REF });
                for (final CursorJoiner.Result joinerResult : joiner)
                {
                    switch (joinerResult)
                    {
                        case LEFT:
                        {
                            foundUnassociatedFlowEvent = true;
                            break;
                        }
                        case BOTH:
                            break;
                        case RIGHT:
                            break;
                    }
                }
            }
            finally
            {
                if (eventsCursor != null)
                {
                    eventsCursor.close();
                    eventsCursor = null;
                }

                if (blob_eventsCursor != null)
                {
                    blob_eventsCursor.close();
                    blob_eventsCursor = null;
                }
            }

            if (!foundUnassociatedFlowEvent)
            {
                tagEvent(FLOW_EVENT, null);
            }
        }

        /**
         * Builds upload blobs for all events.
         *
         * @effects Mutates the database by creating a new upload blob for all events that are unassociated at the time this
         *          method is called.
         */
        /* package */void preUploadBuildBlobs()
        {
            /*
             * Group all events that aren't part of an upload blob into a new blob. While this process is a linear algorithm that
             * requires scanning two database tables, the performance won't be a problem for two reasons: 1. This process happens
             * frequently so the number of events to group will always be low. 2. There is a maximum number of events, keeping the
             * overall size low. Note that close events that are younger than SESSION_EXPIRATION will be skipped to allow session
             * reconnects.
             */

            // temporary set of event ids that aren't in a blob
            final Set<Long> eventIds = new HashSet<Long>();

            Cursor eventsCursor = null;
            Cursor blob_eventsCursor = null;
            try
            {
                eventsCursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                    {
                        EventsDbColumns._ID,
                        EventsDbColumns.EVENT_NAME,
                        EventsDbColumns.WALL_TIME }, null, null, EVENTS_SORT_ORDER);

                blob_eventsCursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                    { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, null, null, UPLOAD_BLOBS_EVENTS_SORT_ORDER);

                final int idColumn = eventsCursor.getColumnIndexOrThrow(EventsDbColumns._ID);
                final CursorJoiner joiner = new CursorJoiner(eventsCursor, new String[]
                    { EventsDbColumns._ID }, blob_eventsCursor, new String[]
                    { UploadBlobEventsDbColumns.EVENTS_KEY_REF });
                for (final CursorJoiner.Result joinerResult : joiner)
                {
                    switch (joinerResult)
                    {
                        case LEFT:
                        {
                            if (CLOSE_EVENT.equals(eventsCursor.getString(eventsCursor.getColumnIndexOrThrow(EventsDbColumns.EVENT_NAME))))
                            {
                                if (System.currentTimeMillis() - eventsCursor.getLong(eventsCursor.getColumnIndexOrThrow(EventsDbColumns.WALL_TIME)) < Constants.SESSION_EXPIRATION)
                                {
                                    break;
                                }
                            }
                            eventIds.add(Long.valueOf(eventsCursor.getLong(idColumn)));
                            break;
                        }
                        case BOTH:
                            break;
                        case RIGHT:
                            break;
                    }
                }
            }
            finally
            {
                if (eventsCursor != null)
                {
                    eventsCursor.close();
                }

                if (blob_eventsCursor != null)
                {
                    blob_eventsCursor.close();
                }
            }

            if (eventIds.size() > 0)
            {
                final long blobId;
                {
                    final ContentValues values = new ContentValues();
                    values.put(UploadBlobsDbColumns.UUID, UUID.randomUUID().toString());
                    blobId = mProvider.insert(UploadBlobsDbColumns.TABLE_NAME, values);
                }

                {
                    final ContentValues values = new ContentValues();
                    for (final Long x : eventIds)
                    {
                        values.clear();

                        values.put(UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF, Long.valueOf(blobId));
                        values.put(UploadBlobEventsDbColumns.EVENTS_KEY_REF, x);

                        mProvider.insert(UploadBlobEventsDbColumns.TABLE_NAME, values);
                    }
                }

                final ContentValues values = new ContentValues();
                values.put(EventHistoryDbColumns.PROCESSED_IN_BLOB, Long.valueOf(blobId));
                mProvider.update(EventHistoryDbColumns.TABLE_NAME, values, String.format("%s IS NULL", EventHistoryDbColumns.PROCESSED_IN_BLOB), null); //$NON-NLS-1$
            }
        }

        /**
         * Initiate upload of all session data currently stored on disk.
         * <p>
         * This method must only be called after {@link #init()} is called. The session does not need to be open for an upload to
         * occur.
         * <p>
         * Note: This method is a private implementation detail. It is only made package accessible for unit testing purposes. The
         * public interface is to send {@link #MESSAGE_UPLOAD} to the Handler.
         *
         * @param callback An optional callback to perform once the upload completes. May be null for no callback.
         * @see #MESSAGE_UPLOAD
         */
        /* package */void upload(final Runnable callback)
        {
            if (sIsUploadingMap.get(mApiKey).booleanValue())
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.d(Constants.LOG_TAG, "Already uploading"); //$NON-NLS-1$
                }

                mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.MESSAGE_RETRY_UPLOAD_REQUEST, callback));
                return;
            }

            try
            {
                mProvider.runBatchTransaction(new Runnable()
                {
                    public void run()
                    {
                        preUploadBuildBlobs();
                    }
                });

                sIsUploadingMap.put(mApiKey, Boolean.TRUE);
                mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.MESSAGE_UPLOAD, callback));
            }
            catch (final Exception e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "Error occurred during upload", e); //$NON-NLS-1$
                }

                sIsUploadingMap.put(mApiKey, Boolean.FALSE);

                // Notify the caller the upload is "complete"
                if (callback != null)
                {
                    /*
                     * Note that a new thread is created for the callback. This ensures that client code can't affect the
                     * performance of the SessionHandler's thread.
                     */
                    new Thread(callback, UploadHandler.UPLOAD_CALLBACK_THREAD_NAME).start();
                }
            }
        }
    }

    /**
     * Helper object to the {@link SessionHandler} which helps process upload requests.
     */
    /* package */static final class UploadHandler extends Handler
    {

        /**
         * Thread name that the upload callback runnable is executed on.
         */
        private static final String UPLOAD_CALLBACK_THREAD_NAME = "upload_callback"; //$NON-NLS-1$

        /**
         * Localytics upload URL, as a format string that contains a format for the API key.
         */
        private final static String ANALYTICS_URL = "http://analytics.localytics.com/api/v2/applications/%s/uploads"; //$NON-NLS-1$

        /**
         * Handler message to upload all data collected so far
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_UPLOAD = 1;

        /**
         * Handler message indicating that there is a queued upload request. When this message is processed, this handler simply
         * forwards the request back to {@link LocalyticsSession#mSessionHandler} with {@link SessionHandler#MESSAGE_UPLOAD}.
         * <p>
         * {@link Message#obj} is a {@code Runnable} to execute when upload is complete. The thread that this runnable will
         * executed on is undefined.
         */
        public static final int MESSAGE_RETRY_UPLOAD_REQUEST = 2;

        /**
         * Reference to the Localytics database
         */
        private final LocalyticsProvider mProvider;

        /**
         * Application context
         */
        private final Context mContext;

        /**
         * The Localytics API key
         */
        private final String mApiKey;

        /**
         * Parent session handler to notify when an upload completes.
         */
        private final Handler mSessionHandler;

        /**
         * Constructs a new Handler that runs on {@code looper}.
         * <p>
         * Note: This constructor may perform disk access.
         *
         * @param context Application context. Cannot be null.
         * @param sessionHandler Parent {@link SessionHandler} object to notify when uploads are completed. Cannot be null.
         * @param apiKey Localytics API key. Cannot be null.
         * @param looper to run the Handler on. Cannot be null.
         */
        public UploadHandler(final Context context, final Handler sessionHandler, final String apiKey, final Looper looper)
        {
            super(looper);

            mContext = context;
            mProvider = LocalyticsProvider.getInstance(context, apiKey);
            mSessionHandler = sessionHandler;
            mApiKey = apiKey;
        }

        @Override
        public void handleMessage(final Message msg)
        {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case MESSAGE_UPLOAD:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "UploadHandler Received MESSAGE_UPLOAD"); //$NON-NLS-1$
                    }

                    /*
                     * Note that callback may be null
                     */
                    final Runnable callback = (Runnable) msg.obj;

                    try
                    {
                        final List<JSONObject> toUpload = convertDatabaseToJson();

                        if (!toUpload.isEmpty())
                        {
                            final StringBuilder builder = new StringBuilder();
                            for (final JSONObject json : toUpload)
                            {
                                builder.append(json.toString());
                                builder.append('\n');
                            }

                            if (uploadSessions(String.format(ANALYTICS_URL, mApiKey), builder.toString()))
                            {
                                mProvider.runBatchTransaction(new Runnable()
                                {
                                    public void run()
                                    {
                                        deleteBlobsAndSessions(mProvider);
                                    }
                                });
                            }
                        }
                    }
                    finally
                    {
                        if (callback != null)
                        {
                            /*
                             * Execute the callback on a separate thread, to avoid exposing this thread to the client of the
                             * library
                             */
                            new Thread(callback, UPLOAD_CALLBACK_THREAD_NAME).start();
                        }

                        mSessionHandler.sendEmptyMessage(SessionHandler.MESSAGE_UPLOAD_COMPLETE);
                    }
                    break;
                }
                case MESSAGE_RETRY_UPLOAD_REQUEST:
                {
                    if (Constants.IS_LOGGABLE)
                    {
                        Log.d(Constants.LOG_TAG, "Received MESSAGE_RETRY_UPLOAD_REQUEST"); //$NON-NLS-1$
                    }

                    mSessionHandler.sendMessage(mSessionHandler.obtainMessage(SessionHandler.MESSAGE_UPLOAD, msg.obj));
                    break;
                }
                default:
                {
                    /*
                     * This should never happen
                     */
                    throw new RuntimeException("Fell through switch statement"); //$NON-NLS-1$
                }
            }
        }

        /**
         * Uploads the post Body to the webservice
         *
         * @param url where {@code body} will be posted to. Cannot be null.
         * @param body upload body as a string. This should be a plain old string. Cannot be null.
         * @return True on success, false on failure.
         */
        /* package */static boolean uploadSessions(final String url, final String body)
        {
            if (Constants.ENABLE_PARAMETER_CHECKING)
            {
                if (null == url)
                {
                    throw new IllegalArgumentException("url cannot be null"); //$NON-NLS-1$
                }

                if (null == body)
                {
                    throw new IllegalArgumentException("body cannot be null"); //$NON-NLS-1$
                }
            }

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("Upload body before compression is: %s", body.toString())); //$NON-NLS-1$
            }

            final DefaultHttpClient client = new DefaultHttpClient();
            final HttpPost method = new HttpPost(url);
            method.addHeader("Content-Type", "application/x-gzip"); //$NON-NLS-1$ //$NON-NLS-2$

            GZIPOutputStream gos = null;
            try
            {
                final byte[] originalBytes = body.getBytes("UTF-8"); //$NON-NLS-1$
                final ByteArrayOutputStream baos = new ByteArrayOutputStream(originalBytes.length);
                gos = new GZIPOutputStream(baos);
                gos.write(originalBytes);
                gos.finish();
                gos.flush();

                final ByteArrayEntity postBody = new ByteArrayEntity(baos.toByteArray());
                method.setEntity(postBody);

                final HttpResponse response = client.execute(method);

                final StatusLine status = response.getStatusLine();
                final int statusCode = status.getStatusCode();
                if (Constants.IS_LOGGABLE)
                {
                    Log.v(Constants.LOG_TAG, String.format("Upload complete with status %d", Integer.valueOf(statusCode))); //$NON-NLS-1$
                }

                /*
                 * 5xx status codes indicate a server error, so upload should be reattempted
                 */
                if (statusCode >= 500 && statusCode <= 599)
                {
                    return false;
                }

                return true;
            }
            catch (final UnsupportedEncodingException e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "UnsupportedEncodingException", e); //$NON-NLS-1$
                }
                return false;
            }
            catch (final ClientProtocolException e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "ClientProtocolException", e); //$NON-NLS-1$
                }
                return false;
            }
            catch (final IOException e)
            {
                if (Constants.IS_LOGGABLE)
                {
                    Log.w(Constants.LOG_TAG, "IOException", e); //$NON-NLS-1$
                }
                return false;
            }
            finally
            {
                if (null != gos)
                {
                    try
                    {
                        gos.close();
                    }
                    catch (final IOException e)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                        }
                    }
                }
            }
        }

        /**
         * Helper that converts blobs in the database into a JSON representation for upload.
         *
         * @return A list of JSON objecs to upload to the server
         */
        /* package */List<JSONObject> convertDatabaseToJson()
        {
            final List<JSONObject> result = new LinkedList<JSONObject>();
            Cursor cursor = null;
            try
            {
                cursor = mProvider.query(UploadBlobsDbColumns.TABLE_NAME, null, null, null, null);

                final long creationTime = getApiKeyCreationTime(mProvider, mApiKey);

                final int idColumn = cursor.getColumnIndexOrThrow(UploadBlobsDbColumns._ID);
                final int uuidColumn = cursor.getColumnIndexOrThrow(UploadBlobsDbColumns.UUID);
                while (cursor.moveToNext())
                {
                    try
                    {
                        final JSONObject blobHeader = new JSONObject();

                        blobHeader.put(JsonObjects.BlobHeader.KEY_DATA_TYPE, BlobHeader.VALUE_DATA_TYPE);
                        blobHeader.put(JsonObjects.BlobHeader.KEY_PERSISTENT_STORAGE_CREATION_TIME_SECONDS, creationTime);
                        blobHeader.put(JsonObjects.BlobHeader.KEY_SEQUENCE_NUMBER, cursor.getLong(idColumn));
                        blobHeader.put(JsonObjects.BlobHeader.KEY_UNIQUE_ID, cursor.getString(uuidColumn));
                        blobHeader.put(JsonObjects.BlobHeader.KEY_ATTRIBUTES, getAttributesFromSession(mProvider, mApiKey, getSessionIdForBlobId(cursor.getLong(idColumn))));
                        result.add(blobHeader);

                        Cursor blobEvents = null;
                        try
                        {
                            blobEvents = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                                { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, String.format("%s = ?", UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF), new String[] //$NON-NLS-1$
                                { Long.toString(cursor.getLong(idColumn)) }, UploadBlobEventsDbColumns.EVENTS_KEY_REF);

                            final int eventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF);
                            while (blobEvents.moveToNext())
                            {
                                result.add(convertEventToJson(mProvider, mContext, blobEvents.getLong(eventIdColumn), cursor.getLong(idColumn), mApiKey));
                            }
                        }
                        finally
                        {
                            if (null != blobEvents)
                            {
                                blobEvents.close();
                            }
                        }
                    }
                    catch (final JSONException e)
                    {
                        if (Constants.IS_LOGGABLE)
                        {
                            Log.w(Constants.LOG_TAG, "Caught exception", e); //$NON-NLS-1$
                        }
                    }
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }

            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, String.format("JSON result is %s", result.toString())); //$NON-NLS-1$
            }

            return result;
        }

        /**
         * Deletes all blobs and sessions/events/attributes associated with those blobs.
         * <p>
         * This should be called after a successful upload completes.
         *
         * @param provider Localytics database provider. Cannot be null.
         */
        /* package */static void deleteBlobsAndSessions(final LocalyticsProvider provider)
        {
            /*
             * Deletion needs to occur in a specific order due to database constraints. Specifically, blobevents need to be
             * deleted first. Then blobs themselves can be deleted. Then attributes need to be deleted first. Then events. Then
             * sessions.
             */

            final LinkedList<Long> sessionsToDelete = new LinkedList<Long>();
            final HashSet<Long> blobsToDelete = new HashSet<Long>();

            Cursor blobEvents = null;
            try
            {
                blobEvents = provider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                    {
                        UploadBlobEventsDbColumns._ID,
                        UploadBlobEventsDbColumns.EVENTS_KEY_REF,
                        UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF }, null, null, null);

                final int uploadBlobIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF);
                final int blobEventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns._ID);
                final int eventIdColumn = blobEvents.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF);
                while (blobEvents.moveToNext())
                {
                    final long blobId = blobEvents.getLong(uploadBlobIdColumn);
                    final long blobEventId = blobEvents.getLong(blobEventIdColumn);
                    final long eventId = blobEvents.getLong(eventIdColumn);

                    // delete the blobevent
                    provider.delete(UploadBlobEventsDbColumns.TABLE_NAME, String.format("%s = ?", UploadBlobEventsDbColumns._ID), new String[] { Long.toString(blobEventId) }); //$NON-NLS-1$

                    /*
                     * Add the blob to the list of blobs to be deleted
                     */
                    blobsToDelete.add(Long.valueOf(blobId));

                    // delete all attributes for the event
                    provider.delete(AttributesDbColumns.TABLE_NAME, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }); //$NON-NLS-1$

                    /*
                     * Check to see if the event is a close event, indicating that the session is complete and can also be deleted
                     */
                    Cursor eventCursor = null;
                    try
                    {
                        eventCursor = provider.query(EventsDbColumns.TABLE_NAME, new String[]
                            { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ? AND %s = ?", EventsDbColumns._ID, EventsDbColumns.EVENT_NAME), new String[] //$NON-NLS-1$
                            {
                                Long.toString(eventId),
                                CLOSE_EVENT }, null);

                        if (eventCursor.moveToFirst())
                        {
                            final long sessionId = eventCursor.getLong(eventCursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));

                            provider.delete(EventHistoryDbColumns.TABLE_NAME, String.format("%s = ?", EventHistoryDbColumns.SESSION_KEY_REF), new String[] //$NON-NLS-1$
                                { Long.toString(sessionId) });

                            sessionsToDelete.add(Long.valueOf(eventCursor.getLong(eventCursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF))));
                        }
                    }
                    finally
                    {
                        if (null != eventCursor)
                        {
                            eventCursor.close();
                        }
                    }

                    // delete the event
                    provider.delete(EventsDbColumns.TABLE_NAME, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(eventId) }); //$NON-NLS-1$
                }
            }
            finally
            {
                if (null != blobEvents)
                {
                    blobEvents.close();
                }
            }

            // delete blobs
            for (final long x : blobsToDelete)
            {
                provider.delete(UploadBlobsDbColumns.TABLE_NAME, String.format("%s = ?", UploadBlobsDbColumns._ID), new String[] { Long.toString(x) }); //$NON-NLS-1$
            }

            // delete sessions
            for (final long x : sessionsToDelete)
            {
                provider.delete(SessionsDbColumns.TABLE_NAME, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(x) }); //$NON-NLS-1$
            }

        }

        /**
         * Gets the creation time for an API key.
         *
         * @param provider Localytics database provider. Cannot be null.
         * @param key Localytics API key. Cannot be null.
         * @return The time in seconds since the Unix Epoch when the API key entry was created in the database.
         * @throws RuntimeException if the API key entry doesn't exist in the database.
         */
        /* package */static long getApiKeyCreationTime(final LocalyticsProvider provider, final String key)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(ApiKeysDbColumns.TABLE_NAME, null, String.format("%s = ?", ApiKeysDbColumns.API_KEY), new String[] { key }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return Math.round((float) cursor.getLong(cursor.getColumnIndexOrThrow(ApiKeysDbColumns.CREATED_TIME)) / DateUtils.SECOND_IN_MILLIS);
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException("API key entry couldn't be found"); //$NON-NLS-1$
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Helper method to generate the attributes object for a session
         *
         * @param provider Instance of the Localytics database provider. Cannot be null.
         * @param apiKey Localytics API key. Cannot be null.
         * @param sessionId The {@link SessionsDbColumns#_ID} of the session.
         * @return a JSONObject representation of the session attributes
         * @throws JSONException if a problem occurred converting the element to JSON.
         */
        /* package */static JSONObject getAttributesFromSession(final LocalyticsProvider provider, final String apiKey, final long sessionId) throws JSONException
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, null, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    final JSONObject result = new JSONObject();
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_CLIENT_APP_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.APP_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DATA_CONNECTION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_TYPE)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_ANDROID_ID_HASH, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_ANDROID_ID_HASH)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_COUNTRY)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_MANUFACTURER)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_MODEL, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_MODEL)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_OS_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.ANDROID_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_PLATFORM, JsonObjects.BlobHeader.Attributes.VALUE_PLATFORM);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_SERIAL_HASH, cursor.isNull(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH)) ? JSONObject.NULL
                            : cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_SERIAL_NUMBER_HASH)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_SDK_LEVEL, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.ANDROID_SDK)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_DEVICE_TELEPHONY_ID, cursor.isNull(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_TELEPHONY_ID)) ? JSONObject.NULL
                            : cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.DEVICE_TELEPHONY_ID)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_API_KEY, apiKey);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_CLIENT_LIBRARY_VERSION, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALYTICS_LIBRARY_VERSION)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALYTICS_DATA_TYPE, JsonObjects.BlobHeader.Attributes.VALUE_DATA_TYPE);
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALE_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALE_COUNTRY)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_LOCALE_LANGUAGE, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.LOCALE_LANGUAGE)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_NETWORK_CARRIER, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_CARRIER)));
                    result.put(JsonObjects.BlobHeader.Attributes.KEY_NETWORK_COUNTRY, cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.NETWORK_COUNTRY)));

                    return result;
                }

                throw new RuntimeException("No session exists"); //$NON-NLS-1$
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Converts an event into a JSON object.
         * <p>
         * There are three types of events: open, close, and application. Open and close events are Localytics events, while
         * application events are generated by the app. The return value of this method will vary based on the type of event that
         * is being converted.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param context Application context. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event to convert.
         * @param blobId {@link UploadBlobEventsDbColumns#_ID} of the upload blob that contains this event.
         * @param apiKey the Localytics API key. Cannot be null.
         * @return JSON representation of the event.
         * @throws JSONException if a problem occurred converting the element to JSON.
         */
        /* package */static JSONObject convertEventToJson(final LocalyticsProvider provider, final Context context, final long eventId, final long blobId, final String apiKey)
                                                                                                                                                                               throws JSONException
        {
            final JSONObject result = new JSONObject();

            Cursor cursor = null;

            try
            {
                cursor = provider.query(EventsDbColumns.TABLE_NAME, null, String.format("%s = ?", EventsDbColumns._ID), new String[] //$NON-NLS-1$
                    { Long.toString(eventId) }, EventsDbColumns._ID);

                if (cursor.moveToFirst())
                {
                    final String eventName = cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.EVENT_NAME));
                    final long sessionId = getSessionIdForEventId(provider, eventId);
                    final String sessionUuid = getSessionUuid(provider, sessionId);
                    final long sessionStartTime = getSessionStartTime(provider, sessionId);

                    if (OPEN_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.SessionOpen.KEY_DATA_TYPE, JsonObjects.SessionOpen.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionOpen.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionOpen.KEY_EVENT_UUID, sessionUuid);

                        /*
                         * Both the database and the web service use 1-based indexing.
                         */
                        result.put(JsonObjects.SessionOpen.KEY_COUNT, sessionId);
                    }
                    else if (CLOSE_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.SessionClose.KEY_DATA_TYPE, JsonObjects.SessionClose.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionClose.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.SessionClose.KEY_SESSION_UUID, sessionUuid);
                        result.put(JsonObjects.SessionClose.KEY_SESSION_START_TIME, Math.round((double) sessionStartTime / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionClose.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));

                        /*
                         * length is a special case, as it depends on the start time embedded in the session table
                         */
                        Cursor sessionCursor = null;
                        try
                        {
                            sessionCursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                                { SessionsDbColumns.SESSION_START_WALL_TIME }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF))) }, null); //$NON-NLS-1$

                            if (sessionCursor.moveToFirst())
                            {
                                result.put(JsonObjects.SessionClose.KEY_SESSION_LENGTH_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                        / DateUtils.SECOND_IN_MILLIS)
                                        - Math.round((double) sessionCursor.getLong(sessionCursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME))
                                                / DateUtils.SECOND_IN_MILLIS));
                            }
                            else
                            {
                                // this should never happen
                                throw new RuntimeException("Session didn't exist"); //$NON-NLS-1$
                            }
                        }
                        finally
                        {
                            if (null != sessionCursor)
                            {
                                sessionCursor.close();
                                sessionCursor = null;
                            }
                        }

                        /*
                         * The close also contains a special case element for the screens history
                         */
                        Cursor eventHistoryCursor = null;
                        try
                        {
                            eventHistoryCursor = provider.query(EventHistoryDbColumns.TABLE_NAME, new String[]
                                { EventHistoryDbColumns.NAME }, String.format("%s = ? AND %s = ?", EventHistoryDbColumns.SESSION_KEY_REF, EventHistoryDbColumns.TYPE), new String[] { Long.toString(sessionId), Integer.toString(EventHistoryDbColumns.TYPE_SCREEN) }, EventHistoryDbColumns._ID); //$NON-NLS-1$

                            final JSONArray screens = new JSONArray();
                            while (eventHistoryCursor.moveToNext())
                            {
                                screens.put(eventHistoryCursor.getString(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME)));
                            }

                            if (screens.length() > 0)
                            {
                                result.put(JsonObjects.SessionClose.KEY_FLOW_ARRAY, screens);
                            }
                        }
                        finally
                        {
                            if (null != eventHistoryCursor)
                            {
                                eventHistoryCursor.close();
                                eventHistoryCursor = null;
                            }
                        }
                    }
                    else if (OPT_IN_EVENT.equals(eventName) || OPT_OUT_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.OptEvent.KEY_DATA_TYPE, JsonObjects.OptEvent.VALUE_DATA_TYPE);
                        result.put(JsonObjects.OptEvent.KEY_API_KEY, apiKey);
                        result.put(JsonObjects.OptEvent.KEY_OPT, OPT_OUT_EVENT.equals(eventName) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
                        result.put(JsonObjects.OptEvent.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                    }
                    else if (FLOW_EVENT.equals(eventName))
                    {
                        result.put(JsonObjects.EventFlow.KEY_DATA_TYPE, JsonObjects.EventFlow.VALUE_DATA_TYPE);
                        result.put(JsonObjects.EventFlow.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.EventFlow.KEY_SESSION_START_TIME, Math.round((double) sessionStartTime / DateUtils.SECOND_IN_MILLIS));

                        /*
                         * Need to generate two objects: the old flow events and the new flow events
                         */

                        /*
                         * Default sort order is ascending by _ID, so these will be sorted chronologically.
                         */
                        Cursor eventHistoryCursor = null;
                        try
                        {
                            eventHistoryCursor = provider.query(EventHistoryDbColumns.TABLE_NAME, new String[]
                                {
                                    EventHistoryDbColumns.TYPE,
                                    EventHistoryDbColumns.PROCESSED_IN_BLOB,
                                    EventHistoryDbColumns.NAME }, String.format("%s = ? AND %s <= ?", EventHistoryDbColumns.SESSION_KEY_REF, EventHistoryDbColumns.PROCESSED_IN_BLOB), new String[] { Long.toString(sessionId), Long.toString(blobId) }, EventHistoryDbColumns._ID); //$NON-NLS-1$

                            final JSONArray newScreens = new JSONArray();
                            final JSONArray oldScreens = new JSONArray();
                            while (eventHistoryCursor.moveToNext())
                            {
                                final String name = eventHistoryCursor.getString(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.NAME));
                                final String type;
                                if (EventHistoryDbColumns.TYPE_EVENT == eventHistoryCursor.getInt(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.TYPE)))
                                {
                                    type = JsonObjects.EventFlow.Element.TYPE_EVENT;
                                }
                                else
                                {
                                    type = JsonObjects.EventFlow.Element.TYPE_SCREEN;
                                }

                                if (blobId == eventHistoryCursor.getLong(eventHistoryCursor.getColumnIndexOrThrow(EventHistoryDbColumns.PROCESSED_IN_BLOB)))
                                {
                                    newScreens.put(new JSONObject().put(type, name));
                                }
                                else
                                {
                                    oldScreens.put(new JSONObject().put(type, name));
                                }
                            }

                            result.put(JsonObjects.EventFlow.KEY_FLOW_NEW, newScreens);
                            result.put(JsonObjects.EventFlow.KEY_FLOW_OLD, oldScreens);
                        }
                        finally
                        {
                            if (null != eventHistoryCursor)
                            {
                                eventHistoryCursor.close();
                                eventHistoryCursor = null;
                            }
                        }
                    }
                    else
                    {
                        /*
                         * This is a normal application event
                         */

                        result.put(JsonObjects.SessionEvent.KEY_DATA_TYPE, JsonObjects.SessionEvent.VALUE_DATA_TYPE);
                        result.put(JsonObjects.SessionEvent.KEY_WALL_TIME_SECONDS, Math.round((double) cursor.getLong(cursor.getColumnIndex(EventsDbColumns.WALL_TIME))
                                / DateUtils.SECOND_IN_MILLIS));
                        result.put(JsonObjects.SessionEvent.KEY_EVENT_UUID, cursor.getString(cursor.getColumnIndexOrThrow(EventsDbColumns.UUID)));
                        result.put(JsonObjects.SessionEvent.KEY_SESSION_UUID, sessionUuid);
                        result.put(JsonObjects.SessionEvent.KEY_NAME, eventName.substring(context.getPackageName().length() + 1, eventName.length()));

                        final JSONObject attributes = convertAttributesToJson(provider, eventId);

                        if (null != attributes)
                        {
                            result.put(JsonObjects.SessionEvent.KEY_ATTRIBUTES, attributes);
                        }
                    }
                }
                else
                {
                    /*
                     * This should never happen
                     */
                    throw new RuntimeException();
                }
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }

            return result;
        }

        /**
         * Private helper to get the {@link SessionsDbColumns#_ID} for a given {@link EventsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#_ID} of the session that owns the event.
         */
        /* package */static long getSessionIdForEventId(final LocalyticsProvider provider, final long eventId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(EventsDbColumns.TABLE_NAME, new String[]
                    { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ?", EventsDbColumns._ID), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Private helper to get the {@link SessionsDbColumns#UUID} for a given {@link SessionsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param sessionId {@link SessionsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#UUID} of the session.
         */
        /* package */static String getSessionUuid(final LocalyticsProvider provider, final long sessionId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                    { SessionsDbColumns.UUID }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getString(cursor.getColumnIndexOrThrow(SessionsDbColumns.UUID));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Private helper to get the {@link SessionsDbColumns#SESSION_START_WALL_TIME} for a given {@link SessionsDbColumns#_ID}.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param sessionId {@link SessionsDbColumns#_ID} of the event to look up
         * @return The {@link SessionsDbColumns#SESSION_START_WALL_TIME} of the session.
         */
        /* package */static long getSessionStartTime(final LocalyticsProvider provider, final long sessionId)
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(SessionsDbColumns.TABLE_NAME, new String[]
                    { SessionsDbColumns.SESSION_START_WALL_TIME }, String.format("%s = ?", SessionsDbColumns._ID), new String[] { Long.toString(sessionId) }, null); //$NON-NLS-1$

                if (cursor.moveToFirst())
                {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(SessionsDbColumns.SESSION_START_WALL_TIME));
                }

                /*
                 * This should never happen
                 */
                throw new RuntimeException();
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Private helper to convert an event's attributes into a {@link JSONObject} representation.
         *
         * @param provider Localytics database instance. Cannot be null.
         * @param eventId {@link EventsDbColumns#_ID} of the event whose attributes are to be loaded.
         * @return {@link JSONObject} representing the attributes of the event. The order of attributes is undefined and may
         *         change from call to call of this method. If the event has no attributes, returns null.
         * @throws JSONException if an error occurs converting the attributes to JSON
         */
        /* package */static JSONObject convertAttributesToJson(final LocalyticsProvider provider, final long eventId) throws JSONException
        {
            Cursor cursor = null;
            try
            {
                cursor = provider.query(AttributesDbColumns.TABLE_NAME, null, String.format("%s = ?", AttributesDbColumns.EVENTS_KEY_REF), new String[] { Long.toString(eventId) }, null); //$NON-NLS-1$

                if (cursor.getCount() == 0)
                {
                    return null;
                }

                final JSONObject attributes = new JSONObject();

                final int keyColumn = cursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_KEY);
                final int valueColumn = cursor.getColumnIndexOrThrow(AttributesDbColumns.ATTRIBUTE_VALUE);
                while (cursor.moveToNext())
                {
                    attributes.put(cursor.getString(keyColumn), cursor.getString(valueColumn));
                }

                return attributes;
            }
            finally
            {
                if (null != cursor)
                {
                    cursor.close();
                }
            }
        }

        /**
         * Given an id of an upload blob, get the session id associated with that blob.
         *
         * @param blobId {@link UploadBlobsDbColumns#_ID} of the upload blob.
         * @return id of the parent session.
         */
        /* package */long getSessionIdForBlobId(final long blobId)
        {
            /*
             * This implementation needs to walk up the tree of database elements.
             */

            long eventId;
            {
                Cursor cursor = null;
                try
                {
                    cursor = mProvider.query(UploadBlobEventsDbColumns.TABLE_NAME, new String[]
                        { UploadBlobEventsDbColumns.EVENTS_KEY_REF }, String.format("%s = ?", UploadBlobEventsDbColumns.UPLOAD_BLOBS_KEY_REF), new String[] //$NON-NLS-1$
                        { Long.toString(blobId) }, null);

                    if (cursor.moveToFirst())
                    {
                        eventId = cursor.getLong(cursor.getColumnIndexOrThrow(UploadBlobEventsDbColumns.EVENTS_KEY_REF));
                    }
                    else
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("No events associated with blob"); //$NON-NLS-1$
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                    }
                }
            }

            long sessionId;
            {
                Cursor cursor = null;
                try
                {
                    cursor = mProvider.query(EventsDbColumns.TABLE_NAME, new String[]
                        { EventsDbColumns.SESSION_KEY_REF }, String.format("%s = ?", EventsDbColumns._ID), new String[] //$NON-NLS-1$
                        { Long.toString(eventId) }, null);

                    if (cursor.moveToFirst())
                    {
                        sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(EventsDbColumns.SESSION_KEY_REF));
                    }
                    else
                    {
                        /*
                         * This should never happen
                         */
                        throw new RuntimeException("No session associated with event"); //$NON-NLS-1$
                    }
                }
                finally
                {
                    if (null != cursor)
                    {
                        cursor.close();
                    }
                }
            }

            return sessionId;
        }
    }

    /**
     * Internal helper class to pass two objects to the Handler via the {@link Message#obj}.
     */
    /*
     * Once support for Android 1.6 is dropped, using Android's built-in Pair class would be preferable
     */
    private static final class Pair<F, S>
    {
        public final F first;

        public final S second;

        public Pair(final F first, final S second)
        {
            this.first = first;
            this.second = second;
        }
    }
}
