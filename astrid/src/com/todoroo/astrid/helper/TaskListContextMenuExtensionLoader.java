/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.todoroo.astrid.api.AstridApiConstants;

public class TaskListContextMenuExtensionLoader {

    public static class ContextMenuItem {
        public CharSequence title;
        public Intent intent;
    }

    private ContextMenuItem[] contextMenuItemCache = new ContextMenuItem[0];

    public void loadContextMenuIntents(Context context) {
        Intent queryIntent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryBroadcastReceivers(queryIntent,
                PackageManager.GET_META_DATA);

        int length = resolveInfoList.size();
        contextMenuItemCache = new ContextMenuItem[length];
        for(int i = 0; i < length; i++) {
            ResolveInfo resolveInfo = resolveInfoList.get(i);

            ContextMenuItem item = new ContextMenuItem();

            item.intent = new Intent(AstridApiConstants.ACTION_TASK_CONTEXT_MENU);
            item.intent.setClassName(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name);
            item.title = resolveInfo.loadLabel(pm);
            contextMenuItemCache[i] = item;
        }
    }

    public void loadInNewThread(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                loadContextMenuIntents(context);
            }
        }).start();
    }

    public ContextMenuItem[] getList() {
        return contextMenuItemCache;
    }


}
