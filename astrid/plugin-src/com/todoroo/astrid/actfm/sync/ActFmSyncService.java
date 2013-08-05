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
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;

import org.json.JSONArray;
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
    public JSONObject invoke(String method, Object... getParameters) throws IOException,
            ActFmServiceException {
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

        protected static long readDate(JSONObject item, String key) {
            return item.optLong(key, 0) * 1000L;
        }

        public static void jsonFromUser(JSONObject json, User model) throws JSONException {
            json.put("id", model.getValue(User.UUID));
            json.put("name", model.getDisplayName());
            json.put("email", model.getValue(User.EMAIL));
            json.put("picture", model.getPictureUrl(User.PICTURE, RemoteModel.PICTURE_THUMB));
            json.put("first_name", model.getValue(User.FIRST_NAME));
        }

        public static void featuredListFromJson(JSONObject json, TagData model) throws JSONException {
            parseTagDataFromJson(json, model, true);
        }

        private static void parseTagDataFromJson(JSONObject json, TagData model, boolean featuredList) throws JSONException {
            model.clearValue(TagData.UUID);
            model.setValue(TagData.UUID, Long.toString(json.getLong("id")));
            model.setValue(TagData.NAME, json.getString("name"));

            if (featuredList) {
                model.setFlag(TagData.FLAGS, TagData.FLAG_FEATURED, true);
            }

            if (json.has("picture")) {
                model.setValue(TagData.PICTURE, json.optString("picture", ""));
            }
            if (json.has("thumb")) {
                model.setValue(TagData.THUMB, json.optString("thumb", ""));
            }

            if (json.has("is_silent")) {
                model.setFlag(TagData.FLAGS, TagData.FLAG_SILENT, json.getBoolean("is_silent"));
            }

            if (!json.isNull("description")) {
                model.setValue(TagData.TAG_DESCRIPTION, json.getString("description"));
            }

            if (json.has("members")) {
                JSONArray members = json.getJSONArray("members");
                model.setValue(TagData.MEMBERS, members.toString());
                model.setValue(TagData.MEMBER_COUNT, members.length());
            }

            if (json.has("deleted_at")) {
                model.setValue(TagData.DELETION_DATE, readDate(json, "deleted_at"));
            }

            if (json.has("tasks")) {
                model.setValue(TagData.TASK_COUNT, json.getInt("tasks"));
            }
        }
    }

}
