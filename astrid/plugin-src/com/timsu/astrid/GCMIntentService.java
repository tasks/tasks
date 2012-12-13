package com.timsu.astrid;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;

@SuppressWarnings("nls")
public class GCMIntentService extends GCMBaseIntentService {

    public static final String SENDER_ID = "1003855277730"; //$NON-NLS-1$
    public static final String PREF_REGISTRATION = "gcm_id";
    public static final String PREF_NEEDS_REGISTRATION = "gcm_needs_reg";

    public static String getDeviceID() {
        String id = Secure.getString(ContextManager.getContext().getContentResolver(), Secure.ANDROID_ID);;
        if(AndroidUtilities.getSdkVersion() > 8) { //Gingerbread and above
            //the following uses relection to get android.os.Build.SERIAL to avoid having to build with Gingerbread
            try {
                if(!Build.UNKNOWN.equals(Build.SERIAL))
                    id = Build.SERIAL;
            } catch(Exception e) {
                // Ah well
            }
        }

        if (TextUtils.isEmpty(id) || "9774d56d682e549c".equals(id)) { // check for failure or devices affected by the "9774d56d682e549c" bug
            return null;
        }

        return id;
    }

    @Autowired
    private ActFmSyncService actFmSyncService;

    public GCMIntentService() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        actFmSyncService.setGCMRegistration(registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        // Server will unregister automatically next time it tries to send a message
    }


    @Override
    protected void onError(Context context, String intent) {
        // Unrecoverable
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        // Intent extras are the keys in the server message's "data" object
    }

    public static final void register(Context context) {
        try {
            if (AndroidUtilities.getSdkVersion() >= 8) {
                GCMRegistrar.checkDevice(context);
                GCMRegistrar.checkManifest(context);
                final String regId = GCMRegistrar.getRegistrationId(context);
                if ("".equals(regId)) {
                    GCMRegistrar.register(context, GCMIntentService.SENDER_ID);
                } else {
                    // TODO: Already registered--do something?
                }
            }
        } catch (Exception e) {
            // phone may not support gcm
            Log.e("actfm-sync", "gcm-register", e);
        }
    }

    public static final void unregister(Context context) {
        try {
            if (AndroidUtilities.getSdkVersion() >= 8) {
                GCMRegistrar.checkDevice(context);
                GCMRegistrar.unregister(context);
            }
        } catch (Exception e) {
            Log.e("actfm-sync", "gcm-unregister", e);
        }
    }

}
