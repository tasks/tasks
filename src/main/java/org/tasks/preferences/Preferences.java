package org.tasks.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.common.primitives.Longs;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import timber.log.Timber;

import static android.content.SharedPreferences.Editor;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybean;

public class Preferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$

    private static final String PREF_SORT_SORT = "sort_sort"; //$NON-NLS-1$

    private final Context context;
    private final PermissionChecker permissionChecker;
    private final SharedPreferences prefs;
    private final SharedPreferences publicPrefs;

    public Preferences(Context context) {
        this(context, new PermissionChecker(context));
    }

    @Inject
    public Preferences(@ForApplication Context context, PermissionChecker permissionChecker) {
        this.context = context;
        this.permissionChecker = permissionChecker;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        publicPrefs = context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS, Context.MODE_PRIVATE);
    }

    public boolean backButtonSavesTask() {
        return getBoolean(R.string.p_back_button_saves_task, false);
    }

    public boolean isCurrentlyQuietHours() {
        if (quietHoursEnabled()) {
            DateTime dateTime = new DateTime();
            DateTime start = dateTime.withMillisOfDay(getQuietHoursStart());
            DateTime end = dateTime.withMillisOfDay(getQuietHoursEnd());
            if (start.isAfter(end)) {
                return dateTime.isBefore(end) || dateTime.isAfter(start);
            } else {
                return dateTime.isAfter(start) && dateTime.isBefore(end);
            }
        }
        return false;
    }

    public long adjustForQuietHours(long time) {
        if (quietHoursEnabled()) {
            DateTime dateTime = new DateTime(time);
            DateTime start = dateTime.withMillisOfDay(getQuietHoursStart());
            DateTime end = dateTime.withMillisOfDay(getQuietHoursEnd());
            if (start.isAfter(end)) {
                if (dateTime.isBefore(end)) {
                    return end.getMillis();
                } else if (dateTime.isAfter(start)) {
                    return end.plusDays(1).getMillis();
                }
            } else {
                if (dateTime.isAfter(start) && dateTime.isBefore(end)) {
                    return end.getMillis();
                }
            }
        }
        return time;
    }

    public boolean quietHoursEnabled() {
        return getBoolean(R.string.p_rmd_enable_quiet, false);
    }

    public int getQuietHoursStart() {
        return getMillisPerDayPref(R.string.p_rmd_quietStart, R.integer.default_quiet_hours_start);
    }

    public int getQuietHoursEnd() {
        return getMillisPerDayPref(R.string.p_rmd_quietEnd, R.integer.default_quiet_hours_end);
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
        int setting = getInt(resId, -1);
        if (setting < 0 || setting > DateTime.MAX_MILLIS_PER_DAY) {
            return context.getResources().getInteger(defResId);
        }
        return setting;
    }

    public boolean isDefaultCalendarSet() {
        String defaultCalendar = getDefaultCalendar();
        return defaultCalendar != null && !defaultCalendar.equals("-1") && !defaultCalendar.equals("0");
    }

    public boolean isTrackingEnabled() {
        return getBoolean(R.string.p_collect_statistics, true);
    }

    public int getNotificationPriority() {
        return getIntegerFromString(R.string.p_notification_priority, 1);
    }

    public String getDefaultCalendar() {
        return getStringValue(R.string.gcal_p_default);
    }

    public int getFirstDayOfWeek() {
        int firstDayOfWeek = getIntegerFromString(R.string.p_start_of_week, 0);
        return firstDayOfWeek < 1 || firstDayOfWeek > 7 ? 0 : firstDayOfWeek;
    }

    public void clear() {
        prefs
                .edit()
                .clear()
                .commit();
    }

    public void setDefaults() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_appearance, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_date_time, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_defaults, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_gtasks, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_misc, true);
        PreferenceManager.setDefaultValues(context, R.xml.preferences_privacy, true);
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

    public boolean isStringValueSet(int keyResource) {
        return !TextUtils.isEmpty(getStringValue(keyResource));
    }

    public int getDefaultReminders() {
        return getIntegerFromString(R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
    }

    public int getDefaultRingMode() {
        return getIntegerFromString(R.string.p_default_reminders_mode_key, 0);
    }

    public int getRowPadding() {
        return getInt(R.string.p_rowPadding, 16);
    }

    public int getFontSize() {
        return getInt(R.string.p_fontSize, 16);
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
            Timber.e(e, e.getMessage());
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
            Timber.e(e, e.getMessage());
            return defValue;
        }
    }

    private boolean notificationsEnabled() {
        return getBoolean(R.string.p_rmd_enabled, true);
    }

    public boolean fieldMissedPhoneCalls() {
        return getBoolean(R.string.p_field_missed_calls, true) &&
                notificationsEnabled() &&
                permissionChecker.canAccessMissedCallPermissions();
    }

    public boolean hasPurchase(int keyResource) {
        return getBoolean(keyResource, false);
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

    public int getLastSetVersion() {
        return getInt(P_CURRENT_VERSION, 0);
    }

    public void setCurrentVersion(int version) {
        setInt(P_CURRENT_VERSION, version);
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

    public boolean useNotificationActions() {
        return atLeastJellybean() && getBoolean(R.string.p_rmd_notif_actions_enabled, true);
    }

    public File getAttachmentsDirectory() {
        File directory = null;
        String customDir = getStringValue(R.string.p_attachment_dir);
        if (permissionChecker.canWriteToExternalStorage() && !TextUtils.isEmpty(customDir)) {
            directory = new File(customDir);
        }

        if (directory == null || !directory.exists()) {
            directory = getDefaultFileLocation(TaskAttachment.FILES_DIRECTORY_DEFAULT);
        }

        return directory;
    }

    private File getDefaultFileLocation(String type) {
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return null;
        }
        String path = String.format("%s/%s",
                externalFilesDir.getAbsolutePath(),
                type);
        File file = new File(path);
        return file.isDirectory() || file.mkdirs() ? file : null;
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
        if (permissionChecker.canWriteToExternalStorage() && !TextUtils.isEmpty(customDir)) {
            directory = new File(customDir);
        }

        if (directory == null || !directory.exists()) {
            directory = getDefaultFileLocation("backups");
        }

        return directory;
    }

    public long[] getVibrationPattern() {
        int vibrationCount = getIntegerFromString(R.string.p_vibrate_count, 3);
        long vibrationDuration = getIntegerFromString(R.string.p_vibrate_duration, 1000);
        long vibrationPause = getIntegerFromString(R.string.p_vibrate_pause, 500);
        List<Long> pattern = new ArrayList<>(vibrationCount);
        pattern.add(0L);
        pattern.add(vibrationDuration);
        for (int i = 1 ; i < vibrationCount ; i++) {
            pattern.add(vibrationPause);
            pattern.add(vibrationDuration);
        }
        return Longs.toArray(pattern);
    }

    public void remove(int resId) {
        Editor editor = prefs.edit();
        editor.remove(context.getString(resId));
        editor.apply();
    }
}
