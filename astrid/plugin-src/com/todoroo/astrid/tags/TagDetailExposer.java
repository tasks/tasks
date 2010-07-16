/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.DetailExposer;
import com.todoroo.astrid.api.TaskDetail;

/**
 * Exposes {@link TaskDetail} for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDetailExposer extends BroadcastReceiver implements DetailExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        TaskDetail taskDetail = getTaskDetails(context, taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    public TaskDetail getTaskDetails(Context context, long id) {
        String tagList = TagService.getInstance().getTagsAsString(id);

        if(tagList.length() == 0)
            return null;

        return new TaskDetail(context.getString(R.string.tag_TLA_detail, tagList));
    }

}
