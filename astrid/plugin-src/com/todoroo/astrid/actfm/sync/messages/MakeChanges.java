package com.todoroo.astrid.actfm.sync.messages;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.tags.TagService;

@SuppressWarnings("nls")
public class MakeChanges<TYPE extends RemoteModel> extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-make-changes";

    private final RemoteModelDao<TYPE> dao;
    private final String table;
    private final long pushedAt;

    public MakeChanges(JSONObject json, RemoteModelDao<TYPE> dao, long pushedAt) {
        super(json);
        this.table = json.optString("table");
        this.dao = dao;
        this.pushedAt = pushedAt;
    }

    @Override
    public void processMessage() {
        JSONObject changes = json.optJSONObject("changes");
        String uuid = json.optString("uuid");
        if (changes != null && !TextUtils.isEmpty(uuid)) {
            if (dao != null) {
                try {
                    TYPE model = dao.getModelClass().newInstance();
                    JSONChangeToPropertyVisitor visitor = new JSONChangeToPropertyVisitor(model, changes);
                    Iterator<String> keys = changes.keys();
                    while (keys.hasNext()) {
                        String column = keys.next();
                        Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, column);
                        if (property != null) { // Unsupported property
                            property.accept(visitor, column);
                        }
                    }

                    StringProperty uuidProperty = (StringProperty) NameMaps.serverColumnNameToLocalProperty(table, "uuid");
                    if (model.getSetValues().containsKey(uuidProperty.name))
                        uuid = model.getValue(uuidProperty);

                    beforeSaveChanges(changes, model, uuid);
                    LongProperty pushedAtProperty = (LongProperty) NameMaps.serverColumnNameToLocalProperty(table, "pushed_at");
                    if (pushedAtProperty != null && pushedAt > 0)
                        model.setValue(pushedAtProperty, pushedAt);

                    if (!model.getSetValues().containsKey(uuidProperty.name))
                        model.setValue(uuidProperty, uuid);

                    if (model.getSetValues().size() > 0) {
                        model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                        boolean success = true;
                        if (dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), model) <= 0) { // If update doesn't update rows. create a new model
                            model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                            success = dao.createNew(model);
                        }
                        if (success)
                            afterSaveChanges(changes, model, uuid);
                    }

                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                }
            } else if (NameMaps.TABLE_ID_PUSHED_AT.equals(table)) {
                long tablePushedAt = 0;
                try {
                    tablePushedAt = DateUtilities.parseIso8601(changes.optString(NameMaps.TABLE_ID_PUSHED_AT));
                } catch (ParseException e) {
                    //
                }
                if (tablePushedAt > 0) {
                    String pushedAtKey = null;
                    if (NameMaps.TABLE_ID_TASKS.equals(uuid))
                        pushedAtKey = NameMaps.PUSHED_AT_TASKS;
                    else if (NameMaps.TABLE_ID_TAGS.equals(uuid))
                        pushedAtKey = NameMaps.PUSHED_AT_TAGS;

                    if (pushedAtKey != null)
                        Preferences.setLong(pushedAtKey, tablePushedAt);

                }
            }
        }
    }

    private void beforeSaveChanges(JSONObject changes, TYPE model, String uuid) {
        ChangeHooks beforeSaveChanges = null;
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            beforeSaveChanges = new BeforeSaveTaskChanges(model, changes, uuid);

        if (beforeSaveChanges != null)
            beforeSaveChanges.performChanges();
    }

    private void afterSaveChanges(JSONObject changes, TYPE model, String uuid) {
        ChangeHooks afterSaveChanges = null;
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            afterSaveChanges = new AfterSaveTaskChanges(model, changes, uuid);

        if (afterSaveChanges != null)
            afterSaveChanges.performChanges();
    }

    private abstract class ChangeHooks {
        protected final TYPE model;
        protected final JSONObject changes;
        protected final String uuid;

        public ChangeHooks(TYPE model, JSONObject changes, String uuid) {
            this.model = model;
            this.changes = changes;
            this.uuid = uuid;
        }

        public abstract void performChanges();
    }

    private class BeforeSaveTaskChanges extends ChangeHooks {

        public BeforeSaveTaskChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @Override
        public void performChanges() {
            //
        }
    }

    private class AfterSaveTaskChanges extends ChangeHooks {

        public AfterSaveTaskChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @Override
        public void performChanges() {
            JSONArray addTags = changes.optJSONArray("tag_added");
            if (addTags != null) {
                TagService tagService = TagService.getInstance();
                for (int i = 0; i < addTags.length(); i++) {
                    try {
                        String tagUuid = addTags.getString(i);
                        tagService.createLink(model.getId(), uuid, tagUuid);
                    } catch (JSONException e) {
                        //
                    }
                }
            }

            JSONArray removeTags = changes.optJSONArray("tag_removed");
            if (removeTags != null) {
                ArrayList<String> toRemove = new ArrayList<String>(removeTags.length());
                for (int i = 0; i < removeTags.length(); i++) {
                    try {
                        String tagUuid = removeTags.getString(i);
                        toRemove.add(tagUuid);
                    } catch (JSONException e) {
                        //
                    }
                }
                TagService.getInstance().deleteLinks(uuid, toRemove.toArray(new String[toRemove.size()]));
            }
        }

    }



}
