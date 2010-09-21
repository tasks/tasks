/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            try {
                JSONArray newLists = new JSONArray();
                JSONObject list = new JSONObject();
                list.put("id", "1");
                list.put("title", "Tim's Tasks");
                newLists.put(list);
                list = new JSONObject();
                list.put("id", "2");
                list.put("title", "Travel");
                newLists.put(list);
                gtasksListService.updateLists(newLists);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
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
