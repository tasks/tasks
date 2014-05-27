/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.injection.InjectingBroadcastReceiver;

import javax.inject.Inject;

/**
 * Exposes Task Detail for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDetailExposer extends InjectingBroadcastReceiver {

    @Inject TagService tagService;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1) {
            return;
        }

        String taskDetail = getTaskDetails(taskId);
        if(taskDetail == null) {
            return;
        }

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private String getTaskDetails(long id) {
        String tagList = tagService.getTagsAsString(id);
        if(tagList.length() == 0) {
            return null;
        }

        return /*"<img src='silk_tag_pink'/> " +*/ tagList;
    }
}
