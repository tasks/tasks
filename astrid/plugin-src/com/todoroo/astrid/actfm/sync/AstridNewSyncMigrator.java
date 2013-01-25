package com.todoroo.astrid.actfm.sync;

import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.dao.UserDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class AstridNewSyncMigrator {

    @Autowired private MetadataService metadataService;
    @Autowired private TagDataService tagDataService;
    @Autowired private TagDataDao tagDataDao;
    @Autowired private TaskDao taskDao;
    @Autowired private UpdateDao updateDao;
    @Autowired private UserDao userDao;

    public static final String PREF_SYNC_MIGRATION = "sync_migration";

    private static final String LOG_TAG = "sync-migrate";

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

        TodorooCursor<Metadata> noTagData = metadataService.query(noTagDataQuery);
        try {
            Metadata tag = new Metadata();
            for (noTagData.moveToFirst(); !noTagData.isAfterLast(); noTagData.moveToNext()) {
                tag.readFromCursor(noTagData);

                if (Constants.DEBUG)
                    Log.w(LOG_TAG, "CREATING TAG DATA " + tag.getValue(TaskToTagMetadata.TAG_NAME));

                TagData newTagData = new TagData();
                newTagData.setValue(TagData.NAME, tag.getValue(TaskToTagMetadata.TAG_NAME));
                tagDataService.save(newTagData);
            }
        } finally {
            noTagData.close();
        }

        // --------------
        // Then ensure that every remote model has a remote id, by generating one using the uuid generator for all those without one
        // --------------
        Query tagsQuery = Query.select(TagData.ID, TagData.REMOTE_ID).where(Criterion.all);
        assertUUIDsExist(tagsQuery, new TagData(), tagDataDao, null);

        Query tasksQuery = Query.select(Task.ID, Task.REMOTE_ID, Task.RECURRENCE, Task.FLAGS).where(Criterion.all);
        assertUUIDsExist(tasksQuery, new Task(), taskDao, new UUIDAssertionExtras<Task>() {
            @Override
            public void beforeSave(Task instance) {
                if (instance.getFlag(Task.FLAGS, Task.FLAG_IS_READONLY)) {
                    instance.setFlag(Task.FLAGS, Task.FLAG_IS_READONLY, false);
                    instance.setValue(Task.IS_READONLY, 1);
                }

                if (instance.getFlag(Task.FLAGS, Task.FLAG_PUBLIC)) {
                    instance.setFlag(Task.FLAGS, Task.FLAG_PUBLIC, false);
                    instance.setValue(Task.IS_PUBLIC, 1);
                }

                String recurrence = instance.getValue(Task.RECURRENCE);
                if (!TextUtils.isEmpty(recurrence)) {
                    boolean repeatAfterCompletion = instance.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION);
                    instance.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, false);

                    recurrence = recurrence.replaceAll("BYDAY=;", "");
                    if (repeatAfterCompletion)
                        recurrence = recurrence + ";FROM=COMPLETION";
                    instance.setValue(Task.RECURRENCE, recurrence);
                }
            }
        });

        Query updatesWithoutUUIDQuery = Query.select(Update.ID, Update.REMOTE_ID, Update.TASK).where(Criterion.all);
        assertUUIDsExist(updatesWithoutUUIDQuery, new Update(), updateDao, new UUIDAssertionExtras<Update>() {
            @Override
            public void beforeSave(Update instance) {
                // Migrate Update.TASK long to Update.TASK_UUID string
                if (instance.getValue(Update.TASK) != 0)
                    instance.setValue(Update.TASK_UUID, Long.toString(instance.getValue(Update.TASK)));
            }
        });


        // --------------
        // Finally, ensure that all tag metadata entities have all important fields filled in
        // --------------
        Query incompleteQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                Criterion.or(TaskToTagMetadata.TASK_UUID.eq(0), TaskToTagMetadata.TASK_UUID.isNull(),
                        TaskToTagMetadata.TAG_UUID.eq(0), TaskToTagMetadata.TAG_UUID.isNull())));
        TodorooCursor<Metadata> incompleteMetadata = metadataService.query(incompleteQuery);
        try {
            Metadata m = new Metadata();
            for (incompleteMetadata.moveToFirst(); !incompleteMetadata.isAfterLast(); incompleteMetadata.moveToNext()) {
                m.clear(); // Need this since some properties may be null
                m.readFromCursor(incompleteMetadata);
                boolean changes = false;

                if (Constants.DEBUG)
                    Log.w(LOG_TAG, "Incomplete linking task " + m.getValue(Metadata.TASK) + " to " + m.getValue(TaskToTagMetadata.TAG_NAME));

                if (!m.containsNonNullValue(TaskToTagMetadata.TASK_UUID) || RemoteModel.NO_UUID.equals(m.getValue(TaskToTagMetadata.TASK_UUID))) {
                    if (Constants.DEBUG)
                        Log.w(LOG_TAG, "No task uuid");
                    updateTaskUuid(m);
                    changes = true;
                }

                if (!m.containsNonNullValue(TaskToTagMetadata.TAG_UUID) || RemoteModel.NO_UUID.equals(m.getValue(TaskToTagMetadata.TAG_UUID))) {
                    if (Constants.DEBUG)
                        Log.w(LOG_TAG, "No tag uuid");
                    updateTagUuid(m);
                    changes = true;
                }

                if (changes)
                    metadataService.save(m);

            }
        } finally {
            incompleteMetadata.close();
        }


        Preferences.setBoolean(PREF_SYNC_MIGRATION, true);
    }

    private interface UUIDAssertionExtras<TYPE extends RemoteModel> {
        void beforeSave(TYPE instance);
    }

    private <TYPE extends RemoteModel> void assertUUIDsExist(Query query, TYPE instance, DatabaseDao<TYPE> dao, UUIDAssertionExtras<TYPE> extras) {
        TodorooCursor<TYPE> cursor = dao.query(query);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                instance.readPropertiesFromCursor(cursor);
                if (!instance.containsNonNullValue(RemoteModel.REMOTE_ID_PROPERTY) || instance.getValue(RemoteModel.REMOTE_ID_PROPERTY) == 0) {
                    // No remote id exists, just create a UUID
                    instance.setValue(RemoteModel.UUID_PROPERTY, UUIDHelper.newUUID());
                } else {
                    // Migrate remote id to uuid field
                    instance.setValue(RemoteModel.UUID_PROPERTY, Long.toString(instance.getValue(RemoteModel.REMOTE_ID_PROPERTY)));
                }
                if (extras != null)
                    extras.beforeSave(instance);

                instance.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                dao.saveExisting(instance);
            }
        } finally {
            cursor.close();
        }
    }

    private void updateTaskUuid(Metadata m) {
        long taskId = m.getValue(Metadata.TASK);
        Task task = taskDao.fetch(taskId, Task.UUID);
        if (task != null) {
            if (Constants.DEBUG)
                Log.w(LOG_TAG, "Linking with task uuid " + task.getValue(Task.UUID));
            m.setValue(TaskToTagMetadata.TASK_UUID, task.getValue(Task.UUID));
        } else {
            if (Constants.DEBUG)
                Log.w(LOG_TAG, "Task not found, deleting link");
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

    private void updateTagUuid(Metadata m) {
        String tag = m.getValue(TaskToTagMetadata.TAG_NAME);
        TagData tagData = tagDataService.getTag(tag, TagData.UUID);
        if (tagData != null) {
            if (Constants.DEBUG)
                Log.w(LOG_TAG, "Linking with tag uuid " + tagData.getValue(TagData.UUID));
            m.setValue(TaskToTagMetadata.TAG_UUID, tagData.getValue(TagData.UUID));
        } else {
            if (Constants.DEBUG)
                Log.w(LOG_TAG, "Tag not found, deleting link");
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

}
