package com.todoroo.astrid.actfm.sync;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.sync.SyncProviderUtilities;

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

    // --- user management

    /**
     * @return get user id
     */
    public static long userId() {
        return Preferences.getLong(PREF_USER_ID, -1L);
    }

    /** Act.fm current user id */
    public static final String PREF_USER_ID = IDENTIFIER + "_user"; //$NON-NLS-1$

    /** Act.fm current user name */
    public static final String PREF_NAME = IDENTIFIER + "_name"; //$NON-NLS-1$

    /** Act.fm current user picture */
    public static final String PREF_PICTURE = IDENTIFIER + "_picture"; //$NON-NLS-1$

    /** Act.fm current user email */
    public static final String PREF_EMAIL = IDENTIFIER + "_email"; //$NON-NLS-1$

    /** Act.fm last sync server time */
    public static final String PREF_SERVER_TIME = IDENTIFIER + "_time"; //$NON-NLS-1$

    private static JSONObject user = null;

    /**
     * Return JSON object user, either yourself or the user of the model
     * @param update
     * @return
     */
    public static JSONObject userFromModel(RemoteModel model) {
        if(model.getValue(RemoteModel.USER_ID_PROPERTY) == 0)
            return thisUser();
        else {
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

    @SuppressWarnings("nls")
    private synchronized static JSONObject thisUser() {
        if(user == null) {
            user = new JSONObject();
            try {
                user.put("name", Preferences.getStringValue(PREF_NAME));
                user.put("email", Preferences.getStringValue(PREF_EMAIL));
                user.put("picture", Preferences.getStringValue(PREF_PICTURE));
                user.put("id", Preferences.getLong(PREF_USER_ID, 0));
                System.err.println(user);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return user;
    }

    @Override
    public String getLoggedInUserName() {
        return Preferences.getStringValue(PREF_NAME);
    }

}