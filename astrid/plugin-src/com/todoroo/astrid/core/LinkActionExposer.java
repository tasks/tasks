/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;

/**
 * Exposes {@link TaskDecoration} for phone numbers, emails, urls, etc
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class LinkActionExposer {

    private PackageManager pm;

    public List<TaskAction> getActionsForTask(Context context, Task task, boolean hasAttachments) {
        List<TaskAction> result = new ArrayList<TaskAction>();
        if (task == null) return result;

        String notes = task.getValue(Task.NOTES);
        Spannable titleSpan = Spannable.Factory.getInstance().newSpannable(task.getValue(Task.TITLE));
        Linkify.addLinks(titleSpan, Linkify.ALL);

        URLSpan[] urlSpans = titleSpan.getSpans(0, titleSpan.length(), URLSpan.class);
        if(urlSpans.length == 0 && TextUtils.isEmpty(notes) &&
                !hasAttachments)
            return result;

        pm = context.getPackageManager();

        for(URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();
            int start = titleSpan.getSpanStart(urlSpan);
            int end = titleSpan.getSpanEnd(urlSpan);
            String text = titleSpan.subSequence(start, end).toString();
            TaskAction taskAction = createLinkAction(context, task.getId(), url, text);
            if (taskAction != null)
                result.add(taskAction);
        }

        Resources r = context.getResources();
        if (hasAttachments) {
            Bitmap icon = ((BitmapDrawable) r.getDrawable(R.drawable.action_attachments)).getBitmap();
            FilesAction filesAction = new FilesAction("", null, icon); //$NON-NLS-1$
            result.add(filesAction);
        }

        if (!TextUtils.isEmpty(notes) && !Preferences.getBoolean(R.string.p_showNotes, false)) {
            Bitmap icon = ((BitmapDrawable) r.getDrawable(R.drawable.action_notes)).getBitmap();
            NotesAction notesAction = new NotesAction("", null, icon); //$NON-NLS-1$
            result.add(notesAction);
        }

        return result;
    }

    @SuppressWarnings("nls")
    private TaskAction createLinkAction(Context context, long id, String url, String text) {
        Intent itemIntent = new Intent(Intent.ACTION_VIEW);
        itemIntent.setData(Uri.parse(url));
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(itemIntent, 0);

        Intent actionIntent;

        // if options > 1, display open with...
        if(resolveInfoList.size() > 1) {
            actionIntent = Intent.createChooser(itemIntent, text);
        }

        // else show app that gets opened
        else if(resolveInfoList.size() == 1) {
            actionIntent = itemIntent;
        }

        // no intents -> no item
        else
            return null;

        Resources r = context.getResources();
        Drawable icon;
        if (url.startsWith("mailto")) {
            icon = r.getDrawable(R.drawable.action_mail);
        } else if (url.startsWith("tel")) {
            icon = r.getDrawable(R.drawable.action_tel);
        } else {
            icon = r.getDrawable(R.drawable.action_web);
        }
        Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();

        if(text.length() > 15)
            text = text.substring(0, 12) + "..."; //$NON-NLS-1$

        TaskAction action = new TaskAction(text,
                PendingIntent.getActivity(context, (int)id, actionIntent, 0), bitmap);
        return action;
    }

}
