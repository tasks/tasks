package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.data.RemoteModel;

public class UserMigrated extends ServerToClientMessage {

    public UserMigrated(JSONObject json) {
        super(json);
    }

    @Override
    public void processMessage(String serverTime) {
        String oldUuid = json.optString("prev_user_id"); //$NON-NLS-1$
        String newUuid = json.optString("new_user_id"); //$NON-NLS-1$
        if (RemoteModel.isValidUuid(newUuid)) {
            Preferences.setString(ActFmPreferenceService.PREF_USER_ID, newUuid);
            new ConvertSelfUserIdsToZero().execute();
        }

        if (RemoteModel.isValidUuid(oldUuid)) {
            new ConvertSelfUserIdsToZero().execute(oldUuid);
        }
    }

}
