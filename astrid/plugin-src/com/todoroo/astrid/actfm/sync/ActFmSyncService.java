/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.GCMIntentService;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Service for synchronizing data on Astrid.com server with local.
 *
 * @author Tim Su <tim@todoroo.com>
 */

public final class ActFmSyncService {

    // --- instance variables

    @Autowired
    private TagDataService tagDataService;
    @Autowired
    private ActFmPreferenceService actFmPreferenceService;
    @Autowired
    private ActFmInvoker actFmInvoker;

    private String token;

    public ActFmSyncService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- data fetch methods
    public void setGCMRegistration(String regId) {
        try {
            String deviceId = GCMIntentService.getDeviceID();
            String existingC2DM = Preferences.getStringValue(GCMIntentService.PREF_C2DM_REGISTRATION);

            ArrayList<Object> params = new ArrayList<Object>();
            params.add("gcm");
            params.add(regId);
            if (!TextUtils.isEmpty(deviceId)) {
                params.add("device_id");
                params.add(deviceId);
            }
            if (!TextUtils.isEmpty(existingC2DM)) { // Unregisters C2DM with the server for migration purposes
                params.add("c2dm");
                params.add(existingC2DM);
            }

            invoke("user_set_gcm", params.toArray(new Object[params.size()]));

            Preferences.setString(GCMIntentService.PREF_REGISTRATION, regId);
            Preferences.setString(GCMIntentService.PREF_C2DM_REGISTRATION, null);
            Preferences.setString(GCMIntentService.PREF_NEEDS_REGISTRATION, null);
            Preferences.setBoolean(GCMIntentService.PREF_NEEDS_RETRY, false);
        } catch (IOException e) {
            Preferences.setString(GCMIntentService.PREF_NEEDS_REGISTRATION, regId);
            Log.e("gcm", "error-gcm-register", e);
        }
    }

    // --- generic invokation

    /**
     * invoke authenticated method against the server
     */
    public JSONObject invoke(String method, Object... getParameters) throws IOException {
        if (!checkForToken()) {
            throw new ActFmServiceException("not logged in", null);
        }
        Object[] parameters = new Object[getParameters.length + 2];
        parameters[0] = "token";
        parameters[1] = token;
        for (int i = 0; i < getParameters.length; i++) {
            parameters[i + 2] = getParameters[i];
        }
        return actFmInvoker.invoke(method, parameters);
    }

    protected void handleException(String message, Exception exception) {
        Log.w("actfm-sync", message, exception);
    }

    private boolean checkForToken() {
        if (!actFmPreferenceService.isLoggedIn()) {
            return false;
        }
        token = actFmPreferenceService.getToken();
        return true;
    }

    // --- json reader helper

    /**
     * Read data models from JSON
     */
    public static class JsonHelper {

        public static void jsonFromUser(JSONObject json, User model) throws JSONException {
            json.put("id", model.getValue(User.UUID));
            json.put("name", model.getDisplayName());
            json.put("email", model.getValue(User.EMAIL));
            json.put("picture", model.getPictureUrl(User.PICTURE, RemoteModel.PICTURE_THUMB));
            json.put("first_name", model.getValue(User.FIRST_NAME));
        }
    }

}
