package com.todoroo.astrid.sync;

import java.math.BigInteger;

import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.Astrid44SyncMigrator;
import com.todoroo.astrid.tags.TagMetadata;

public class Astrid44MigrationTest extends NewSyncTestCase {

	@Autowired
	private MetadataDao metadataDao;
	
	public void testAstrid44Migration() {
		setupOldDatabase();
		new Astrid44SyncMigrator().performMigration();
		assertAllModelsHaveUUID();
		assertAllTagsHaveTagData();
		assertAllMetadataHasAllFields();
	}
	
	private void setupOldDatabase() {
		// Init 5 unsynced tasks and tags
		for (int i = 1; i <= 5; i++) {
			Task task = createTask("Task " + i);
			task.setValue(Task.REMOTE_ID, null);
			taskDao.save(task);
			
			TagData tag = createTagData("Tag " + i);
			tag.setValue(TagData.REMOTE_ID, null);
			tagDataDao.saveExisting(tag);
		}
		
		Metadata m = new Metadata();
		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 1L);
		m.setValue(TagMetadata.TAG_NAME, "Tag 1");
		metadataDao.createNew(m);
		m.clear();
		
		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 2L);
		m.setValue(TagMetadata.TAG_NAME, "New tag");
		metadataDao.createNew(m);
		m.clear();
		
		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 3L);
		m.setValue(TagMetadata.TAG_NAME, "Tag 3");
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 3L);
		m.setValue(TagMetadata.TAG_NAME, "Tag 4");
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TagMetadata.TAG_NAME, "Tag 1");
		metadataDao.createNew(m);
		m.clear();

		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TagMetadata.TAG_NAME, "Tag 5");
		metadataDao.createNew(m);

		m.setValue(Metadata.KEY, TagMetadata.KEY);
		m.setValue(Metadata.TASK, 5L);
		m.setValue(TagMetadata.TAG_NAME, "New tag 2");
		metadataDao.createNew(m);
		
		// State (task: tags)
		// Task 1: Tag 1
		// Task 2: New tag
		// Task 3: Tag 3, Tag 4
		// Task 4: (nothing)
		// Task 5: Tag 5, New tag 2
	}
	
	private void assertAllModelsHaveUUID() {
		assertRemoteIds(Task.TABLE, new Task(), taskDao, Task.ID, Task.UUID);
		assertRemoteIds(TagData.TABLE, new TagData(), tagDataDao, TagData.ID, TagData.UUID);
	}
	
	private <TYPE extends RemoteModel> void assertRemoteIds(Table table, TYPE instance, RemoteModelDao<TYPE> dao, Property<?>... properties) {
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
		for (int i = 1; i <= 5; i++) {
			String name = "Tag " + i;
			assertTagForName(name);
		}
		assertTagForName("New tag");
		assertTagForName("New tag 2");
	}
	
	private void assertTagForName(String name) {
		TodorooCursor<TagData> tagData = tagDataDao.query(Query.select(TagData.NAME).where(TagData.NAME.eq(name)));
		try {
			assertEquals(tagData.getCount(), 1);
		} finally {
			tagData.close();
		}
	}
	
	private void assertAllMetadataHasAllFields() {
		TodorooCursor<Metadata> tagMetadata = metadataDao.query(Query.select(Metadata.PROPERTIES)
				.where(MetadataCriteria.withKey(TagMetadata.KEY)));
		try {
			assertEquals(tagMetadata.getCount(), 7);
			Metadata m = new Metadata();
			for (tagMetadata.moveToFirst(); !tagMetadata.isAfterLast(); tagMetadata.moveToNext()) {
				m.readFromCursor(tagMetadata);
				assertTrue(!TextUtils.isEmpty(m.getValue(TagMetadata.TAG_NAME)));
				assertTrue(!RemoteModel.NO_UUID.equals(m.getValue(TagMetadata.TASK_UUID)));
				assertTrue(!RemoteModel.NO_UUID.equals(m.getValue(TagMetadata.TAG_UUID)));
			}
		} finally {
			tagMetadata.close();
		}
	}
	
}
