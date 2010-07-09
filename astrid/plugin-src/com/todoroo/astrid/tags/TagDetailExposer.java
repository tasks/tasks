/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDetail;

/**
 * Exposes {@link TaskDetail} for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDetailExposer extends BroadcastReceiver {

    private static TagService tagService = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        if(tagService == null)
            tagService = new TagService();
        String tagList = tagService.getTagsAsString(taskId);

        if(tagList.length() == 0)
            return;

        TaskDetail taskDetail = new TaskDetail(TagsPlugin.IDENTIFIER,
                context.getString(R.string.tag_TLA_detail, tagList));

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
