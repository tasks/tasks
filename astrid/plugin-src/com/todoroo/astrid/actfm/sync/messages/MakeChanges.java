package com.todoroo.astrid.actfm.sync.messages;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.HistoryDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagMetadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.MetadataService;
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

    public static <T extends RemoteModel> T changesToModel(RemoteModelDao<T> dao, JSONObject changes, String table) throws IllegalAccessException, InstantiationException {
        T model = dao.getModelClass().newInstance();
        JSONChangeToPropertyVisitor visitor = new JSONChangeToPropertyVisitor(model, changes);
        Iterator<String> keys = changes.keys();
        while (keys.hasNext()) {
            String column = keys.next();
            Property<?> property = NameMaps.serverColumnNameToLocalProperty(table, column);
            if (property != null) { // Unsupported property
                property.accept(visitor, column);
            }
        }
        return model;
    }

    private static <T extends RemoteModel> void saveOrUpdateModelAfterChanges(RemoteModelDao<T> dao, T model, String oldUuid, String uuid, String serverTime, Criterion orCriterion) {
        Criterion uuidCriterion;
        if (oldUuid == null)
            uuidCriterion = RemoteModel.UUID_PROPERTY.eq(uuid);
        else
            uuidCriterion = RemoteModel.UUID_PROPERTY.eq(oldUuid);

        if (orCriterion != null)
            uuidCriterion = Criterion.or(uuidCriterion, orCriterion);

        if (model.getSetValues() != null && model.getSetValues().size() > 0) {
            long pushedAt;
            try {
                pushedAt = DateUtilities.parseIso8601(serverTime);
            } catch (ParseException e) {
                pushedAt = 0;
            }

            if (pushedAt > 0)
                model.setValue(RemoteModel.PUSHED_AT_PROPERTY, pushedAt);

            model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            if (dao.update(uuidCriterion, model) <= 0) { // If update doesn't update rows. create a new model
                model.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                dao.createNew(model);
            }
        }
    }

    @Override
    public void processMessage(String serverTime) {
        JSONObject changes = json.optJSONObject("changes");
        String uuid = json.optString("uuid");
        if (changes != null && !TextUtils.isEmpty(uuid)) {
            if (dao != null) {
                try {
                    TYPE model = changesToModel(dao, changes, table);

                    StringProperty uuidProperty = (StringProperty) NameMaps.serverColumnNameToLocalProperty(table, "uuid");
                    String oldUuid = null; // For indicating that a uuid collision has occurred
                    if (model.getSetValues() != null && model.getSetValues().containsKey(uuidProperty.name)) {
                        oldUuid = uuid;
                        uuid = model.getValue(uuidProperty);
                    }

                    beforeSaveChanges(changes, model, uuid);

                    if (model.getSetValues() != null && !model.getSetValues().containsKey(uuidProperty.name))
                        model.setValue(uuidProperty, uuid);

                    saveOrUpdateModelAfterChanges(dao, model, oldUuid, uuid, serverTime, getMatchCriterion(model));
                    afterSaveChanges(changes, model, uuid, oldUuid);

                } catch (IllegalAccessException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                } catch (InstantiationException e) {
                    Log.e(ERROR_TAG, "Error instantiating model for MakeChanges", e);
                }
            }
        }
    }

    private Criterion getMatchCriterion(TYPE model) {
        if (NameMaps.TABLE_ID_TASK_LIST_METADATA.equals(table)) {
            if (model.getSetValues().containsKey(TaskListMetadata.FILTER.name))
                return TaskListMetadata.FILTER.eq(model.getSetValues().getAsString(TaskListMetadata.FILTER.name));
            else if (model.getSetValues().containsKey(TaskListMetadata.TAG_UUID.name))
                return TaskListMetadata.TAG_UUID.eq(model.getSetValues().getAsString(TaskListMetadata.TAG_UUID.name));
        }
        return null;
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

    private void afterSaveChanges(JSONObject changes, TYPE model, String uuid, String oldUuid) {
        ChangeHooks afterSaveChanges = null;
        if (NameMaps.TABLE_ID_TASKS.equals(table))
            afterSaveChanges = new AfterSaveTaskChanges(model, changes, uuid, oldUuid);
        else if (NameMaps.TABLE_ID_TAGS.equals(table))
            afterSaveChanges = new AfterSaveTagChanges(model, changes, uuid, oldUuid);
        else if (NameMaps.TABLE_ID_USERS.equals(table))
            afterSaveChanges = new AfterSaveUserChanges(model, changes, uuid);

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

        @SuppressWarnings("deprecation")
        @Override
        public void performChanges() {
            JSONArray addMembers = changes.optJSONArray("member_added");
            boolean membersAdded = (addMembers != null && addMembers.length() > 0);
            if (membersAdded)
                model.setValue(TagData.MEMBERS, ""); // Clear this value for migration purposes
        }
    }

    private class AfterSaveTaskChanges extends ChangeHooks {

        private final String oldUuid;

        public AfterSaveTaskChanges(TYPE model, JSONObject changes, String uuid, String oldUuid) {
            super(model, changes, uuid);
            this.oldUuid = oldUuid;
        }

        @SuppressWarnings("null")
        @Override
        public void performChanges() {
            if (!TextUtils.isEmpty(oldUuid) && !oldUuid.equals(uuid)) {
                uuidChanged(oldUuid, uuid);
            }

            if (changes.has(NameMaps.localPropertyToServerColumnName(NameMaps.TABLE_ID_TASKS, Task.DUE_DATE)) ||
                    changes.has(NameMaps.localPropertyToServerColumnName(NameMaps.TABLE_ID_TASKS, Task.COMPLETION_DATE))) {
                Task t = PluginServices.getTaskDao().fetch(uuid, ReminderService.NOTIFICATION_PROPERTIES);
                if (t != null) {
                    if ((changes.has("task_repeated") && t.getValue(Task.DUE_DATE) > DateUtilities.now()) || t.getValue(Task.COMPLETION_DATE) > 0)
                        Notifications.cancelNotifications(t.getId());
                    ReminderService.getInstance().scheduleAlarm(t);
                }
            }

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

        private void uuidChanged(String fromUuid, String toUuid) {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.e(ERROR_TAG, "Task UUID collision -- old uuid: " + fromUuid + ", new uuid: " + toUuid);

            // Update reference from UserActivity to task uuid
            UserActivityDao activityDao = PluginServices.getUserActivityDao();
            UserActivity activityTemplate = new UserActivity();
            activityTemplate.setValue(UserActivity.TARGET_ID, toUuid);
            activityTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            activityDao.update(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT), UserActivity.TARGET_ID.eq(fromUuid)), activityTemplate);

            // Update reference from task to tag metadata to task uuid
            MetadataService metadataService = PluginServices.getMetadataService();
            Metadata taskToTagTemplate = new Metadata();
            taskToTagTemplate.setValue(TaskToTagMetadata.TASK_UUID, toUuid);
            taskToTagTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            metadataService.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TASK_UUID.eq(fromUuid)), taskToTagTemplate);

            HistoryDao historyDao = PluginServices.getHistoryDao();
            History histTemplate = new History();
            histTemplate.setValue(History.TARGET_ID, toUuid);
            historyDao.update(Criterion.and(History.TABLE_ID.eq(NameMaps.TABLE_ID_TAGS), History.TARGET_ID.eq(oldUuid)), histTemplate);
        }

    }

    private class AfterSaveTagChanges extends ChangeHooks {

        private final String oldUuid;

        public AfterSaveTagChanges(TYPE model, JSONObject changes, String uuid, String oldUuid) {
            super(model, changes, uuid);
            this.oldUuid = oldUuid;
        }

        @SuppressWarnings("null")
        @Override
        public void performChanges() {
            if (!TextUtils.isEmpty(oldUuid) && !oldUuid.equals(uuid)) {
                uuidChanged(oldUuid, uuid);
            }

            String nameCol = NameMaps.localPropertyToServerColumnName(NameMaps.TABLE_ID_TAGS, TagData.NAME);
            String deletedCol = NameMaps.localPropertyToServerColumnName(NameMaps.TABLE_ID_TAGS, TagData.DELETION_DATE);
            if (changes.has(nameCol)) {
                Metadata template = new Metadata();
                template.setValue(TaskToTagMetadata.TAG_NAME, changes.optString(nameCol));
                PluginServices.getMetadataService().update(
                        Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                                TaskToTagMetadata.TAG_UUID.eq(uuid)), template);
            } else if (changes.has(deletedCol)) {
                Metadata template = new Metadata();
                String valueString = changes.optString(deletedCol);
                long deletedValue = 0;
                if (!TextUtils.isEmpty(valueString)) {
                    try {
                        deletedValue = DateUtilities.parseIso8601(valueString);
                    } catch (Exception e){
                        //
                    }
                }
                template.setValue(Metadata.DELETION_DATE, deletedValue);
                template.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
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

        private void uuidChanged(String fromUuid, String toUuid) {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.e(ERROR_TAG, "Tag UUID collision -- old uuid: " + fromUuid + ", new uuid: " + toUuid);

            UserActivityDao activityDao = PluginServices.getUserActivityDao();
            UserActivity activityTemplate = new UserActivity();
            activityTemplate.setValue(UserActivity.TARGET_ID, toUuid);
            activityTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            activityDao.update(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TAG_COMMENT), UserActivity.TARGET_ID.eq(fromUuid)), activityTemplate);

            // Update reference from task to tag metadata to tag uuid
            MetadataService metadataService = PluginServices.getMetadataService();
            Metadata taskToTagTemplate = new Metadata();
            taskToTagTemplate.setValue(TaskToTagMetadata.TAG_UUID, toUuid);
            taskToTagTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            metadataService.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(fromUuid)), taskToTagTemplate);

            // Update reference from tag metadata to tag uuid
            TagMetadataDao tagMetadataDao = PluginServices.getTagMetadataDao();
            TagMetadata memberMetadataTemplate = new TagMetadata();
            memberMetadataTemplate.setValue(TagMetadata.TAG_UUID, toUuid);
            memberMetadataTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            tagMetadataDao.update(TagMetadata.TAG_UUID.eq(fromUuid), memberMetadataTemplate);

            HistoryDao historyDao = PluginServices.getHistoryDao();
            History histTemplate = new History();
            histTemplate.setValue(History.TARGET_ID, toUuid);
            historyDao.update(Criterion.and(History.TABLE_ID.eq(NameMaps.TABLE_ID_TAGS), History.TARGET_ID.eq(oldUuid)), histTemplate);

            TaskListMetadataDao taskListMetadataDao = PluginServices.getTaskListMetadataDao();
            TaskListMetadata tlm = new TaskListMetadata();
            tlm.setValue(TaskListMetadata.TAG_UUID, toUuid);
            tlm.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            taskListMetadataDao.update(TaskListMetadata.TAG_UUID.eq(fromUuid), tlm);
        }
    }

    private class AfterSaveUserChanges extends ChangeHooks {

        public AfterSaveUserChanges(TYPE model, JSONObject changes, String uuid) {
            super(model, changes, uuid);
        }

        @Override
        public void performChanges() {
            Preferences.setBoolean(R.string.p_show_friends_view, true);
        }
    }



}
