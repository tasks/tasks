/**
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
import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for phone numbers, emails, urls, etc
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class LinkActionExposer {

    private PackageManager pm;

    public List<TaskAction> getActionsForTask(Context context, long taskId) {
        List<TaskAction> result = new ArrayList<TaskAction>();
        if(taskId == -1)
            return result;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.TITLE);
        if (task == null) return result;

        Spannable titleSpan = Spannable.Factory.getInstance().newSpannable(task.getValue(Task.TITLE));
        Linkify.addLinks(titleSpan, Linkify.ALL);

        URLSpan[] urlSpans = titleSpan.getSpans(0, titleSpan.length(), URLSpan.class);
        if(urlSpans.length == 0)
            return result;

        pm = context.getPackageManager();

        for(URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();
            int start = titleSpan.getSpanStart(urlSpan);
            int end = titleSpan.getSpanEnd(urlSpan);
            String text = titleSpan.subSequence(start, end).toString();
            TaskAction taskAction = createLinkAction(context, url, text);
            if (taskAction != null)
                result.add(taskAction);
        }
        return result;
    }

    @SuppressWarnings("nls")
    private TaskAction createLinkAction(Context context, String url, String text) {
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
                PendingIntent.getActivity(context, 0, actionIntent, 0), bitmap);
        return action;
    }

}
