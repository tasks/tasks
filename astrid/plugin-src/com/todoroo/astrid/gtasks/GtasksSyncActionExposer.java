/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.SyncAction;
import com.todoroo.gtasks.GoogleTaskListInfo;

/**
 * Exposes sync action
 *
 */
public class GtasksSyncActionExposer extends BroadcastReceiver {

    @Autowired private GtasksPreferenceService gtasksPreferenceService;

    @Autowired private GtasksListService gtasksListService;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);

        if(intent.getBooleanExtra("setup", false)) {
            gtasksPreferenceService.setToken("haha");
            GoogleTaskListInfo[] newLists = new GoogleTaskListInfo[2];
            GoogleTaskListInfo list = new GoogleTaskListInfo("1", "Tim's Tasks");
            newLists[0] = list;
            list = new GoogleTaskListInfo("2", "Travel");
            newLists[1] = list;
            gtasksListService.updateLists(newLists);
            System.err.println("you've ben set up the bomb.");
            return;
        }


        // if we aren't logged in, don't expose sync action
        //if(!gtasksPreferenceService.isLoggedIn())
        //    return;

        Intent syncIntent = new Intent(intent.getAction(), null,
                context, GtasksSyncActionExposer.class);
        syncIntent.putExtra("setup", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, syncIntent, 0);
        SyncAction syncAction = new SyncAction(context.getString(R.string.gtasks_GPr_header),
                pendingIntent);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, syncAction);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
