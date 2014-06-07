/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.content.Context;
import android.text.TextUtils;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.TaskAttachment;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.tasks.date.DateTimeUtils.newDate;

public class FileUtilities {

    /**
     * @return Date string for use with file attachment names
     */
    public static String getDateStringForFilename(Context context, Date date) {
        return DateUtilities.getDateStringHideYear(date) + ", " + getTimeStringForFilename(context, date); //$NON-NLS-1$
    }

    private static String getTimeStringForFilename(Context context, Date date) {
        String value;
        if (DateUtilities.is24HourFormat(context)) {
            value = "HH.mm";
        }
        else {
            value = "hh.mma";
        }
        return new SimpleDateFormat(value).format(date);
    }

    public static String getNewImageAttachmentPath(Preferences preferences, Context context, AtomicReference<String> nameReference) {
        return getNewAttachmentPath(preferences, context, R.string.file_prefix_image, ".png", nameReference); //$NON-NLS-1$
    }

    public static String getNewAudioAttachmentPath(Preferences preferences, Context context, AtomicReference<String> nameReference) {
        return getNewAttachmentPath(preferences, context, R.string.file_prefix_voice, ".m4a", nameReference); //$NON-NLS-1$
    }

    private static String getNewAttachmentPath(Preferences preferences, Context context, int prefixId, String extension, AtomicReference<String> nameReference) {

        String dir = getAttachmentsDirectory(preferences, context).getAbsolutePath();

        String name = getNonCollidingFileName(dir, context.getString(prefixId) + " " + getDateStringForFilename(context, newDate()), extension);

        if (nameReference != null) {
            nameReference.set(name);
        }

        return dir + File.separator + name;
    }

    public static File getAttachmentsDirectory(Preferences preferences, Context context) {
        File directory = null;
        String customDir = preferences.getStringValue(TaskAttachment.FILES_DIRECTORY_PREF);
        if (!TextUtils.isEmpty(customDir)) {
            directory = new File(customDir);
        }

        if (directory == null || !directory.exists()) {
            directory = context.getExternalFilesDir(TaskAttachment.FILES_DIRECTORY_DEFAULT);
        }

        return directory;
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

}
