/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import android.text.TextUtils;
import android.util.Log;

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
