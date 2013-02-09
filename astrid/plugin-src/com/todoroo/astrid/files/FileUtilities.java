/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import android.content.Context;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.TaskAttachment;

public class FileUtilities {

    /**
     * @return Date string for use with file attachment names
     */
    public static String getDateStringForFilename(Context context, Date date) {
        return DateUtilities.getDateStringHideYear(context, date) + ", " + getTimeStringForFilename(context, date); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
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

    public static String getNewImageAttachmentPath(Context context, AtomicReference<String> nameReference) {
        return getNewAttachmentPath(context, R.string.file_prefix_image, ".png", nameReference); //$NON-NLS-1$
    }

    public static String getNewAudioAttachmentPath(Context context, AtomicReference<String> nameReference) {
        return getNewAttachmentPath(context, R.string.file_prefix_voice, ".m4a", nameReference); //$NON-NLS-1$
    }

    private static String getNewAttachmentPath(Context context, int prefixId, String extension, AtomicReference<String> nameReference) {
        StringBuilder fileNameBuilder = new StringBuilder();
        fileNameBuilder.append(context.getString(prefixId))
                .append(" ") //$NON-NLS-1$
                .append(getDateStringForFilename(context, new Date()));

        String dir = getAttachmentsDirectory(context).getAbsolutePath();

        String name = getNonCollidingFileName(dir, fileNameBuilder.toString(), extension);

        StringBuilder filePathBuilder = new StringBuilder();
        filePathBuilder.append(dir)
                .append(File.separator)
                .append(name);
        if (nameReference != null)
            nameReference.set(name);

        return filePathBuilder.toString();
    }

    public static File getAttachmentsDirectory(Context context) {
        File directory = null;
        String customDir = Preferences.getStringValue(TaskAttachment.FILES_DIRECTORY_PREF);
        if (!TextUtils.isEmpty(customDir))
            directory = new File(customDir);

        if (directory == null || !directory.exists())
            directory = context.getExternalFilesDir(TaskAttachment.FILES_DIRECTORY_DEFAULT);

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
