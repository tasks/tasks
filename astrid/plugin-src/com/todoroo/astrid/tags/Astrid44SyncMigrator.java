package com.todoroo.astrid.tags;

import android.util.Log;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.utility.Constants;

public class Astrid44SyncMigrator {

    @Autowired private MetadataService metadataService;
    @Autowired private TagDataService tagDataService;
    @Autowired private TagDataDao tagDataDao;
    @Autowired private TaskDao taskDao;
    @Autowired private UpdateDao updateDao;

    private static final String PREF_MIGRATED_TASKS_TO_TAGS = "tasks_to_tags_migration"; //$NON-NLS-1$

    public Astrid44SyncMigrator() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @SuppressWarnings("nls")
    public void performMigration() {
        if (Preferences.getBoolean(PREF_MIGRATED_TASKS_TO_TAGS, false))
            return;

        // --------------
        // First ensure that a TagData object exists for each tag metadata
        // --------------
        Query noTagDataQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataCriteria.withKey(TagMetadata.KEY),
                Criterion.not(TagMetadata.TAG_NAME.in(Query.select(TagData.NAME).from(TagData.TABLE))))).groupBy(TagMetadata.TAG_NAME);

        TodorooCursor<Metadata> noTagData = metadataService.query(noTagDataQuery);
        try {
            Metadata tag = new Metadata();
            for (noTagData.moveToFirst(); !noTagData.isAfterLast(); noTagData.moveToNext()) {
                tag.readFromCursor(noTagData);

                if (Constants.DEBUG)
                    Log.w("tag-link-migrate", "CREATING TAG DATA " + tag.getValue(TagMetadata.TAG_NAME));

                TagData newTagData = new TagData();
                newTagData.setValue(TagData.NAME, tag.getValue(TagMetadata.TAG_NAME));
                tagDataService.save(newTagData);
            }
        } finally {
            noTagData.close();
        }

        // --------------
        // Then ensure that every remote model has a remote id, by generating one using the uuid generator for all those without one
        // --------------
        Query tagsWithoutUUIDQuery = Query.select(TagData.ID, TagData.REMOTE_ID).where(Criterion.or(TagData.REMOTE_ID.isNull(), TagData.REMOTE_ID.eq(0)));
        assertUUIDsExist(tagsWithoutUUIDQuery, new TagData(), tagDataDao);

        Query tasksWithoutUUIDQuery = Query.select(Task.ID, Task.REMOTE_ID).where(Criterion.or(Task.REMOTE_ID.isNull(), Task.REMOTE_ID.eq(0)));
        assertUUIDsExist(tasksWithoutUUIDQuery, new Task(), taskDao);

        Query updatesWithoutUUIDQuery = Query.select(Update.ID, Update.REMOTE_ID).where(Criterion.or(Update.REMOTE_ID.isNull(), Update.REMOTE_ID.eq(0)));
        assertUUIDsExist(updatesWithoutUUIDQuery, new Update(), updateDao);


        // --------------
        // Finally, ensure that all tag metadata entities have all important fields filled in
        // --------------
        Query incompleteQuery = Query.select(Metadata.PROPERTIES).where(Criterion.and(
                MetadataCriteria.withKey(TagMetadata.KEY),
                Criterion.or(TagMetadata.TASK_UUID.eq(0), TagMetadata.TASK_UUID.isNull(),
                        TagMetadata.TAG_UUID.eq(0), TagMetadata.TAG_UUID.isNull())));
        TodorooCursor<Metadata> incompleteMetadata = metadataService.query(incompleteQuery);
        try {
            Metadata m = new Metadata();
            for (incompleteMetadata.moveToFirst(); !incompleteMetadata.isAfterLast(); incompleteMetadata.moveToNext()) {
                m.readFromCursor(incompleteMetadata);
                boolean changes = false;

                if (!m.containsNonNullValue(TagMetadata.TASK_UUID) || m.getValue(TagMetadata.TASK_UUID) == 0) {
                    updateTaskUuid(m);
                    changes = true;
                }

                if (!m.containsNonNullValue(TagMetadata.TAG_UUID) || m.getValue(TagMetadata.TAG_UUID) == 0) {
                    updateTagUuid(m);
                    changes = true;
                }

                if (changes)
                    metadataService.save(m);

            }
        } finally {
            incompleteMetadata.close();
        }



        Preferences.setBoolean(PREF_MIGRATED_TASKS_TO_TAGS, true);
    }

    private <TYPE extends RemoteModel> void assertUUIDsExist(Query query, TYPE instance, DatabaseDao<TYPE> dao) {
        TodorooCursor<TYPE> cursor = dao.query(query);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                instance.readPropertiesFromCursor(cursor);
                Pair<Long, String> uuidPair = UUIDHelper.newUUID();
                instance.setValue(RemoteModel.REMOTE_ID_PROPERTY, uuidPair.getLeft());
                instance.setValue(RemoteModel.PROOF_TEXT_PROPERTY, uuidPair.getRight());
                dao.saveExisting(instance);
            }
        } finally {
            cursor.close();
        }
    }

    private void updateTaskUuid(Metadata m) {
        long taskId = m.getValue(Metadata.TASK);
        Task task = taskDao.fetch(taskId, Task.REMOTE_ID);
        if (task != null) {
            m.setValue(TagMetadata.TASK_UUID, task.getValue(Task.REMOTE_ID));
        } else {
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

    private void updateTagUuid(Metadata m) {
        String tag = m.getValue(TagMetadata.TAG_NAME);
        TagData tagData = tagDataService.getTag(tag, TagData.REMOTE_ID);
        if (tagData != null) {
            m.setValue(TagMetadata.TAG_UUID, tagData.getValue(TagData.REMOTE_ID));
        } else {
            m.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        }
    }

}
