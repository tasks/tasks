package com.timsu.astrid.sync;

import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mdt.rtm.ApplicationInfo;
import com.mdt.rtm.ServiceImpl;
import com.mdt.rtm.data.RtmList;
import com.mdt.rtm.data.RtmLists;
import com.mdt.rtm.data.RtmAuth.Perms;
import com.timsu.astrid.utilities.Constants;

public class Synchronizer {

    private static ServiceImpl rtmService = null;

    public static void authenticate(Activity activity) {
        try {
            String apiKey = "127d19adab1a7b6922d8dfda3ef09645";
            String sharedSecret = "503816890a685753";
            String appName = null;
            String authToken = null;

            // check if our auth token works
            if(authToken != null) {
                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName, authToken));
                if(!rtmService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName));
                String url = rtmService.beginAuthorization(Perms.delete);

                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(url));
                activity.startActivityForResult(browserIntent,
                        Constants.RESULT_SYNCHRONIZE);
            } else {
                performSync(activity, false);
            }

        } catch (Exception e) {
            Log.e("astrid", "error parsing", e); // TODO dialog box
        }
    }

    public static void performSync(Activity activity, boolean justLoggedIn) {
        try {
            // store token
            if(justLoggedIn) {
                String token = rtmService.completeAuthorization();
                Log.w("astrid", "LOOK A TOKEN " + token);
            }

            System.out.println("isAuthorized: " + rtmService.isServiceAuthorized());
            RtmLists lists = rtmService.lists_getList();
            for(Entry<String, RtmList> list : lists.getLists().entrySet()) {
                Log.i("astrid", "look, " + list.getKey());
            }
        } catch (Exception e) {
            Log.e("astrid", "error parsing", e); // TODO dialog box
        }
    }
}
