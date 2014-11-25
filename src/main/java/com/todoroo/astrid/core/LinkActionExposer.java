/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;

import org.tasks.R;

import java.util.HashMap;
import java.util.List;

/**
 * Exposes {@link TaskAction} for phone numbers, emails, urls, etc
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class LinkActionExposer {

    public static TaskAction getActionsForTask(Context context, Task task, boolean hasAttachments, boolean hasNotes) {
        if (task == null) {
            return null;
        }

        Spannable titleSpan = Spannable.Factory.getInstance().newSpannable(task.getTitle());
        Linkify.addLinks(titleSpan, Linkify.ALL);

        URLSpan[] urlSpans = titleSpan.getSpans(0, titleSpan.length(), URLSpan.class);
        if(urlSpans.length == 0 && !hasNotes &&
                !hasAttachments) {
            return null;
        }

        PackageManager pm = context.getPackageManager();

        for(URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();
            int start = titleSpan.getSpanStart(urlSpan);
            int end = titleSpan.getSpanEnd(urlSpan);
            String text = titleSpan.subSequence(start, end).toString();
            TaskAction taskAction = createLinkAction(context, task.getId(), url, text, pm);
            if (taskAction != null) {
                return taskAction;
            }
        }

        Resources r = context.getResources();
        if (hasAttachments) {
            BitmapDrawable icon = getBitmapDrawable(R.drawable.action_attachments, r);
            return new FilesAction(icon);
        }

        if (hasNotes) {
            BitmapDrawable icon = getBitmapDrawable(R.drawable.action_notes, r);
            return new NotesAction(icon);
        }

        return null;
    }

    private static TaskAction createLinkAction(Context context, long id, String url, String text, PackageManager pm) {
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
        else {
            return null;
        }

        Resources r = context.getResources();
        Drawable icon;
        if (url.startsWith("mailto")) {
            icon = getBitmapDrawable(R.drawable.action_mail, r);
        } else if (url.startsWith("tel")) {
            icon = getBitmapDrawable(R.drawable.action_tel, r);
        } else {
            icon = getBitmapDrawable(R.drawable.action_web, r);
        }

        return new TaskAction(PendingIntent.getActivity(context, (int)id, actionIntent, 0), (BitmapDrawable)icon);
    }

    private static final HashMap<Integer, BitmapDrawable> IMAGE_CACHE = new HashMap<>();

    private static BitmapDrawable getBitmapDrawable(int resId, Resources resources) {
        if (IMAGE_CACHE.containsKey(resId)) {
            return IMAGE_CACHE.get(resId);
        } else {
            BitmapDrawable b = (BitmapDrawable) resources.getDrawable(resId);
            IMAGE_CACHE.put(resId, b);
            return b;
        }
    }

}
