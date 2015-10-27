package org.tasks.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.widget.WidgetConfigActivity;

import org.tasks.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

import static android.content.SharedPreferences.Editor;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;

public class Preferences {

    private static final Logger log = LoggerFactory.getLogger(Preferences.class);

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$
    private static final String P_CURRENT_VERSION_NAME = "cvname"; //$NON-NLS-1$

    private static final String PREF_SORT_SORT = "sort_sort"; //$NON-NLS-1$

    private static final String FILE_APPENDER_NAME = "FILE";

    protected final Context context;
    private DeviceInfo deviceInfo;
    private final SharedPreferences prefs;
    private final SharedPreferences publicPrefs;

    @Inject
    public Preferences(@ForApplication Context context, DeviceInfo deviceInfo) {
        this.context = context;
        this.deviceInfo = deviceInfo;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        publicPrefs = context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS, Context.MODE_WORLD_READABLE);
    }

    public boolean quietHoursEnabled() {
        return getBoolean(R.string.p_rmd_enable_quiet, false);
    }

    public int getDateShortcutMorning() {
        return getMillisPerDayPref(R.string.p_date_shortcut_morning, R.integer.default_morning);
    }

    public int getDateShortcutAfternoon() {
        return getMillisPerDayPref(R.string.p_date_shortcut_afternoon, R.integer.default_afternoon);
    }

    public int getDateShortcutEvening() {
        return getMillisPerDayPref(R.string.p_date_shortcut_evening, R.integer.default_evening);
    }

    public int getDateShortcutNight() {
        return getMillisPerDayPref(R.string.p_date_shortcut_night, R.integer.default_night);
    }

    private int getMillisPerDayPref(int resId, int defResId) {
        int defaultValue = context.getResources().getInteger(defResId);
        int setting = getInt(resId, defaultValue);
        return setting < 0 || setting > DateTime.MAX_MILLIS_PER_DAY ? defaultValue : setting;
    }

    public boolean useDarkWidgetTheme(int widgetId) {
        boolean legacySetting = getBoolean(R.string.p_use_dark_theme_widget, false);
        return getBoolean(WidgetConfigActivity.PREF_DARK_THEME + widgetId, legacySetting);
    }

    public boolean isDefaultCalendarSet() {
        String defaultCalendar = getDefaultCalendar();
        return defaultCalendar != null && !defaultCalendar.equals("-1") && !defaultCalendar.equals("0");
    }

    public String getDefaultCalendar() {
        return getStringValue(R.string.gcal_p_default);
    }

    public boolean geofencesEnabled() {
        return deviceInfo.supportsLocationServices() && getBoolean(R.string.p_geofence_reminders_enabled, true);
    }

    public void clear() {
        prefs
                .edit()
                .clear()
                .commit();
    }

    public void setDefaults() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences_defaults, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_appearance, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_misc, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_reminders, true);

        BeastModePreferences.setDefaultOrder(this, context);
    }

    public void reset() {
        clear();
        setDefaults();
    }

    public String getStringValue(String key) {
        return prefs.getString(key, null);
    }

    public String getStringValue(int keyResource) {
        return prefs.getString(context.getResources().getString(keyResource), null);
    }

    public int getDefaultReminders() {
        return getIntegerFromString(R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
    }

    public int getDefaultRingMode() {
        return getIntegerFromString(R.string.p_default_reminders_mode_key, 0);
    }

    public int getIntegerFromString(int keyResource, int defaultValue) {
        Resources r = context.getResources();
        String value = prefs.getString(r.getString(keyResource), null);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return defaultValue;
        }
    }

    public void setString(int key, String newValue) {
        setString(context.getString(key), newValue);
    }

    public void setString(String key, String newValue) {
        Editor editor = prefs.edit();
        editor.putString(key, newValue);
        editor.commit();
    }

    public void setStringFromInteger(int keyResource, int newValue) {
        Editor editor = prefs.edit();
        editor.putString(context.getString(keyResource), Integer.toString(newValue));
        editor.commit();
    }

    public boolean getBoolean(String key, boolean defValue) {
        try {
            return prefs.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            log.error(e.getMessage(), e);
            return defValue;
        }
    }

    public boolean notificationsEnabled() {
        return getBoolean(R.string.p_rmd_enabled, true);
    }

    public boolean fieldMissedPhoneCalls() {
        return getBoolean(R.string.p_field_missed_calls, true) && notificationsEnabled();
    }

    public boolean getBoolean(int keyResources, boolean defValue) {
        return getBoolean(context.getString(keyResources), defValue);
    }

    public void setBoolean(int keyResource, boolean value) {
        setBoolean(context.getString(keyResource), value);
    }

    public void setBoolean(String key, boolean value) {
        Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public int getInt(int resourceId) {
        return getInt(resourceId, 0);
    }

    public int getInt(int resourceId, int defValue) {
        return getInt(context.getString(resourceId), defValue);
    }

    public int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    public void setInt(int resourceId, int value) {
        setInt(context.getString(resourceId), value);
    }

    public void setInt(String key, int value) {
        Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }

    public void setLong(String key, long value) {
        Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void clear(String key) {
        Editor editor = prefs.edit();
        editor.remove(key);
        editor.commit();
    }

    public int getCurrentVersion() {
        return getInt(P_CURRENT_VERSION, 0);
    }

    public void setCurrentVersion(int version) {
        setInt(P_CURRENT_VERSION, version);
    }

    public void setCurrentVersionName(String versionName) {
        setString(P_CURRENT_VERSION_NAME, versionName);
    }

    public int getSortMode() {
        return publicPrefs.getInt(PREF_SORT_SORT, SortHelper.SORT_AUTO);
    }

    public void setSortMode(int value) {
        setPublicPref(PREF_SORT_SORT, value);
    }

    private void setPublicPref(String key, int value) {
        if (publicPrefs != null) {
            Editor edit = publicPrefs.edit();
            if (edit != null) {
                edit.putInt(key, value).commit();
            }
        }
    }

    public void setupLogger() {
        setupLogger(getBoolean(R.string.p_debug_logging, false));
    }

    public void setupLogger(boolean enableDebugLogging) {
        try {
            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (enableDebugLogging) {
                rootLogger.setLevel(Level.DEBUG);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    attachRollingFileAppender(rootLogger);
                }
            } else {
                rootLogger.setLevel(Level.INFO);
                rootLogger.detachAppender(FILE_APPENDER_NAME);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void attachRollingFileAppender(ch.qos.logback.classic.Logger rootLogger) {
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final RollingFileAppender<ILoggingEvent> rfa = new RollingFileAppender<ILoggingEvent>() {{
            setName(FILE_APPENDER_NAME);
            setContext(loggerContext);
            setFile(path + "/tasks-debug.log");
            setEncoder(new PatternLayoutEncoder() {{
                setContext(loggerContext);
                setPattern("%date [%thread] %-5level %logger{35} - %msg%n");
                start();
            }});
            setTriggeringPolicy(new SizeBasedTriggeringPolicy<ILoggingEvent>() {{
                setMaxFileSize("5MB");
                start();
            }});
        }};
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy() {{
            setContext(loggerContext);
            setParent(rfa);
            setMinIndex(1);
            setMaxIndex(3);
            setFileNamePattern(path + "/tasks-debug.%i.log.zip");
            start();
        }};
        rfa.setRollingPolicy(rollingPolicy);
        rfa.start();
        rootLogger.addAppender(rfa);
    }

    public boolean useNotificationActions() {
        return atLeastJellybean() && getBoolean(R.string.p_rmd_notif_actions_enabled, true);
    }

    public File getAttachmentsDirectory() {
        File directory = null;
        String customDir = getStringValue(R.string.p_attachment_dir);
        if (!TextUtils.isEmpty(customDir)) {
            directory = new File(customDir);
        }

        if (directory == null || !directory.exists()) {
            directory = getExternalFilesDir(TaskAttachment.FILES_DIRECTORY_DEFAULT);
        }

        return directory;
    }

    private File getExternalFilesDir(String type) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String directory = String.format("%s/Android/data/%s/files/%s", Environment.getExternalStorageDirectory(), context.getPackageName(), type);
            File file = new File(directory);
            if (file.isDirectory() || file.mkdirs()) {
                return file;
            }
        }

        return null;
    }

    public String getNewAudioAttachmentPath(AtomicReference<String> nameReference) {
        return getNewAttachmentPath(".m4a", nameReference); //$NON-NLS-1$
    }

    public String getNewAttachmentPath(String extension, AtomicReference<String> nameReference) {
        String dir = getAttachmentsDirectory().getAbsolutePath();

        String name = getNonCollidingFileName(dir, new DateTime().toString("yyyyMMddHHmm"), extension);

        if (nameReference != null) {
            nameReference.set(name);
        }

        return dir + File.separator + name;
    }

    private static String getNonCollidingFileName(String dir, String baseName, String extension) {
        int tries = 1;
        File f = new File(dir + File.separator + baseName + extension);
        String tempName = baseName;
        while (f.exists()) {
            tempName = baseName + "-" + tries; //$NON-NLS-1$
            f = new File(dir + File.separator + tempName + extension);
            tries++;
        }
        return tempName + extension;
    }

    public File getBackupDirectory() {
        File directory = null;
        String customDir = getStringValue(R.string.p_backup_dir);
        if (!TextUtils.isEmpty(customDir)) {
            directory = new File(customDir);
        }

        if (directory == null || !directory.exists()) {
            directory = defaultExportDirectory();
        }

        return directory;
    }

    /**
     * @return export directory for tasks, or null if no SD card
     */
    private static File defaultExportDirectory() {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            path = path + "/astrid";
            return new File(path);
        }
        return null;
    }
}
