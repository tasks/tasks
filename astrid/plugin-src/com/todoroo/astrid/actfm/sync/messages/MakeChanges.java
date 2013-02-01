package com.todoroo.astrid.actfm.sync.messages;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

@SuppressWarnings("nls")
public class MakeChanges<TYPE extends RemoteModel> extends ServerToClientMessage {

    private static final String ERROR_TAG = "actfm-make-changes";

    private final RemoteModelDao<TYPE> dao;
    private final String table;

    public MakeChanges(JSONObject json, RemoteModelDao<TYPE> dao) {
        super(json);
        this.table = json.optString("table");
        this.dao = dao;
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
                    if (model.getSetValues() != null && model.getSetValues().containsKey(uuidProperty.name))
                        uuid = model.getValue(uuidProperty);

                    beforeSaveChanges(changes, model, uuid);

                    if (model.getSetValues() != null && !model.getSetValues().containsKey(uuidProperty.name))
                        model.setValue(uuidProperty, uuid);

                    if (model.getSetValues() != null && model.getSetValues().size() > 0) {
                        model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                        if (dao.update(RemoteModel.UUID_PROPERTY.eq(uuid), model) <= 0) { // If update doesn't update rows. create a new model
                            model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                            dao.createNew(model);
                        }
                    }
                    afterSaveChanges(changes, model, uuid);

                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                }
            }
        }
    }

    private void beforeSaveChanges(JSONObject changes, TYPE model, String uuid) {
        ChangeHooks beforeSaveChanges = null;
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            beforeSaveChanges = new BeforeSaveTaskChanges(model, changes, uuid);
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            beforeSaveChanges = new BeforeSaveTagChanges(model, changes, uuid);

        if (beforeSaveChanges != null)
            beforeSaveChanges.performChanges();
    }

    private void afterSaveChanges(JSONObject changes, TYPE model, String uuid) {
        ChangeHooks afterSaveChanges = null;
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            afterSaveChanges = new AfterSaveTaskChanges(model, changes, uuid);
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            afterSaveChanges = new AfterSaveTagChanges(model, changes, uuid);

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

        protected long getLocalId() {
            long localId;
            if (!model.isSaved()) { // We don't have the local task id
                localId = dao.localIdFromUuid(uuid);
                model.setId(localId);
            } else {
                localId = model.getId();
            }
            return localId;
        }
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

    private class BeforeSaveTagChanges extends ChangeHooks {

        public BeforeSaveTagChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @Override
        public void performChanges() {
            JSONArray addMembers = changes.optJSONArray("member_added");
            boolean membersAdded = (addMembers != null && addMembers.length() > 0);
            if (membersAdded)
                model.setValue(TagData.MEMBERS, ""); // Clear this value for migration purposes
        }
    }

    private class AfterSaveTaskChanges extends ChangeHooks {

        public AfterSaveTaskChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @SuppressWarnings("null")
        @Override
        public void performChanges() {
            JSONArray addTags = changes.optJSONArray("tag_added");
            JSONArray removeTags = changes.optJSONArray("tag_removed");
            boolean tagsAdded = (addTags != null && addTags.length() > 0);
            boolean tagsRemoved = (removeTags != null && removeTags.length() > 0);
            if (!tagsAdded && !tagsRemoved)
                return;

            long localId = AbstractModel.NO_ID;
            if (tagsAdded || tagsRemoved)
                localId = getLocalId();

            if (tagsAdded) {
                if (model.isSaved()) {
                    TagService tagService = TagService.getInstance();
                    for (int i = 0; i < addTags.length(); i++) {
                        try {
                            String tagUuid = addTags.getString(i);
                            tagService.createLink(model.getId(), uuid, tagUuid, true);
                        } catch (JSONException e) {
                            //
                        }
                    }
                }
            }

            if (tagsRemoved) {
                ArrayList<String> toRemove = new ArrayList<String>(removeTags.length());
                for (int i = 0; i < removeTags.length(); i++) {
                    try {
                        String tagUuid = removeTags.getString(i);
                        toRemove.add(tagUuid);
                    } catch (JSONException e) {
                        //
                    }
                }
                TagService.getInstance().deleteLinks(localId, uuid, toRemove.toArray(new String[toRemove.size()]), true);
            }
        }

    }

    private class AfterSaveTagChanges extends ChangeHooks {

        public AfterSaveTagChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @SuppressWarnings("null")
        @Override
        public void performChanges() {
            if (changes.has("name")) {
                Metadata template = new Metadata();
                template.setValue(TaskToTagMetadata.TAG_NAME, changes.optString("name"));
                PluginServices.getMetadataService().update(
                        Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                                TaskToTagMetadata.TAG_UUID.eq(uuid)), template);
            }

            TagMetadataDao tagMetadataDao = PluginServices.getTagMetadataDao();

            JSONArray addMembers = changes.optJSONArray("member_added");
            JSONArray removeMembers = changes.optJSONArray("member_removed");
            boolean membersAdded = (addMembers != null && addMembers.length() > 0);
            boolean membersRemoved = (removeMembers != null && removeMembers.length() > 0);

            long localId = AbstractModel.NO_ID;
            if (membersAdded || membersRemoved)
                localId = getLocalId();

            if (membersAdded) {
                for (int i = 0; i < addMembers.length(); i++) {
                    try {
                        String memberId = addMembers.getString(i);
                        tagMetadataDao.createMemberLink(localId, uuid, memberId, true);
                    } catch (JSONException e) {
                        //
                    }
                }
            }

            if (membersRemoved) {
                ArrayList<String> toRemove = new ArrayList<String>(removeMembers.length());
                for (int i = 0; i < removeMembers.length(); i++) {
                    try {
                        String tagUuid = removeMembers.getString(i);
                        toRemove.add(tagUuid);
                    } catch (JSONException e) {
                        //
                    }
                }
                tagMetadataDao.removeMemberLinks(localId, uuid, toRemove.toArray(new String[toRemove.size()]), true);
            }
        }
    }



}
