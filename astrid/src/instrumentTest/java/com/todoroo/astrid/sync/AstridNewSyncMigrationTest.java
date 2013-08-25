package com.todoroo.astrid.sync;

import java.util.ArrayList;

import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.AstridNewSyncMigrator;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TaskToTagMetadata;

public class AstridNewSyncMigrationTest extends NewSyncTestCase {

	private static final LongProperty TASK_REMOTE_ID_FIELD = new LongProperty(Task.TABLE, RemoteModel.UUID_PROPERTY_NAME);
	private static final LongProperty TAG_REMOTE_ID_FIELD = new LongProperty(TagData.TABLE, RemoteModel.UUID_PROPERTY_NAME);
	
	@Autowired
	private MetadataDao metadataDao;
	
	public void testAstridSyncMigration() {
		setupOldDatabase();
		Preferences.setBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
		new AstridNewSyncMigrator().performMigration();
		assertAllModelsHaveUUID();
		assertAllTagsHaveTagData();
		assertAllMetadataHasAllFields();
	}
	
	private void setupOldDatabase() {
		// Init 5 unsynced tasks and tags
		for (int i = 1; i <= 5; i++) {
			Task task = createTask("Task " + i, true);
			task.setValue(TASK_REMOTE_ID_FIELD, null);
			taskDao.save(task);
			
			TagData tag = createTagData("Tag " + i, true);
			tag.setValue(TAG_REMOTE_ID_FIELD, null);
			tagDataDao.saveExisting(tag);
		}
		
		Metadata m = new Metadata();
		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 1L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "Tag 1");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		m.clear();
		
		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 2L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "New tag");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		m.clear();
		
		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 3L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "Tag 3");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 3L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "Tag 4");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "Tag 1");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "Tag 5");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);

		m.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TaskToTagMetadata.TAG_NAME, "New tag 2");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		
		// State (task: tags)
		// Task 1: Tag 1
		// Task 2: New tag
		// Task 3: Tag 3, Tag 4
		// Task 4: (nothing)
		// Task 5: Tag 5, New tag 2
	}
	
	private void assertAllModelsHaveUUID() {
		assertUUIDs(Task.TABLE, new Task(), taskDao, Task.ID, Task.UUID);
		assertUUIDs(TagData.TABLE, new TagData(), tagDataDao, TagData.ID, TagData.UUID);
	}
	
	private <TYPE extends RemoteModel> void assertUUIDs(Table table, TYPE instance, RemoteModelDao<TYPE> dao, Property<?>... properties) {
		TodorooCursor<TYPE> cursor = dao.query(Query.select(properties).from(table));
		try {
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				instance.clear();
				instance.readPropertiesFromCursor(cursor);
				String uuid = instance.getValue(RemoteModel.UUID_PROPERTY);
				if (uuid == null || RemoteModel.NO_UUID.equals(uuid)) {
					fail(instance.getClass().getName() + " " + instance.getId() + " didn't have a uuid");
				}
			}
		} finally {
			cursor.close();
		}
	}
	
	private void assertAllTagsHaveTagData() {
		ArrayList<String> names = new ArrayList<String>();
 		for (int i = 1; i <= 5; i++) {
			String name = "Tag " + i;
			names.add(name);
		}
		names.add("New tag");
		names.add("New tag 2");
		
		String[] namesArray = names.toArray(new String[names.size()]);
		TodorooCursor<TagData> tagData = tagDataDao.query(Query.select(TagData.NAME).where(TagData.NAME.in(namesArray)));
		try {
			assertEquals(namesArray.length, tagData.getCount());
		} finally {
			tagData.close();
		}
		
		
	}
	
	private void assertAllMetadataHasAllFields() {
		TodorooCursor<Metadata> tagMetadata = metadataDao.query(Query.select(Metadata.PROPERTIES)
				.where(MetadataCriteria.withKey(TaskToTagMetadata.KEY)));
		try {
			assertEquals(tagMetadata.getCount(), 7);
			Metadata m = new Metadata();
			for (tagMetadata.moveToFirst(); !tagMetadata.isAfterLast(); tagMetadata.moveToNext()) {
				m.readFromCursor(tagMetadata);
				assertTrue(!TextUtils.isEmpty(m.getValue(TaskToTagMetadata.TAG_NAME)));
				assertTrue(!RemoteModel.NO_UUID.equals(m.getValue(TaskToTagMetadata.TASK_UUID)));
				assertTrue(!RemoteModel.NO_UUID.equals(m.getValue(TaskToTagMetadata.TAG_UUID)));
			}
		} finally {
			tagMetadata.close();
		}
	}
	
}
