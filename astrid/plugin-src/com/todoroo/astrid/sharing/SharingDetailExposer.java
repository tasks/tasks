/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sharing;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;

/**
 * Exposes Task Detail for notes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SharingDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        boolean extended = intent.getBooleanExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        String taskDetail = getTaskDetails(taskId, extended);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, extended);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(long id, boolean extended) {
        Metadata metadata = PluginServices.getMetadataByTaskAndWithKey(id, SharingFields.METADATA_KEY);
        if(metadata == null || extended)
            return null;

        if(metadata.getValue(SharingFields.PRIVACY) == SharingFields.PRIVACY_PUBLIC)
                return "<img src='silk_world'/> Public"; //$NON-NLS-1$

        return null;
    }

}
