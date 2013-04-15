/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.GCMIntentService;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.billing.BillingConstants;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.abtesting.ABTestEventReportingService;
import com.todoroo.astrid.tags.reusable.FeaturedListFilterExposer;

/**
 * Service for synchronizing data on Astrid.com server with local.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class ActFmSyncService {

    // --- instance variables

    @Autowired
    private TagDataService tagDataService;
    @Autowired
    private ActFmPreferenceService actFmPreferenceService;
    @Autowired
    private ActFmInvoker actFmInvoker;
    @Autowired
    private ABTestEventReportingService abTestEventReportingService;

    private String token;

    public ActFmSyncService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    private void addAbTestEventInfo(List<Object> params) {
        JSONArray abTestInfo = abTestEventReportingService.getTestsWithVariantsArray();
        try {
            for (int i = 0; i < abTestInfo.length(); i++) {
                params.add("ab_variants[]"); params.add(abTestInfo.getString(i));
            }
        } catch (JSONException e) {
            Log.e("Error parsing AB test info", abTestInfo.toString(), e);
        }
    }

    // --- data fetch methods
    public int fetchFeaturedLists(int serverTime) throws JSONException, IOException {
        if (!checkForToken())
            return 0;
        JSONObject result = actFmInvoker.invoke("featured_lists",
                "token", token, "modified_after", serverTime);
        JSONArray featuredLists = result.getJSONArray("list");
        if (featuredLists.length() > 0)
            Preferences.setBoolean(FeaturedListFilterExposer.PREF_SHOULD_SHOW_FEATURED_LISTS, true);

        for (int i = 0; i < featuredLists.length(); i++) {
            JSONObject featObject = featuredLists.getJSONObject(i);
            tagDataService.saveFeaturedList(featObject);
        }

        return result.optInt("time", 0);
    }

    public void updateUserSubscriptionStatus(Runnable onSuccess, Runnable onRecoverableError, Runnable onInvalidToken) {
        String purchaseToken = Preferences.getStringValue(BillingConstants.PREF_PURCHASE_TOKEN);
        String productId = Preferences.getStringValue(BillingConstants.PREF_PRODUCT_ID);
        try {
            if (!checkForToken())
                throw new ActFmServiceException("Not logged in", null);

            ArrayList<Object> params = new ArrayList<Object>();
            params.add("purchase_token"); params.add(purchaseToken);
            params.add("product_id"); params.add(productId);
            addAbTestEventInfo(params);
            params.add("token"); params.add(token);

            actFmInvoker.invoke("premium_update_android", params.toArray(new Object[params.size()]));
            Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
            if (onSuccess != null)
                onSuccess.run();
        } catch (Exception e) {
            if (e instanceof ActFmServiceException) {
                ActFmServiceException ae = (ActFmServiceException)e;
                if (ae.result != null && ae.result.optString("status").equals("error")) {
                    if (ae.result.optString("code").equals("invalid_purchase_token")) { // Not a valid purchase--expired or duolicate
                        Preferences.setBoolean(ActFmPreferenceService.PREF_LOCAL_PREMIUM, false);
                        Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, false);
                        if (onInvalidToken != null)
                            onInvalidToken.run();
                        return;
                    }
                }
            }
            Preferences.setBoolean(BillingConstants.PREF_NEEDS_SERVER_UPDATE, true);
            if (onRecoverableError != null)
                onRecoverableError.run();
        }
    }

    public void setGCMRegistration(String regId) {
        try {
            String deviceId = GCMIntentService.getDeviceID();
            String existingC2DM = Preferences.getStringValue(GCMIntentService.PREF_C2DM_REGISTRATION);

            ArrayList<Object> params = new ArrayList<Object>();
            params.add("gcm"); params.add(regId);
            if (!TextUtils.isEmpty(deviceId)) {
                params.add("device_id"); params.add(deviceId);
            }
            if (!TextUtils.isEmpty(existingC2DM)) { // Unregisters C2DM with the server for migration purposes
                params.add("c2dm"); params.add(existingC2DM);
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

    /** invoke authenticated method against the server */
    public JSONObject invoke(String method, Object... getParameters) throws IOException,
    ActFmServiceException {
        if(!checkForToken())
            throw new ActFmServiceException("not logged in", null);
        Object[] parameters = new Object[getParameters.length + 2];
        parameters[0] = "token";
        parameters[1] = token;
        for(int i = 0; i < getParameters.length; i++)
            parameters[i+2] = getParameters[i];
        return actFmInvoker.invoke(method, parameters);
    }

    protected void handleException(String message, Exception exception) {
        Log.w("actfm-sync", message, exception);
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
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

            if (featuredList)
                model.setFlag(TagData.FLAGS, TagData.FLAG_FEATURED, true);

            if(json.has("picture"))
                model.setValue(TagData.PICTURE, json.optString("picture", ""));
            if(json.has("thumb"))
                model.setValue(TagData.THUMB, json.optString("thumb", ""));

            if(json.has("is_silent"))
                model.setFlag(TagData.FLAGS, TagData.FLAG_SILENT,json.getBoolean("is_silent"));

            if(!json.isNull("description"))
                model.setValue(TagData.TAG_DESCRIPTION, json.getString("description"));

            if(json.has("members")) {
                JSONArray members = json.getJSONArray("members");
                model.setValue(TagData.MEMBERS, members.toString());
                model.setValue(TagData.MEMBER_COUNT, members.length());
            }

            if (json.has("deleted_at"))
                model.setValue(TagData.DELETION_DATE, readDate(json, "deleted_at"));

            if(json.has("tasks"))
                model.setValue(TagData.TASK_COUNT, json.getInt("tasks"));
        }
    }

}
