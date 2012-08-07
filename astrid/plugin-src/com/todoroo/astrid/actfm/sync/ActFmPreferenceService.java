/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.AstridPreferences;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
public class ActFmPreferenceService extends SyncProviderUtilities {

    /** add-on identifier */
    public static final String IDENTIFIER = "actfm"; //$NON-NLS-1$

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public int getSyncIntervalKey() {
        return R.string.actfm_APr_interval_key;
    }

    @Override
    public void clearLastSyncDate() {
        super.clearLastSyncDate();
        Preferences.setInt(ActFmPreferenceService.PREF_SERVER_TIME, 0);
    }

    @Override
    public boolean shouldShowToast() {
        if(Preferences.getBoolean(AstridPreferences.P_FIRST_TASK, true))
            return false;
        return super.shouldShowToast();
    }

    // --- user management

    /**
     * @return get user id
     */
    public static long userId() {
        return Preferences.getLong(PREF_USER_ID, -2L);
    }

    /** Act.fm current user id */
    public static final String PREF_USER_ID = IDENTIFIER + "_user"; //$NON-NLS-1$

    /** Act.fm current user name */
    public static final String PREF_NAME = IDENTIFIER + "_name"; //$NON-NLS-1$

    /** Act.fm current user first name */
    public static final String PREF_FIRST_NAME = IDENTIFIER + "_first_name"; //$NON-NLS-1$

    /** Act.fm current user last name */
    public static final String PREF_LAST_NAME = IDENTIFIER + "_last_name"; //$NON-NLS-1$

    /** Act.fm current user premium status */
    public static final String PREF_PREMIUM = IDENTIFIER + "_premium"; //$NON-NLS-1$

    /** Act.fm current user picture */
    public static final String PREF_PICTURE = IDENTIFIER + "_picture"; //$NON-NLS-1$

    /** Act.fm current user email */
    public static final String PREF_EMAIL = IDENTIFIER + "_email"; //$NON-NLS-1$

    /** Act.fm last sync server time */
    public static final String PREF_SERVER_TIME = IDENTIFIER + "_time"; //$NON-NLS-1$

    private static JSONObject user = null;

    @Override
    protected void reportLastErrorImpl(String lastError, String type) {
        StatisticsService.reportEvent(StatisticsConstants.ACTFM_SYNC_ERROR, "type", type); //$NON-NLS-1$
    }

    /**
     * Return JSON object user, either yourself or the user of the model
     * @param update
     * @return
     */
    public static JSONObject userFromModel(RemoteModel model) {
        if(model.getValue(RemoteModel.USER_ID_PROPERTY) == 0) {
            return thisUser();
        }else {
            try {
                return new JSONObject(model.getValue(RemoteModel.USER_JSON_PROPERTY));
            } catch (JSONException e) {
                return new JSONObject();
            }
        }
    }

    @SuppressWarnings("nls")
    public static String updateToString(Update update) {
        JSONObject updateUser = ActFmPreferenceService.userFromModel(update);
        String description = update.getValue(Update.ACTION);
        String message = update.getValue(Update.MESSAGE);
        if(update.getValue(Update.ACTION_CODE).equals("task_comment") ||
                update.getValue(Update.ACTION_CODE).equals("tag_comment"))
            description = message;
        else if(!TextUtils.isEmpty(message))
            description += " " + message;
        return String.format("%s: %s", updateUser.optString("name"), description);
    }


    public synchronized static JSONObject thisUser() {
        if(user == null) {
            user = new JSONObject();
            populateUser();
        }
        return user;
    }

    public synchronized static void reloadThisUser() {
        if (user == null)
            return;
        populateUser();
    }

    @SuppressWarnings("nls")
    private static void populateUser() {
        try {
            user.put("name", Preferences.getStringValue(PREF_NAME));
            user.put("first_name", Preferences.getStringValue(PREF_FIRST_NAME));
            user.put("last_name", Preferences.getStringValue(PREF_LAST_NAME));
            user.put("premium", Preferences.getBoolean(PREF_PREMIUM, false));
            user.put("email", Preferences.getStringValue(PREF_EMAIL));
            user.put("picture", Preferences.getStringValue(PREF_PICTURE));
            user.put("id", Preferences.getLong(PREF_USER_ID, 0));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPremiumUser() {
        return Preferences.getBoolean(PREF_PREMIUM, false);
    }

    @Override
    public String getLoggedInUserName() {
        return Preferences.getStringValue(PREF_NAME);
    }

}
