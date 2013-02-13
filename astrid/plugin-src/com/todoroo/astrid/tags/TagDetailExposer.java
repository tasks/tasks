/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Exposes Task Detail for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(long id) {
        String tagList = TagService.getInstance().getTagsAsString(id);
        if(tagList.length() == 0)
            return null;

        return /*"<img src='silk_tag_pink'/> " +*/ tagList;
    }

}
