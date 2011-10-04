/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class LinkActionExposer extends BroadcastReceiver {

    private PackageManager pm;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.TITLE);
        if (task == null) return;

        Spannable titleSpan = Spannable.Factory.getInstance().newSpannable(task.getValue(Task.TITLE));
        Linkify.addLinks(titleSpan, Linkify.ALL);

        URLSpan[] urlSpans = titleSpan.getSpans(0, titleSpan.length(), URLSpan.class);
        if(urlSpans.length == 0)
            return;

        pm = context.getPackageManager();

        for(URLSpan urlSpan : urlSpans) {
            String url = urlSpan.getURL();
            int start = titleSpan.getSpanStart(urlSpan);
            int end = titleSpan.getSpanEnd(urlSpan);
            String text = titleSpan.subSequence(start, end).toString();
            sendLinkAction(context, taskId, url, text);
        }
    }

    private void sendLinkAction(Context context, long taskId, String url, String text) {
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
            return;

        Drawable icon = resolveInfoList.get(0).loadIcon(pm);
        Bitmap bitmap = ((BitmapDrawable)icon).getBitmap();

        if(text.length() > 15)
            text = text.substring(0, 12) + "..."; //$NON-NLS-1$

        TaskAction action = new TaskAction(text,
                PendingIntent.getActivity(context, 0, actionIntent, 0), bitmap);

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, CorePlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
