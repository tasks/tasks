package com.todoroo.astrid.actfm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.TagData;

public class ShowTagDataExposer extends BroadcastReceiver {

    private static final String FILTER_ACTION = "com.todoroo.astrid.SHOW_PROJECT"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        TagData tagData = PluginServices.getTagDataService().getTagData(taskId,
                TagData.ID, TagData.TITLE);
        if(tagData == null)
            return;

        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            final String label = tagData.getValue(TagData.TITLE);
            final Drawable drawable = context.getResources().getDrawable(R.drawable.tango_users);
            Intent newIntent = new Intent(FILTER_ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            Bitmap icon = ((BitmapDrawable)drawable).getBitmap();
            TaskAction action = new TaskAction(label,
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), icon);

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ActFmPreferenceService.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if(FILTER_ACTION.equals(intent.getAction())) {
            Intent launchIntent = new Intent(context, TagDataViewActivity.class);
            launchIntent.putExtra(TagDataViewActivity.EXTRA_PROJECT_ID, tagData.getId());
            ContextManager.getContext().startActivity(launchIntent);
        }
    }

}
