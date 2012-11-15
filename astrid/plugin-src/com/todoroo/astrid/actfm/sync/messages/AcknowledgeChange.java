package com.todoroo.astrid.actfm.sync.messages;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;

public class AcknowledgeChange extends ServerToClientMessage {

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    @Autowired
    private TagOutstandingDao tagOutstandingDao;

    public AcknowledgeChange(JSONObject json) {
        super(json);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    @SuppressWarnings("nls")
    public void processMessage() {
        JSONArray idsArray = json.optJSONArray("ids");
        String table = json.optString("table");
        if (idsArray != null && !TextUtils.isEmpty(table)) {
            OutstandingEntryDao<?> dao = null;
            if (NameMaps.SERVER_TABLE_TASKS.equals(table))
                dao = taskOutstandingDao;
            else if (NameMaps.SERVER_TABLE_TAGS.equals(table))
                dao = tagOutstandingDao;

            if (dao == null)
                return;

            ArrayList<Long> idsList = new ArrayList<Long>();
            for (int i = 0; i < idsArray.length(); i++) {
                try {
                    Long id = idsArray.getLong(i);
                    if (id <= 0)
                        continue;

                    idsList.add(id);
                } catch (JSONException e) {
                    //
                }
            }
            dao.deleteWhere(AbstractModel.ID_PROPERTY.in(idsList.toArray(new Long[idsList.size()])));
        }
    }

}
