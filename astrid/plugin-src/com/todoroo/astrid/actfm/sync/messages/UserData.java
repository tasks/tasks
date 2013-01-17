package com.todoroo.astrid.actfm.sync.messages;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.User;

public class UserData extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-user-data"; //$NON-NLS-1$

    public UserData(JSONObject json) {
        super(json);
    }

    @Override
    public void processMessage() {
        UserDao userDao = PluginServices.getUserDao();
        try {
            String uuid = json.getString("uuid"); //$NON-NLS-1$
            User model = new User();
            JSONChangeToPropertyVisitor visitor = new JSONChangeToPropertyVisitor(model, json);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String column = keys.next();
                Property<?> property = NameMaps.serverColumnNameToLocalProperty(NameMaps.TABLE_ID_USERS, column);
                if (property != null) { // Unsupported property
                    property.accept(visitor, column);
                }
            }

            StringProperty uuidProperty = (StringProperty) NameMaps.serverColumnNameToLocalProperty(NameMaps.TABLE_ID_USERS, "uuid"); //$NON-NLS-1$
            if (!model.getSetValues().containsKey(uuidProperty.name))
                model.setValue(uuidProperty, uuid);

            if (model.getSetValues().size() > 0) {
                if (userDao.update(RemoteModel.UUID_PROPERTY.eq(uuid), model) <= 0) { // If update doesn't update rows. create a new model
                    model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                    userDao.createNew(model);
                }
            }
        } catch (JSONException e) {
            Log.e(ERROR_TAG, "Error parsing UserData JSON " + json, e); //$NON-NLS-1$
        }
    }

}
