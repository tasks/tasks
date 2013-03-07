package com.todoroo.astrid.actfm.sync.messages;

import org.json.JSONObject;

import android.text.TextUtils;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TagMetadataDao.TagMetadataCriteria;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagMemberMetadata;

public class UserData extends ServerToClientMessage {

    public UserData(JSONObject json) {
        super(json);
    }

    @Override
    @SuppressWarnings("nls")
    public void processMessage(String serverTime) {
        String uuid = json.optString("uuid");
        String email = json.optString("email");

        if (TextUtils.isEmpty(uuid))
            return;

        Task taskTemplate = new Task();
        taskTemplate.setValue(Task.USER_ID, uuid);
        taskTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        PluginServices.getTaskDao().update(Task.USER_ID.eq(email), taskTemplate);

        TagMetadata metadataTemplate = new TagMetadata();
        metadataTemplate.setValue(TagMemberMetadata.USER_UUID, uuid);
        metadataTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        PluginServices.getTagMetadataDao().update(Criterion.and(TagMetadataCriteria.withKey(TagMemberMetadata.KEY),
                TagMemberMetadata.USER_UUID.eq(email)), metadataTemplate);

    }

}
