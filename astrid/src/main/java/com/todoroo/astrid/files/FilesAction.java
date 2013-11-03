/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.PendingIntent;
import android.graphics.drawable.BitmapDrawable;

import com.todoroo.astrid.api.TaskAction;

public class FilesAction extends TaskAction {

    public FilesAction(PendingIntent intent, BitmapDrawable icon) {
        super(intent, icon);
    }
}
