/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.AndroidRuntimeException;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.files.FilesAction;
import com.todoroo.astrid.notes.NotesAction;
import java.util.List;
import org.tasks.R;
import timber.log.Timber;

/**
 * Exposes {@link TaskAction} for phone numbers, emails, urls, etc
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class LinkActionExposer {

  public static TaskAction getActionsForTask(Context context, Task task, boolean hasAttachments) {
    if (task == null) {
      return null;
    }

    boolean hasNotes = task.hasNotes();

    Spannable titleSpan = Spannable.Factory.getInstance().newSpannable(task.getTitle());
    try {
      Linkify.addLinks(titleSpan, Linkify.ALL);
    } catch (AndroidRuntimeException e) {
      // This can happen if WebView is missing
      Timber.w(e);
      return null;
    }

    URLSpan[] urlSpans = titleSpan.getSpans(0, titleSpan.length(), URLSpan.class);
    if (urlSpans.length == 0 && !hasNotes && !hasAttachments) {
      return null;
    }

    PackageManager pm = context.getPackageManager();

    for (URLSpan urlSpan : urlSpans) {
      String url = urlSpan.getURL();
      int start = titleSpan.getSpanStart(urlSpan);
      int end = titleSpan.getSpanEnd(urlSpan);
      String text = titleSpan.subSequence(start, end).toString();
      TaskAction taskAction = createLinkAction(context, task.getId(), url, text, pm);
      if (taskAction != null) {
        return taskAction;
      }
    }

    if (hasAttachments) {
      return new FilesAction(R.drawable.ic_attachment_24dp);
    }

    if (hasNotes) {
      return new NotesAction(R.drawable.ic_event_note_24dp);
    }

    return null;
  }

  private static TaskAction createLinkAction(
      Context context, long id, String url, String text, PackageManager pm) {
    Intent itemIntent = new Intent(Intent.ACTION_VIEW);
    itemIntent.setData(Uri.parse(url));
    List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(itemIntent, 0);

    Intent actionIntent;

    // if options > 1, display open with...
    if (resolveInfoList.size() > 1) {
      actionIntent = Intent.createChooser(itemIntent, text);
    }

    // else show app that gets opened
    else if (resolveInfoList.size() == 1) {
      actionIntent = itemIntent;
    }

    // no intents -> no item
    else {
      return null;
    }

    int icon;
    if (url.startsWith("mailto")) {
      icon = R.drawable.ic_email_black_24dp;
    } else if (url.startsWith("tel")) {
      icon = R.drawable.ic_phone_white_24dp;
    } else {
      icon = R.drawable.ic_public_black_24dp;
    }

    return new TaskAction(PendingIntent.getActivity(context, (int) id, actionIntent, 0), icon);
  }
}
