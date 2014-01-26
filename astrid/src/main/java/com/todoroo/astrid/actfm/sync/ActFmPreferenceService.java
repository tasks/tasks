/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import com.todoroo.andlib.utility.Preferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
public class ActFmPreferenceService {

    /** add-on identifier */
    public static final String IDENTIFIER = "actfm"; //$NON-NLS-1$

    /**
     * @return get user id
     */
    public static String userId() {
        try {
            String value = Preferences.getStringValue(PREF_USER_ID);
            if (value == null) {
                return Long.toString(Preferences.getLong(PREF_USER_ID, -2L));
            }
            return value;
        } catch (Exception e) {
            return Long.toString(Preferences.getLong(PREF_USER_ID, -2L));
        }
    }

    /** Act.fm current user id */
    public static final String PREF_USER_ID = IDENTIFIER + "_user"; //$NON-NLS-1$

    /** Act.fm current user name */
    public static final String PREF_NAME = IDENTIFIER + "_name"; //$NON-NLS-1$

    /** Act.fm current user first name */
    public static final String PREF_FIRST_NAME = IDENTIFIER + "_first_name"; //$NON-NLS-1$

    /** Act.fm current user last name */
    public static final String PREF_LAST_NAME = IDENTIFIER + "_last_name"; //$NON-NLS-1$

    /** Act.fm current user picture */
    public static final String PREF_PICTURE = IDENTIFIER + "_picture"; //$NON-NLS-1$

    /** Act.fm current user email */
    public static final String PREF_EMAIL = IDENTIFIER + "_email"; //$NON-NLS-1$

    private static JSONObject user = null;

    public synchronized static JSONObject thisUser() {
        if(user == null) {
            user = new JSONObject();
            populateUser();
        }
        return user;
    }

    private static void populateUser() {
        try {
            user.put("name", Preferences.getStringValue(PREF_NAME));
            user.put("first_name", Preferences.getStringValue(PREF_FIRST_NAME));
            user.put("last_name", Preferences.getStringValue(PREF_LAST_NAME));
            user.put("premium", true);
            user.put("email", Preferences.getStringValue(PREF_EMAIL));
            user.put("picture", Preferences.getStringValue(PREF_PICTURE));
            user.put("id", ActFmPreferenceService.userId());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
