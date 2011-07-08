/**
 * LocalyticsSession.java
 * Copyright (C) 2009 Char Software Inc., DBA Localytics
 *
 *  This code is provided under the Localytics Modified BSD License.
 *  A copy of this license has been distributed in a file called LICENSE
 *  with this source code.
 *
 *  Please visit www.localytics.com for more information.
 */

package com.localytics.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * The class which manages creating, collecting, & uploading a Localytics session.
 * Please see the following guides for information on how to best use this
 * library, sample code, and other useful information:
 * <ul>
 * <li><a href="http://wiki.localytics.com/index.php?title=Developer's_Integration_Guide">Main Developer's Integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_2_Minute_Integration">Android 2 minute integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_Integration_Guide">Android Integration Guide</a></li>
 * </ul>
 * <p>
 * Permissions required or recommended for this class:
 * <ul>
 * <li>android.permission.INTERNET</li> - Required.  Necessary to upload data to the webservice.</li>
 * <li>android.permission.ACCESS_WIFI_STATE</li> - Optional.  Without this users connecting via WIFI will show up as
 * having a connection type of 'unknown' on the webservice</li>
 * </ul>
 *
 * <strong>Best Practices</strong>
 * <ul>
 * <li>Instantiate the LocalyticsSession object in onCreate.</li>
 * <li>Create a new LocalyticsSession object, and open it in the onCreaet
 * of every activity in your application.  This will cause every new
 * activity you display to reconnect to the running session.</li>
 * <li>Open your session and begin your uploads in onCreate. This way the
 * upload has time to complete and it all happens before your users have a
 * chance to begin any data intensive actions of their own.</li>
 * <li>Close the session in onPause.  This is the last terminating function
 * which is guaranteed to be called.  The final close is the only one
 * considered so worrying about activity re-entrance is not a problem.</li>
 * <li>Do not call any Localytics functions inside a loop.  Instead, calls
 * such as <code>tagEvent</code> should follow user actions.  This limits the
 * amount of data which is stored and uploaded.</li>
 * <li>Do not use multiple LocalticsSession objects to upload data with
 * multiple application keys.  This can cause invalid state.</li>
 * </ul>
 * @author Localytics
 * @version 1.5
 */
@SuppressWarnings("nls")
public final class LocalyticsSession
{
    ////////////////////////////////////////
    // Member Variables ////////////////////
    ////////////////////////////////////////
    private String _localyticsDirPath;		// Path for this app's Localytics Files
    private String _sessionFilename = null; // Filename for this session
    private String _closeFilename = null;   // Filename for this session's close events
    private String _sessionUUID;			// Unique identifier for this session.
    private String _applicationKey;         // Unique identifier for the instrumented application

    private Context _appContext;			// The context used to access device resources

    private boolean _isSessionOpen = false;	// Whether or not this session has been opened.

    private static boolean _isUploading = false;  // Only allow one instance of the app to upload at once.
    private static boolean _isOptedIn = false;    // Optin/out needs to be shared by all instances of this class.

    ////////////////////////////////////////
    // Constants ///////////////////////////
    ////////////////////////////////////////
    private static final String CLIENT_VERSION = "1.5";  // The version of this library.
    private static final int MAX_NUM_SESSIONS = 10;      // Number of sessions to store on the disk
    private static final int MAX_NUM_ATTRIBUTES = 10;    // Maximum attributes per event session
    protected static final int MAX_NAME_LENGTH = 128;      // Maximum characters in an event name or attribute key/value

    // Filename and directory constants.
    private static final String LOCALYTICS_DIR       = "localytics";
    private static final String SESSION_FILE_PREFIX  = "s_";
    private static final String UPLOADER_FILE_PREFIX = "u_";
    private static final String CLOSE_FILE_PREFIX    = "c_";
    private static final String OPTOUT_FILNAME       = "opted_out";
    private static final String DEVICE_ID_FILENAME   = "device_id";
    private static final String SESSION_ID_FILENAME  = "last_session_id";

    // All session opt-in / opt-out events need to be written to same place to gaurantee ordering on the server.
    private static final String OPT_SESSION = LocalyticsSession.SESSION_FILE_PREFIX + "opt_session";

    // The tag used for identifying Localytics Log messages.
    private static final String LOG_TAG = "Localytics_Session";

