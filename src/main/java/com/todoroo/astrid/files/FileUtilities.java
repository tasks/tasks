/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.content.Context;
import android.text.TextUtils;

import com.todoroo.astrid.data.TaskAttachment;

import org.joda.time.DateTime;
import org.tasks.files.FileHelper;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class FileUtilities {

    public static String getNewAudioAttachmentPath(Preferences preferences, Context context, AtomicReference<String> nameReference) {
        return getNewAttachmentPath(preferences, context, ".m4a", nameReference); //$NON-NLS-1$
    }

    public static String getNewAttachmentPath(Preferences preferences, Context context, String extension, AtomicReference<String> nameReference) {

        String dir = getAttachmentsDirectory(preferences, context).getAbsolutePath();

        String name = getNonCollidingFileName(dir, new DateTime().toString("yyyyMMddHHmm"), extension);

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
            directory = FileHelper.getExternalFilesDir(context, TaskAttachment.FILES_DIRECTORY_DEFAULT);
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
