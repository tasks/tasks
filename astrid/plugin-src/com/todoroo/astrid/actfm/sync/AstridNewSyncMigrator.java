package com.todoroo.astrid.actfm.sync;

import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.files.FileMetadata;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.subtasks.SubtasksUpdater;
import com.todoroo.astrid.tags.TaskToTagMetadata;

@SuppressWarnings("nls")
public class AstridNewSyncMigrator {

    @Autowired private MetadataService metadataService;
    @Autowired private TagDataService tagDataService;
    @Autowired private TagDataDao tagDataDao;
    @Autowired private TaskDao taskDao;
    @Autowired private UpdateDao updateDao;
    @Autowired private UserActivityDao userActivityDao;
    @Autowired private UserDao userDao;
    @Autowired private TaskAttachmentDao taskAttachmentDao;
    @Autowired private TaskListMetadataDao taskListMetadataDao;

    @Autowired private TaskOutstandingDao taskOutstandingDao;
    @Autowired private TagOutstandingDao tagOutstandingDao;

    private static final String LOG_TAG = "sync-migrate";

    public static final String PREF_SYNC_MIGRATION = "p_sync_migration";

    public AstridNewSyncMigrator() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @SuppressWarnings("deprecation")
    public void performMigration() {
        if (Preferences.getBoolean(PREF_SYNC_MIGRATION, false))
            return;

        // --------------
        // First ensure that a TagData object exists for each tag metadata
        // --------------
        Query noTagDataQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                Criterion.or(TaskToTagMetadata.TAG_UUID.isNull(), TaskToTagMetadata.TAG_UUID.eq(0)),
                Criterion.not(TaskToTagMetadata.TAG_NAME.in(Query.select(TagData.NAME).from(TagData.TABLE))))).groupBy(TaskToTagMetadata.TAG_NAME);

        TodorooCursor<Metadata> noTagData = null;
        try {
            noTagData = metadataService.query(noTagDataQuery);
            Metadata tag = new Metadata();
            TagData newTagData = new TagData();
            for (noTagData.moveToFirst(); !noTagData.isAfterLast(); noTagData.moveToNext()) {
                try {
                    newTagData.clear();
                    tag.clear();
                    tag.readFromCursor(noTagData);

                    if (ActFmInvoker.SYNC_DEBUG)
                        Log.w(LOG_TAG, "CREATING TAG DATA " + tag.getValue(TaskToTagMetadata.TAG_NAME));

                    newTagData.setValue(TagData.NAME, tag.getValue(TaskToTagMetadata.TAG_NAME));
                    tagDataService.save(newTagData);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error creating tag data", e);
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating tag data", e);
            Crittercism.logHandledException(e);
        } finally {
            if (noTagData != null)
                noTagData.close();
        }

        // --------------
        // Delete all emergent tag data, we don't need it
        // --------------
        TodorooCursor<TagData> emergentTags = null;
        try {
            emergentTags = tagDataDao.query(Query.select(TagData.ID, TagData.NAME).where(Functions.bitwiseAnd(TagData.FLAGS, TagData.FLAG_EMERGENT).gt(0)));
            TagData td = new TagData();
            for (emergentTags.moveToFirst(); !emergentTags.isAfterLast(); emergentTags.moveToNext()) {
                try {
                    td.clear();
                    td.readFromCursor(emergentTags);
                    String name = td.getValue(TagData.NAME);
                    tagDataDao.delete(td.getId());
                    if (!TextUtils.isEmpty(name))
                        metadataService.deleteWhere(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_NAME.eq(name)));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error clearing emergent tags");
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e){
            Crittercism.logHandledException(e);
        } finally {
            if (emergentTags != null)
                emergentTags.close();
        }

        // --------------
        // Then ensure that every remote model has a remote id, by generating one using the uuid generator for all those without one
        // --------------
        final Set<Long> tasksThatNeedTagSync = new HashSet<Long>();
        try {
            Query tagsQuery = Query.select(TagData.ID, TagData.UUID, TagData.MODIFICATION_DATE)
                    .where(Criterion.or(TagData.UUID.eq(RemoteModel.NO_UUID), TagData.UUID.isNull(), TagData.UUID.eq("")));
            assertUUIDsExist(tagsQuery, new TagData(), tagDataDao, tagOutstandingDao, new TagOutstanding(), NameMaps.syncableProperties(NameMaps.TABLE_ID_TAGS), new UUIDAssertionExtras<TagData>() {
                private static final String LAST_TAG_FETCH_TIME = "actfm_lastTag"; //$NON-NLS-1$
                private final long lastFetchTime = Preferences.getInt(LAST_TAG_FETCH_TIME, 0) * 1000L;

                @Override
                public boolean shouldCreateOutstandingEntries(TagData instance) {
                    boolean result = lastFetchTime == 0 || (instance.containsNonNullValue(TagData.MODIFICATION_DATE) && instance.getValue(TagData.MODIFICATION_DATE) > lastFetchTime);
                    return result && RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
                }

                @Override
                public void afterSave(TagData instance, boolean createdOutstanding) {/**/}
            });

            Query tasksQuery = Query.select(Task.ID, Task.UUID, Task.RECURRENCE, Task.FLAGS, Task.MODIFICATION_DATE, Task.LAST_SYNC)
                    .where(Criterion.or(Task.UUID.eq(RemoteModel.NO_UUID), Task.UUID.isNull(), Task.UUID.eq("")));
            assertUUIDsExist(tasksQuery, new Task(), taskDao, taskOutstandingDao, new TaskOutstanding(), NameMaps.syncableProperties(NameMaps.TABLE_ID_TASKS), new UUIDAssertionExtras<Task>() {
                @Override
                public boolean shouldCreateOutstandingEntries(Task instance) {
                    if (!instance.containsNonNullValue(Task.MODIFICATION_DATE) || instance.getValue(Task.LAST_SYNC) == 0)
                        return RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);

                    return (instance.getValue(Task.LAST_SYNC) < instance.getValue(Task.MODIFICATION_DATE)) && RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_RECORD_OUTSTANDING);
                }

                @Override
                public void afterSave(Task instance, boolean createdOutstanding) {
                    if (createdOutstanding)
                        tasksThatNeedTagSync.add(instance.getId());
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error asserting UUIDs", e);
            Crittercism.logHandledException(e);
        }

        // --------------
        // Update task flags
        // --------------
        Task template = new Task();
        try {
            template.setValue(Task.IS_READONLY, 1);
            taskDao.update(Functions.bitwiseAnd(Task.FLAGS, Task.FLAG_IS_READONLY).gt(0), template);
            template.clear();
            template.setValue(Task.IS_PUBLIC, 1);
            taskDao.update(Functions.bitwiseAnd(Task.FLAGS, Task.FLAG_PUBLIC).gt(0), template);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error clearing task flags", e);
            Crittercism.logHandledException(e);
        }

        // --------------
        // Update recurrence values
        // --------------
        TodorooCursor<Task> tasksWithRecurrence = null;
        try {
            tasksWithRecurrence = taskDao.query(Query.select(Task.ID, Task.FLAGS, Task.RECURRENCE).where(Criterion.or(Task.RECURRENCE.isNotNull(), Task.RECURRENCE.neq(""))));
            for (tasksWithRecurrence.moveToFirst(); !tasksWithRecurrence.isAfterLast(); tasksWithRecurrence.moveToNext()) {
                try {
                    template.clear();
                    template.readFromCursor(tasksWithRecurrence);
                    String recurrence = template.getValue(Task.RECURRENCE);
                    if (!TextUtils.isEmpty(recurrence)) {
                        String fromCompletion = ";FROM=COMPLETION";
                        boolean repeatAfterCompletion = template.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION);
                        template.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

                        recurrence = recurrence.replaceAll("BYDAY=;", "");
                        if (fromCompletion.equals(recurrence))
                            recurrence = "";
                        else if (repeatAfterCompletion)
                            recurrence = recurrence + fromCompletion;
                        template.setValue(Task.RECURRENCE, recurrence);

                        template.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
                        taskDao.saveExisting(template);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error migrating recurrence", e);
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating recurrence", e);
            Crittercism.logHandledException(e);
        } finally {
            if (tasksWithRecurrence != null)
                tasksWithRecurrence.close();
        }

        // --------------
        // Migrate unsynced task comments to UserActivity table
        // --------------
        TodorooCursor<Update> updates = null;
        try {
            updates = updateDao.query(Query.select(Update.PROPERTIES).where(
                    Criterion.and(Criterion.or(Update.REMOTE_ID.eq(0), Update.REMOTE_ID.isNull()), Criterion.or(Update.ACTION_CODE.eq(UserActivity.ACTION_TAG_COMMENT),
                            Update.ACTION_CODE.eq(UserActivity.ACTION_TASK_COMMENT)))));
            Update update = new Update();
            UserActivity userActivity = new UserActivity();
            for (updates.moveToFirst(); !updates.isAfterLast(); updates.moveToNext()) {
                try {
                    update.clear();
                    userActivity.clear();

                    update.readFromCursor(updates);

                    boolean setTarget = true;
                    if (!RemoteModel.isUuidEmpty(update.getValue(Update.TASK).toString())) {
                        userActivity.setValue(UserActivity.TARGET_ID, update.getValue(Update.TASK).toString());
                    } else if (update.getValue(Update.TASK_LOCAL) > 0) {
                        Task local = taskDao.fetch(update.getValue(Update.TASK_LOCAL), Task.UUID);
                        if (local != null && !RemoteModel.isUuidEmpty(local.getUuid()))
                            userActivity.setValue(UserActivity.TARGET_ID, local.getUuid());
                        else
                            setTarget = false;
                    } else {
                        setTarget = false;
                    }

                    if (setTarget) {
                        userActivity.setValue(UserActivity.USER_UUID, update.getValue(Update.USER_ID).toString());
                        userActivity.setValue(UserActivity.ACTION, update.getValue(Update.ACTION_CODE));
                        userActivity.setValue(UserActivity.MESSAGE, update.getValue(Update.MESSAGE));
                        userActivity.setValue(UserActivity.CREATED_AT, update.getValue(Update.CREATION_DATE));
                        userActivityDao.createNew(userActivity);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error migrating updates", e);
                    Crittercism.logHandledException(e);
                }

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating updates", e);
            Crittercism.logHandledException(e);
        } finally {
            if (updates != null)
                updates.close();
        }



        // --------------
        // Drop any entries from the Users table that don't have a UUID
        // --------------
        try {
            userDao.deleteWhere(Criterion.or(User.UUID.isNull(), User.UUID.eq(""), User.UUID.eq("0")));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error deleting incomplete user entries", e);
            Crittercism.logHandledException(e);
        }

        // --------------
        // Migrate legacy FileMetadata models to new TaskAttachment models
        // --------------
        TodorooCursor<Metadata> fmCursor = null;
        try {
            fmCursor = metadataService.query(Query.select(Metadata.PROPERTIES)
                    .where(MetadataCriteria.withKey(FileMetadata.METADATA_KEY)));
            Metadata m = new Metadata();
            TaskAttachment attachment = new TaskAttachment();
            for (fmCursor.moveToFirst(); !fmCursor.isAfterLast(); fmCursor.moveToNext()) {
                try {
                    attachment.clear();
                    m.clear();
                    m.readFromCursor(fmCursor);

                    Task task = taskDao.fetch(m.getValue(Metadata.TASK), Task.UUID);
                    if (task == null || !RemoteModel.isValidUuid(task.getUuid()))
                        continue;

                    Long oldRemoteId = m.getValue(FileMetadata.REMOTE_ID);
                    boolean synced = false;
                    if (oldRemoteId != null && oldRemoteId > 0) {
                        synced = true;
                        attachment.setValue(TaskAttachment.UUID, Long.toString(oldRemoteId));
                    }
                    attachment.setValue(TaskAttachment.TASK_UUID, task.getUuid());
                    if (m.containsNonNullValue(FileMetadata.NAME))
                        attachment.setValue(TaskAttachment.NAME, m.getValue(FileMetadata.NAME));
                    if (m.containsNonNullValue(FileMetadata.URL))
                        attachment.setValue(TaskAttachment.URL, m.getValue(FileMetadata.URL));
                    if (m.containsNonNullValue(FileMetadata.FILE_PATH))
                        attachment.setValue(TaskAttachment.FILE_PATH, m.getValue(FileMetadata.FILE_PATH));
                    if (m.containsNonNullValue(FileMetadata.FILE_TYPE))
                        attachment.setValue(TaskAttachment.CONTENT_TYPE, m.getValue(FileMetadata.FILE_TYPE));
                    if (m.containsNonNullValue(FileMetadata.DELETION_DATE))
                        attachment.setValue(TaskAttachment.DELETED_AT, m.getValue(FileMetadata.DELETION_DATE));

                    if (synced) {
                        attachment.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                    }

                    if (!ActFmPreferenceService.isPremiumUser())
                        attachment.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                    taskAttachmentDao.createNew(attachment);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error migrating task attachment metadata", e);
                    Crittercism.logHandledException(e);
                }

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating task attachment metadata", e);
            Crittercism.logHandledException(e);
        } finally {
            if (fmCursor != null)
                fmCursor.close();
        }

        // --------------
        // Create task list metadata entries for each tag
        // --------------
        TaskListMetadata tlm = new TaskListMetadata();
        try {
            String activeTasksOrder = Preferences.getStringValue(SubtasksUpdater.ACTIVE_TASKS_ORDER);
            if (TextUtils.isEmpty(activeTasksOrder))
                activeTasksOrder = "[]";

            activeTasksOrder = SubtasksHelper.convertTreeToRemoteIds(activeTasksOrder);

            tlm.setValue(TaskListMetadata.FILTER, TaskListMetadata.FILTER_ID_ALL);
            tlm.setValue(TaskListMetadata.TASK_IDS, activeTasksOrder);
            if (taskListMetadataDao.update(TaskListMetadata.FILTER.eq(TaskListMetadata.FILTER_ID_ALL), tlm) <= 0) {
                taskListMetadataDao.createNew(tlm);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating active tasks ordering", e);
            Crittercism.logHandledException(e);
        }

        try {
            tlm.clear();
            String todayTasksOrder = Preferences.getStringValue(SubtasksUpdater.TODAY_TASKS_ORDER);
            if (TextUtils.isEmpty(todayTasksOrder))
                todayTasksOrder = "[]";

            todayTasksOrder = SubtasksHelper.convertTreeToRemoteIds(todayTasksOrder);

            tlm.setValue(TaskListMetadata.FILTER, TaskListMetadata.FILTER_ID_TODAY);
            tlm.setValue(TaskListMetadata.TASK_IDS, todayTasksOrder);
            if (taskListMetadataDao.update(TaskListMetadata.FILTER.eq(TaskListMetadata.FILTER_ID_TODAY), tlm) <= 0) {
                taskListMetadataDao.createNew(tlm);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating today ordering", e);
            Crittercism.logHandledException(e);
        }

        TodorooCursor<TagData> allTagData = null;
        try {
            allTagData = tagDataDao.query(Query.select(TagData.ID, TagData.UUID, TagData.TAG_ORDERING));
            TagData td = new TagData();
            for (allTagData.moveToFirst(); !allTagData.isAfterLast(); allTagData.moveToNext()) {
                try {
                    tlm.clear();
                    td.clear();

                    td.readFromCursor(allTagData);
                    String tagOrdering = td.getValue(TagData.TAG_ORDERING);
                    tagOrdering = SubtasksHelper.convertTreeToRemoteIds(tagOrdering);

                    tlm.setValue(TaskListMetadata.TASK_IDS, tagOrdering);
                    tlm.setValue(TaskListMetadata.TAG_UUID, td.getUuid());
                    taskListMetadataDao.createNew(tlm);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error migrating tag ordering", e);
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error migrating tag ordering", e);
            Crittercism.logHandledException(e);
        } finally {
            if (allTagData != null)
                allTagData.close();
        }

        // --------------
        // Ensure that all tag metadata entities have all important fields filled in
        // --------------
        TodorooCursor<Metadata> incompleteMetadata = null;
        try {
            Query incompleteQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                    MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                    Criterion.or(TaskToTagMetadata.TASK_UUID.eq(0), TaskToTagMetadata.TASK_UUID.isNull(),
                            TaskToTagMetadata.TAG_UUID.eq(0), TaskToTagMetadata.TAG_UUID.isNull())));
            incompleteMetadata = metadataService.query(incompleteQuery);;
            Metadata m = new Metadata();
            for (incompleteMetadata.moveToFirst(); !incompleteMetadata.isAfterLast(); incompleteMetadata.moveToNext()) {
                try {
                    m.clear(); // Need this since some properties may be null
                    m.readFromCursor(incompleteMetadata);

                    if (ActFmInvoker.SYNC_DEBUG)
                        Log.w(LOG_TAG, "Incomplete linking task " + m.getValue(Metadata.TASK) + " to " + m.getValue(TaskToTagMetadata.TAG_NAME));

                    if (!m.containsNonNullValue(TaskToTagMetadata.TASK_UUID) || RemoteModel.isUuidEmpty(m.getValue(TaskToTagMetadata.TASK_UUID))) {
                        if (ActFmInvoker.SYNC_DEBUG)
                            Log.w(LOG_TAG, "No task uuid");
                        updateTaskUuid(m);
                    }

                    if (!m.containsNonNullValue(TaskToTagMetadata.TAG_UUID) || RemoteModel.isUuidEmpty(m.getValue(TaskToTagMetadata.TAG_UUID))) {
                        if (ActFmInvoker.SYNC_DEBUG)
                            Log.w(LOG_TAG, "No tag uuid");
                        updateTagUuid(m);
                    }

                    if (m.getSetValues() != null && m.getSetValues().size() > 0)
                        metadataService.save(m);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error validating task to tag metadata", e);
                    Crittercism.logHandledException(e);
                }

            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error validating task to tag metadata", e);
            Crittercism.logHandledException(e);
        } finally {
            if (incompleteMetadata != null)
                incompleteMetadata.close();
        }

        // --------------
        // Delete all featured list data
        // --------------
        try {
            tagDataDao.deleteWhere(Functions.bitwiseAnd(TagData.FLAGS, TagData.FLAG_FEATURED).gt(0));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error deleting featured list data", e);
            Crittercism.logHandledException(e);
        }


        // --------------
        // Finally, create oustanding entries for tags on unsynced tasks
        // --------------
        TodorooCursor<Metadata> tagsAdded = null;
        try {
            Long[] ids = tasksThatNeedTagSync.toArray(new Long[tasksThatNeedTagSync.size()]);
            tagsAdded = metadataService.query(Query.select(Metadata.PROPERTIES)
                    .where(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.TASK.in(ids))).orderBy(Order.asc(Metadata.TASK)));
            Metadata m = new Metadata();
            for (tagsAdded.moveToFirst(); !tagsAdded.isAfterLast(); tagsAdded.moveToNext()) {
                try {
                    m.clear();
                    m.readFromCursor(tagsAdded);
                    Long deletionDate = m.getValue(Metadata.DELETION_DATE);
                    String tagUuid = m.getValue(TaskToTagMetadata.TAG_UUID);
                    if (!RemoteModel.isValidUuid(tagUuid))
                        continue;

                    TaskOutstanding to = new TaskOutstanding();
                    to.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, m.getValue(Metadata.TASK));
                    to.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());
                    String addedOrRemoved = NameMaps.TAG_ADDED_COLUMN;
                    if (deletionDate != null && deletionDate > 0)
                        addedOrRemoved = NameMaps.TAG_REMOVED_COLUMN;

                    to.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, addedOrRemoved);
                    to.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, tagUuid);
                    taskOutstandingDao.createNew(to);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error creating tag_added outstanding entries", e);
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating tag_added outstanding entries", e);
            Crittercism.logHandledException(e);
        } finally {
            if (tagsAdded != null)
                tagsAdded.close();
        }

        Preferences.setBoolean(PREF_SYNC_MIGRATION, true);
        ActFmSyncMonitor monitor = ActFmSyncMonitor.getInstance();
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    private interface UUIDAssertionExtras<TYPE extends RemoteModel> {
        boolean shouldCreateOutstandingEntries(TYPE instance);
        void afterSave(TYPE instance, boolean createdOutstanding);
    }

    private <TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> void assertUUIDsExist(Query query, TYPE instance, DatabaseDao<TYPE> dao, OutstandingEntryDao<OE> oeDao, OE oe, Property<?>[] propertiesForOutstanding, UUIDAssertionExtras<TYPE> extras) {
        TodorooCursor<TYPE> cursor = null;
        try {
            cursor = dao.query(query);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                try {
                    instance.clear();
                    instance.readPropertiesFromCursor(cursor);
                    boolean unsyncedModel = false;
                    if (!instance.containsNonNullValue(RemoteModel.UUID_PROPERTY) || RemoteModel.NO_UUID.equals(instance.getValue(RemoteModel.UUID_PROPERTY)) ||
                            "".equals(instance.getValue(RemoteModel.UUID_PROPERTY)) || "null".equals(instance.getValue(RemoteModel.UUID_PROPERTY))) {
                        // No remote id exists, just create a UUID
                        unsyncedModel = true;
                        instance.setValue(RemoteModel.UUID_PROPERTY, UUIDHelper.newUUID());
                    }

                    instance.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                    instance.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
                    dao.saveExisting(instance);
                    boolean createdOutstanding = false;
                    if (propertiesForOutstanding != null && (unsyncedModel || (extras != null && extras.shouldCreateOutstandingEntries(instance)))) {
                        createdOutstanding = true;
                        createOutstandingEntries(instance.getId(), dao, oeDao, oe, propertiesForOutstanding);
                    }
                    if (extras != null)
                        extras.afterSave(instance, createdOutstanding);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error asserting UUIDs", e);
                    Crittercism.logHandledException(e);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error asserting UUIDs", e);
            Crittercism.logHandledException(e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private <TYPE extends RemoteModel, OE extends OutstandingEntry<TYPE>> void createOutstandingEntries(long id, DatabaseDao<TYPE> dao, OutstandingEntryDao<OE> oeDao, OE oe, Property<?>[] propertiesForOutstanding) {
        TYPE instance = dao.fetch(id, propertiesForOutstanding);
        long now = DateUtilities.now();
        for (Property<?> property : propertiesForOutstanding) {
            oe.clear();
            oe.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, id);
            oe.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, property.name);
            Object value = instance.getValue(property);
            if (value == null)
                value = "";
            oe.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, value.toString());
            oe.setValue(OutstandingEntry.CREATED_AT_PROPERTY, now);
            oeDao.createNew(oe);
        }
    }

    private void updateTaskUuid(Metadata m) {
        long taskId = m.getValue(Metadata.TASK);
        Task task = taskDao.fetch(taskId, Task.UUID);
        if (task != null) {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.w(LOG_TAG, "Linking with task uuid " + task.getValue(Task.UUID));
            m.setValue(TaskToTagMetadata.TASK_UUID, task.getValue(Task.UUID));
        } else {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.w(LOG_TAG, "Task not found, deleting link");
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

    private void updateTagUuid(Metadata m) {
        String tag = m.getValue(TaskToTagMetadata.TAG_NAME);
        TagData tagData = tagDataService.getTagByName(tag, TagData.UUID);
        if (tagData != null) {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.w(LOG_TAG, "Linking with tag uuid " + tagData.getValue(TagData.UUID));
            m.setValue(TaskToTagMetadata.TAG_UUID, tagData.getValue(TagData.UUID));
        } else {
            if (ActFmInvoker.SYNC_DEBUG)
                Log.w(LOG_TAG, "Tag not found, deleting link");
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

}