    // The number of milliseconds after which a session is considered closed and can't be reattached to
    // 15 seconds * 1000ms
    private static int SESSION_EXPIRATION = 15 * 1000;

    ////////////////////////////////////////
    // Public Methods //////////////////////
    ////////////////////////////////////////
    /**
     * Creates the Localytics Object.  If Localytics is opted out at the time
     * this object is created, no data will be collected for the lifetime of
     * this session.
     * @param appContext The context used to access resources on behalf of the app.
     * It is recommended to use <code>getApplicationContext</code> to avoid the potential
     * memory leak incurred by maintaining references to activities.
     * @param applicationKey The key unique for each application generated
     * at www.localytics.com
     */
    public LocalyticsSession(final Context appContext, final String applicationKey)
    {
        this._appContext = appContext;
        this._applicationKey = applicationKey;

        // Put each application key's files inside a different directory.  This
        // makes it possible to have multiple app keys inside a single application.
        // However, this is not a recommended practice!
        this._localyticsDirPath = appContext.getFilesDir() + "/"
        + LocalyticsSession.LOCALYTICS_DIR + "/"
        + this._applicationKey + "/";

        // All Localytics API calls are wrapped in try / catch blobs which swallow
        // all exceptions.  This way if there is a problem with the library the
        // integrating application does not crash.
        try
        {
            // If there is an opt-out file, everything is opted out.
            File optOutFile = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);
            if(optOutFile.exists())
            {
                LocalyticsSession._isOptedIn = false;
                return;
            }

            // Otherwise, everything is opted in.
            LocalyticsSession._isOptedIn = true;
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    /**
     * Sets the Localytics Optin state for this application.  This
     * call is not necessary and is provided for people who wish to allow
     * their users the ability to opt out of data collection.  It can be
     * called at any time.  Passing false causes all further data collection
     * to stop, and an opt-out event to be sent to the server so the user's
     * data is removed from the charts.
     * <br>
     * There are very serious implications to the quality of your data when
     * providing an opt out option.  For example, users who have opted out will appear
     * as never returning, causing your new/returning chart to skew.
     * <br>
     * If two instances of the same application are running, and one
     * is opted in and the second opts out, the first will also become opted
     * out, and neither will collect any more data.
     * <br>
     * If a session was started while the app was opted out, the session open
     * event has already been lost.  For this reason, all sessions started
     * while opted out will not collect data even after the user opts back in
     * or else it taints the comparisons of session lengths and other metrics.
     * @param optedIn True if the user wishes to be opted in, false if they
     * wish to be opted out and have all their Localytics data deleted.
     */
    public void setOptIn(final boolean optedIn)
    {
        try
        {
            // Do nothing if optin is unchanged
            if(optedIn == LocalyticsSession._isOptedIn)
            {
                return;
            }

            LocalyticsSession._isOptedIn = optedIn;
            File fp;

            if(optedIn == true)
            {
                // To opt in, delete the opt out file if it exists.
                fp = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);
                fp.delete();

                createOptEvent(true);
            }
            else
            {
                // Create the opt-out file.  If it can't be written this is fine because
                // it means session files can't be written either so the user is effectively opted out.
                fp = new File(this._localyticsDirPath);
                fp.mkdirs();
                fp = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);
                try
                {
                    fp.createNewFile();
                }
                catch (IOException e) { /**/ }

                createOptEvent(false);
            }
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    /**
     * Checks whether or not this session is opted in.
     * It is not recommended that an application branch on analytics code
     * because this adds an unnecessary testing burden to the developer.
     * However, this function is provided for developers who wish to
     * pre-populate check boxes in settings menus.
     * @return True if the user is opted in, false otherwise.
     */
    public boolean isOptedIn()
    {
        return LocalyticsSession._isOptedIn;
    }

    /**
     * Opens the Localytics session.  The session time as presented on the
     * website is the time between the first <code>open</code> and the final <code>close</code>
     * so it is recommended to open the session as early as possible, and close
     * it at the last moment.  The session must be opened before any tags can
     * be written.  It is recommended that this call be placed in <code>onCreate</code>.
     * <br>
     * If for any reason this is called more than once every subsequent open call
     * will be ignored.
     * <br>
     * For applications with multiple activites, every activity should call <code>open</code>
     * in <code>onCreate</code>.  This will cause each activity to reconnect to the currently
     * running session.
     */
    public void open()
    {
        // Allow only one open call to happen.
        synchronized(LocalyticsSession.class)
        {
            if(LocalyticsSession._isOptedIn == false ||  // do nothing if opted out
                    this._isSessionOpen == true)			     // do nothing if already open
            {
                Log.v(LocalyticsSession.LOG_TAG, "Session not opened");
                return;
            }

            this._isSessionOpen = true;
        }

        try
        {
            // Check if this session was closed within the last 15 seconds.  If so, reattach
            // to that session rather than open a new one.  This will be the case whenever
            // a new activity is loaded, or the current one is redrawn.  Otherwise, create
            // a new session
            this._sessionUUID = getOldSessionUUId();
            if(this._sessionUUID != null)
            {
                this._sessionFilename = LocalyticsSession.SESSION_FILE_PREFIX + this._sessionUUID;
                this._closeFilename = LocalyticsSession.CLOSE_FILE_PREFIX + this._sessionUUID;;
                Log.v(LocalyticsSession.LOG_TAG, "Reconnected to existing session");
            }
            else
            {
                // if there are too many files on the disk already, return w/o doing anything.
                // All other session calls, such as tagEvent and close will return because isSessionOpen == false
                File fp = new File(this._localyticsDirPath);
                if(fp.exists())
                {
                    // Get a list of all the session files.
                    FilenameFilter filter = new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            // accept any session or uploader files, but ignore the close files b/c they are tiny.
                            return (name.startsWith(LocalyticsSession.SESSION_FILE_PREFIX)
                                    || name.startsWith(LocalyticsSession.UPLOADER_FILE_PREFIX));
                        }
                    };

                    // If that list is larger than the max number, don't create a new session.
                    if( fp.list(filter).length >= LocalyticsSession.MAX_NUM_SESSIONS)
                    {
                        this._isSessionOpen = false;
                        Log.v(LocalyticsSession.LOG_TAG, "Queue full, session not created");
                        return;
                    }
                }

                // Otherwise, prepare the session.
                this._sessionUUID = UUID.randomUUID().toString();
                this._sessionFilename = LocalyticsSession.SESSION_FILE_PREFIX + this._sessionUUID;
                this._closeFilename = LocalyticsSession.CLOSE_FILE_PREFIX + this._sessionUUID;;

                // It isn't necessary to have each session live in its own file because every event
                // has the session_id in it.  However, this makes maintaining a queue much simpler,
                // and it simplifies multithreading because different instances write to different files
                fp = getOrCreateFileWithDefaultPath(this._sessionFilename);
                if(fp == null)
                {
                    this._isSessionOpen = false;
                    return;
                }

                // If the file already exists then an open event has already been written.
                else if(fp.length() != 0)
                {
                    Log.v(LocalyticsSession.LOG_TAG, "Session already opened");
                    return;
                }

                appendDataToFile(fp, getOpenSessionString());
                Log.v(LocalyticsSession.LOG_TAG, "Session opened");
            }
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    /**
     * Closes the Localytics session.  This should be done when the application or
     * activity is ending.  Because of the way the Android lifecycle works, this
     * call could end up in a place which gets called multiple times (such as
     * <code>onPause</code> which is the recommended location).  This is
     * fine because only the last close is processed by the server.
     * <br>
     * Closing does not cause the session to stop collecting data.  This is a result
     * of the application life cycle.  It is possible for onPause to be called long
     * before the application is actually ready to close the session.
     */
    public void close()
    {
        if(LocalyticsSession._isOptedIn == false ||		// do nothing if opted out
                this._isSessionOpen == false) 	// do nothing if session is not open
        {
            Log.v(LocalyticsSession.LOG_TAG, "Session not closed.");
            return;
        }

        try
        {
            // Create the session close blob
            StringBuffer closeString = new StringBuffer();
            closeString.append(DatapointHelper.CONTROLLER_SESSION);
            closeString.append(DatapointHelper.ACTION_UPDATE);
            closeString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_UUID,
                    this._sessionUUID,
                    2));
            closeString.append(DatapointHelper.OBJECT_SESSION_DP);
            closeString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_APP_UUID,
                    this._applicationKey,
                    3));
            closeString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_CLIENT_CLOSED_TIME,
                    DatapointHelper.getTimeAsDatetime(),
                    3));

            // Overwrite the existing close event with the new one
            File fp = getOrCreateFileWithDefaultPath(this._closeFilename);
            overwriteFile(fp, closeString.toString());

            // Write this session id to disk along with a timestamp.  This is used to
            // determine whether a session is reconnnecting to an existing session or
            // being created fresh.
            fp = getOrCreateFileWithDefaultPath(LocalyticsSession.SESSION_ID_FILENAME);
            overwriteFile(fp,
                    this._sessionUUID + "\n" + Long.toString(System.currentTimeMillis()));

            Log.v(LocalyticsSession.LOG_TAG, "Close event written.");
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    /**
     * Allows a session to tag a particular event as having occurred.  For
     * example, if a view has three buttons, it might make sense to tag
     * each button click with the name of the button which was clicked.
     * For another example, in a game with many levels it might be valuable
     * to create a new tag every time the user gets to a new level in order
     * to determine how far the average user is progressing in the game.
     * <br>
     * <strong>Tagging Best Practices</strong>
     * <ul>
     * <li>DO NOT use tags to record personally identifiable information.</li>
     * <li>The best way to use tags is to create all the tag strings as predefined
     * constants and only use those.  This is more efficient and removes the risk of
     * collecting personal information.</li>
     * <li>Do not set tags inside loops or any other place which gets called
     * frequently.  This can cause a lot of data to be stored and uploaded.</li>
     * </ul>
     * <br>
     * @param event The name of the event which occurred.
     */
    public void tagEvent(final String event)
    {
        tagEvent(event, null);
    }

    /**
     * Allows a session to tag a particular event as having occurred, and
     * optionally attach a collection of attributes to it.  For example, if a
     * view has three buttons, it might make sense to tag each button with the
     * name of the button which was clicked.
     * For another example, in a game with many levels it might be valuable
     * to create a new tag every time the user gets to a new level in order
     * to determine how far the average user is progressing in the game.
     * <br>
     * <strong>Tagging Best Practices</strong>
     * <ul>
     * <li>DO NOT use tags to record personally identifiable information.</li>
     * <li>The best way to use tags is to create all the tag strings as predefined
     * constants and only use those.  This is more efficient and removes the risk of
     * collecting personal information.</li>
     * <li>Do not set tags inside loops or any other place which gets called
     * frequently.  This can cause a lot of data to be stored and uploaded.</li>
     * </ul>
     * <br>
     *
     * @param event The name of the event which occurred.
     * @param attributes The collection of attributes for this particular event.
     */
    public void tagEvent(final String event, final Map<String, String> attributes)
    {
        if(LocalyticsSession._isOptedIn == false || // do nothing if opted out
                this._isSessionOpen == false)       // do nothing if session is not open
        {
            Log.v(LocalyticsSession.LOG_TAG, "Tag not written");
            return;
        }

        try
        {
            // Create the YML for the event
            StringBuffer eventString = new StringBuffer();
            eventString.append(DatapointHelper.CONTROLLER_EVENT);
            eventString.append(DatapointHelper.ACTION_CREATE);
            eventString.append(DatapointHelper.OBJECT_EVENT_DP);
            eventString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_APP_UUID, this._applicationKey, 3));
            eventString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_UUID, UUID.randomUUID().toString(), 3));
            eventString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_SESSION_UUID, this._sessionUUID, 3));
            eventString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_CLIENT_TIME, DatapointHelper.getTimeAsDatetime(), 3));

            eventString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_EVENT_NAME, event, 3));

            if (attributes != null)
            {
                eventString.append(DatapointHelper.EVENT_ATTRIBUTE);

                // Iterate through the map's elements and append a line for each one
                Iterator<String> attr_it = attributes.keySet().iterator();
                for (int currentAttr = 0; attr_it.hasNext() && (currentAttr < MAX_NUM_ATTRIBUTES); currentAttr++)
                {
                    String key = (String) attr_it.next();
                    String value = (String) attributes.get(key);
                    eventString.append(DatapointHelper.formatYAMLLine(key, value, 4));
                }
            }

            File fp = getOrCreateFileWithDefaultPath(this._sessionFilename);
            appendDataToFile(fp, eventString.toString());
            Log.v(LocalyticsSession.LOG_TAG, "Tag written.");
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    /**
     * Sorts an int value into a set of regular intervals as defined by the
     * minimum, maximum, and step size. Both the min and max values are
     * inclusive, and in the instance where (max - min + 1) is not evenly
     * divisible by step size, the method guarantees only the minimum and the
     * step size to be accurate to specification, with the new maximum will be
     * moved to the next regular step.
     *
     * @param actualValue The int value to be sorted.
     * @param minValue The int value representing the inclusive minimum interval.
     * @param maxValue The int value representing the inclusive maximum interval.
     * @param step The int value representing the increment of each interval.
     */
    public String createRangedAttribute(int actualValue, int minValue, int maxValue, int step)
    {
        // Confirm there is at least one bucket
        if (step < 1)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Step must not be less than zero.  Returning null.");
            return null;
        }
        if (minValue >= maxValue)
        {
            Log.v(LocalyticsSession.LOG_TAG, "maxValue must not be less than minValue.  Returning null.");
            return null;
        }

        // Determine the number of steps, rounding up using int math
        int stepQuantity = (maxValue - minValue + step) / step;
        int[] steps = new int[stepQuantity + 1];
        for (int currentStep = 0; currentStep <= stepQuantity; currentStep++)
        {
            steps[currentStep] = minValue + (currentStep) * step;
        }
        return createRangedAttribute(actualValue, steps);
    }

    /**
     * Sorts an int value into a predefined, pre-sorted set of intervals, returning a string representing the
     * new expected value.  The array must be sorted in ascending order, with the first element representing
     * the inclusive lower bound and the last element representing the exclusive upper bound.  For instance,
     * the array [0,1,3,10] will provide the following buckets: less than 0, 0, 1-2, 3-9, 10 or greater.
     *
     * @param actualValue The int value to be bucketed.
     * @param steps The sorted int array representing the bucketing intervals.
     */
    public String createRangedAttribute(int actualValue, int[] steps)
    {
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
                bucketIndex = (- bucketIndex) - 2;
            }
            if (steps[bucketIndex] == (steps[bucketIndex + 1] - 1))
            {
                bucket = Integer.toString(steps[bucketIndex]);
            }
            else
            {
                bucket = steps[bucketIndex] + "-" + (steps[bucketIndex + 1] - 1);
            }
        }
        return bucket;
    }

    /**
     * Creates a low priority thread which uploads any Localytics data already stored
     * on the device.  This should be done early in the process life in order to
     * guarantee as much time as possible for slow connections to complete.  It
     * is necessary to do this even if the user has opted out because this is how
     * the opt out is transported to the webservice.
     */
    public void upload()
    {
        // Synchronize the check to make sure the upload is not
        // already happening.  This avoids the possibility of two
        // uploader threads being started at once. While this isn't necessary it could
        // conceivably reduce the load on the server
        synchronized(LocalyticsSession.class)
        {
            // Uploading should still happen even if the session is opted out.
            // This way the opt-out event gets sent to the server so we know this
            // user is opted out.  After that, no data will be collected so nothing
            // will get uploaded.
            if(LocalyticsSession._isUploading)
            {
                return;
            }

            LocalyticsSession._isUploading = true;
        }

        try
        {
            File fp = new File(this._localyticsDirPath);
            UploaderThread uploader = new UploaderThread(
                    fp,
                    LocalyticsSession.SESSION_FILE_PREFIX,
                    LocalyticsSession.UPLOADER_FILE_PREFIX,
                    LocalyticsSession.CLOSE_FILE_PREFIX,
                    this.uploadComplete);

            uploader.start();
        }
        catch (Exception e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "Swallowing exception: " + e.getMessage());
        }
    }

    ////////////////////////////////////////
    // Private Methods /////////////////////
    ////////////////////////////////////////
    /**
     * Gets a file from the application storage, or creates if it isn't there.
     * The file is created inside this session's default storage location which
     * makes it specific to the app key the user selects.
     * @param path relative path to create the file in. should not be seperator_terminated
     * @param filename the file to create
     * @param path to create the file in
     * @return a File object, or null if something goes wrong
     */
    private File getOrCreateFileWithDefaultPath(final String filename)
    {
        return getOrCreateFile(filename, this._localyticsDirPath);
    }

    /**
     * Gets a file from the application storage, or creates if it isn't there.
     * @param path relative path to create the file in. should not be seperator_terminated
     * @param filename the file to create
     * @param path to create the file in
     * @return a File object, or null if something goes wrong
     */
    private File getOrCreateFile(final String filename, final String path)
    {
        // Get the file if it already exists
        File fp = new File(path + filename);
        if(fp.exists())
        {
            return fp;
        }

        // Otherwise, create any necessary directories, and the file itself.
        new File(path).mkdirs();
        try
        {
            if(fp.createNewFile())
            {
                return fp;
            }
        }
        catch (IOException e)
        {
            Log.v(LocalyticsSession.LOG_TAG,
                    "Unable to get or create file: " + filename
                    + " in path: " + path);
        }

        return null;
    }

    /**
     * Uses an OutputStreamWriter to write and flush a string to the end of a text file.
     * @param file Text file to append data to.
     * @param data String to be appended
     */
    private static void appendDataToFile(final File file, final String data)
    {
        try
        {
            if(file != null)
            {
                // Only allow one append to happen at a time.  This gaurantees files don't get corrupted by
                // multiple threads in the same app writing at the same time, and it gaurantees app-wide
                // like device_id don't get broken by multiple instance apps.
                synchronized(LocalyticsSession.class)
                {
                    OutputStream out = new FileOutputStream(file, true);
                    out.write(data.getBytes("UTF8"));
                    out.close();
                }
            }
        }
        catch(IOException e)
        {
            Log.v(LocalyticsSession.LOG_TAG, "AppendDataToFile failed with IO Exception: " + e.getMessage());
        }
    }

    /**
     * Overwrites a given file with new contents
     * @param file file to overwrite
     * @param contents contents to store in the file
     */
    private static void overwriteFile(final File file, final String contents)
    {
        if(file != null)
        {
            try
            {
                FileWriter writer = new FileWriter(file);
                writer.write(contents);
                writer.flush();
                writer.close();
            }
            catch(IOException e)
            {
                Log.v(LocalyticsSession.LOG_TAG, "Ovewriting file failed with IO Exception: " + e.getMessage());
            }
        }
    }

    /**
     * Creates the YAML string for the open session event.
     * Collects all the basic session datapoints and writes them out as a YAML string.
     * @return The YAML blob for the open session event.
     */
    private String getOpenSessionString()
    {
        StringBuffer openString = new StringBuffer();
        TelephonyManager telephonyManager = (TelephonyManager)this._appContext.getSystemService(Context.TELEPHONY_SERVICE);
        Locale defaultLocale = Locale.getDefault();

        openString.append(DatapointHelper.CONTROLLER_SESSION);
        openString.append(DatapointHelper.ACTION_CREATE);
        openString.append(DatapointHelper.OBJECT_SESSION_DP);

        // Application and session information
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_UUID, this._sessionUUID, 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_APP_UUID, this._applicationKey, 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_APP_VERSION, DatapointHelper.getAppVersion(this._appContext), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_LIBRARY_VERSION, LocalyticsSession.CLIENT_VERSION, 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_CLIENT_TIME, DatapointHelper.getTimeAsDatetime(), 3));

        // Other device information
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_DEVICE_UUID, getDeviceId(), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_DEVICE_PLATFORM, "Android", 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_OS_VERSION, Build.ID, 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_DEVICE_MODEL, Build.MODEL, 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_LOCALE_LANGUAGE, defaultLocale.getLanguage(), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_LOCALE_COUNTRY, defaultLocale.getCountry(), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_DEVICE_COUNTRY, telephonyManager.getSimCountryIso(), 3));

        // Network information
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_NETWORK_CARRIER, telephonyManager.getNetworkOperatorName(), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso(), 3));
        openString.append(DatapointHelper.formatYAMLLine(
                DatapointHelper.PARAM_DATA_CONNECTION, DatapointHelper.getNetworkType(this._appContext, telephonyManager), 3));

        return openString.toString();
    }

    /**
     * Gets an identifier which is unique to this machine, but generated randomly
     * so it can't be traced.
     * @return Returns the deviceID as a string
     */
    private String getDeviceId()
    {
        // Try and get the global device ID.  If that fails, maintain an id
        // local to this application.  This way it is still possible to tell things like
        // 'new vs returning users' on the webservice.
        String deviceId = DatapointHelper.getGlobalDeviceId(this._appContext);
        if(deviceId == null)
        {
            deviceId = getLocalDeviceId();
        }

        return deviceId;
    }

    /**
     * Gets an identifier unique to this application on this device.  If one is not currently available,
     * a new one is generated and stored.
     * @return An identifier unique to this application this device.
     */
    private String getLocalDeviceId()
    {
        String deviceId = null;
        final int bufferSize = 100;

        // Open the device ID file.  This file is stored at the root level so that
        // if an application has multiple app keys (which it shouldn't!) all sessions
        // will have a common device_id.
        //File fp = getOrCreateFileWithDefaultPath(LocalyticsSession.DEVICE_ID_FILENAME);
        File fp = getOrCreateFile(
                LocalyticsSession.DEVICE_ID_FILENAME,
                this._appContext.getFilesDir() + "/"
                + LocalyticsSession.LOCALYTICS_DIR + "/");

        // if the file doesn't exist, create one.
        if(fp.length() == 0)
        {
            deviceId = UUID.randomUUID().toString();
            appendDataToFile(fp, deviceId);
        }
        else
        {
            try
            {
                // If it did exist, the file contains the ID.
                char[] buf = new char[100];
                int numRead;
                BufferedReader reader = new BufferedReader(new FileReader(fp), bufferSize);
                numRead = reader.read(buf);
                deviceId = String.copyValueOf(buf, 0, numRead);
                reader.close();
            }
            catch (FileNotFoundException e) { Log.v(LocalyticsSession.LOG_TAG, "GetLocalDeviceID failed with FNF: " + e.getMessage()); }
            catch (IOException e) { Log.v(LocalyticsSession.LOG_TAG, "GetLocalDeviceId Failed with IO Exception: " + e.getMessage()); }
        }

        return deviceId;
    }

    /**
     * Creates an event telling the webservice that the user opted in or out.
     * @param optState True if they opted in, false if they opted out.
     */
    private void createOptEvent(boolean optState)
    {
        File fp = getOrCreateFileWithDefaultPath(LocalyticsSession.OPT_SESSION);
        if(fp != null)
        {
            // Create the session close blob
            StringBuffer optString = new StringBuffer();
            optString.append(DatapointHelper.CONTROLLER_OPT);
            optString.append(DatapointHelper.ACTION_OPTIN);
            optString.append(DatapointHelper.OBJECT_OPT);

            optString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_DEVICE_UUID,
                    getDeviceId(),
                    3));

            optString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_APP_UUID,
                    this._applicationKey,
                    3));

            optString.append(DatapointHelper.formatYAMLLine(
                    DatapointHelper.PARAM_OPT_VALUE,
                    Boolean.toString(optState),
                    3));

            appendDataToFile(fp, optString.toString());
        }
    }

    /**
     * Returns the UUID of the last session which was closed if the last session
     * was closed within 15 seconds.  This allows sessions to be shared between
     * activities within the same application.
     *
     * @return the UUID of the previous LocalyticsSession or null if the session has been closed for too long.
     */
    private String getOldSessionUUId()
    {
        final int bufferSize = 100;

        // Open the stored session id file
        File fp = new File(this._localyticsDirPath + LocalyticsSession.SESSION_ID_FILENAME);
        if(fp.exists())
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(fp), bufferSize);
                String storedId = reader.readLine();
                String timeStamp = reader.readLine();
                reader.close();

                if(timeStamp != null)
                {
                    // Check if the session happened recently enough
                    long timeSinceSession = System.currentTimeMillis() - Long.parseLong(timeStamp);
                    if(SESSION_EXPIRATION > timeSinceSession)
                    {
                        return storedId;
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                Log.v(LocalyticsSession.LOG_TAG, "File Not Found opening stored session");
                return null;
            }
            catch (IOException e)
            {
                Log.v(LocalyticsSession.LOG_TAG, "IO Exception getting stored session: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Runnable which gets passed to the uploader thread so it can
     * notify the library when uploads are complete.
     */
    private final Runnable uploadComplete = new Runnable()
    {
        public void run()
        {
            LocalyticsSession._isUploading = false;
        }
    };
}
