package com.timsu.astrid.sync;

import java.util.Map.Entry;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.mdt.rtm.ApplicationInfo;
import com.mdt.rtm.ServiceImpl;
import com.mdt.rtm.data.RtmList;
import com.mdt.rtm.data.RtmLists;
import com.mdt.rtm.data.RtmAuth.Perms;
import com.timsu.astrid.R;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;

public class RTMSyncService implements SynchronizationService {

    private ServiceImpl rtmService = null;
    private int id;

    public RTMSyncService(int id) {
        this.id = id;
    }

    @Override
    public void synchronize(Activity activity) {
        authenticate(activity);
    }

    @Override
    public void synchronizationDisabled(Activity activity) {
        Preferences.setSyncRTMToken(activity, null);
    }

    /** Perform authentication with RTM. Will open the SyncBrowser if necessary */
    private void authenticate(final Activity activity) {
        try {
            String apiKey = "bd9883b3384a21ead17501da38bb1e68";
            String sharedSecret = "a19b2a020345219b";
            String appName = null;
            String authToken = Preferences.getSyncRTMToken(activity);

            // check if our auth token works
            if(authToken != null) {
                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName, authToken));
                if(!rtmService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                // try completing the authorization.
                if(rtmService != null) {
                    try {
                        String token = rtmService.completeAuthorization();
                        Log.w("astrid", "got RTM token: " + token);
                        Preferences.setSyncRTMToken(activity, token);
                        performSync(activity);

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName));
                final String url = rtmService.beginAuthorization(Perms.delete);

                Resources r = activity.getResources();
                DialogUtilities.okCancelDialog(activity,
                        r.getString(R.string.sync_auth_request, "RTM"),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(url));
                        activity.startActivity(intent);
                    }
                }, null);
            } else {
                performSync(activity);
            }

        } catch (Exception e) {
            Synchronizer.showError(activity, e);
        }
    }

    private void performSync(Activity activity) {
        try {
            Log.i("astrid", "isAuthorized: " + rtmService.isServiceAuthorized());
            RtmLists lists = rtmService.lists_getList();
            for(Entry<String, RtmList> list : lists.getLists().entrySet()) {
                Log.i("astrid", "look, " + list.getKey());
            }

            // fetch tasks that've changed since last sync

            // grab my own list of tasks that have changed since last sync


            // if we find a conflict... remember and ignore


            // update tasks that have changed


        } catch (Exception e) {
            Synchronizer.showError(activity, e);
        }
    }

}
