package com.todoroo.astrid.sync;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.ReplayOutstandingEntries;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
import com.todoroo.astrid.actfm.sync.messages.UserMigrated;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;


public class SyncMessageTest extends NewSyncTestCase {

	public void testTaskChangesHappenedConstructor() {
		Task t = createTask();
		try {
			ChangesHappened<?, ?> changes = ChangesHappened.instantiateChangesHappened(t.getId(), ModelType.TYPE_TASK);
			assertNotNull(changes.serializeToJSON(null));
			assertTrue(changes.getChanges().size() > 0);
			assertFalse(RemoteModel.NO_UUID.equals(changes.getUUID()));
			assertEquals(t.getValue(Task.UUID), changes.getUUID());
		} catch (Exception e) {
			fail("ChangesHappened constructor threw exception " + e);
		}
	}

	private static final String MAKE_CHANGES_TITLE = "Made changes to title";
	private JSONObject getMakeChanges(String... extraChanges) throws JSONException {
		JSONObject makeChanges = new JSONObject();
		makeChanges.put("type", ServerToClientMessage.TYPE_MAKE_CHANGES);
		makeChanges.put("table", NameMaps.TABLE_ID_TASKS);

		JSONObject changes = new JSONObject();
		changes.put("title", MAKE_CHANGES_TITLE);
		changes.put("importance", Task.IMPORTANCE_DO_OR_DIE);
		if (extraChanges != null) {
			for (int i = 0; i < extraChanges.length - 1; i+=2) {
				String key = extraChanges[i];
				String value = extraChanges[i + 1];
				changes.put(key, value);
			}
		}

		makeChanges.put("changes", changes);
		return makeChanges;
	}

	public void testMakeChangesMakesChanges() {
		Task t = createTask();
		try {
			JSONObject makeChanges = getMakeChanges();
			makeChanges.put("uuid", t.getValue(Task.UUID));

			ServerToClientMessage message = ServerToClientMessage.instantiateMessage(makeChanges);
			message.processMessage(null);

			t = taskDao.fetch(t.getId(), Task.TITLE, Task.IMPORTANCE);
			assertEquals(MAKE_CHANGES_TITLE, t.getValue(Task.TITLE));
			assertEquals(Task.IMPORTANCE_DO_OR_DIE, t.getValue(Task.IMPORTANCE).intValue());
		} catch (JSONException e) {
			e.printStackTrace();
			fail("JSONException");
		}
	}

	public void testMakeChangesMakesNewTasks() {
		try {
			JSONObject makeChanges = getMakeChanges();
			makeChanges.put("uuid", "1");
			ServerToClientMessage message = ServerToClientMessage.instantiateMessage(makeChanges);
			message.processMessage(null);

			TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID, Task.UUID, Task.TITLE, Task.IMPORTANCE).where(Task.UUID.eq("1")));
			try {
				assertEquals(1, cursor.getCount());
				cursor.moveToFirst();
				Task t = new Task(cursor);

				assertEquals(MAKE_CHANGES_TITLE, t.getValue(Task.TITLE));
				assertEquals(Task.IMPORTANCE_DO_OR_DIE, t.getValue(Task.IMPORTANCE).intValue());
				assertEquals("1", t.getValue(Task.UUID));
			} finally {
				cursor.close();
			}

		} catch (JSONException e) {
			e.printStackTrace();
			fail("JSONException");
		}
	}

	public void testMakeChangesWithUuidCollision() {
		Task t = createTask();
		String oldUuid = t.getUuid();
		try {
			String newUuid = oldUuid + "blorp";
			JSONObject makeChanges = getMakeChanges("uuid", newUuid);
			makeChanges.put("uuid", oldUuid);
			ServerToClientMessage message = ServerToClientMessage.instantiateMessage(makeChanges);
			message.processMessage(null);

			TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.UUID).where(Task.UUID.eq(newUuid)));
			try {
				assertEquals(1, cursor.getCount());
			} finally {
				cursor.close();
			}

			cursor = taskDao.query(Query.select(Task.UUID).where(Task.UUID.eq(oldUuid)));
			try {
				assertEquals(0, cursor.getCount());
			} finally {
				cursor.close();
			}
		} catch (JSONException e) {
			e.printStackTrace();
			fail("JSONException");
		}
	}

	public void testReplayOutstandingEntries() {
		Task t = createTask();

		t.setValue(Task.TITLE, "change title");
		t.setValue(Task.IMPORTANCE, Task.IMPORTANCE_NONE);
		t.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		taskDao.save(t);

		new ReplayOutstandingEntries<Task, TaskOutstanding>(Task.class, NameMaps.TABLE_ID_TASKS, taskDao, taskOutstandingDao, false).execute();

		t = taskDao.fetch(t.getId(), Task.TITLE, Task.IMPORTANCE);
		assertEquals(SYNC_TASK_TITLE, t.getValue(Task.TITLE));
		assertEquals(SYNC_TASK_IMPORTANCE, t.getValue(Task.IMPORTANCE).intValue());
	}

	public void testUserMigratedUpdatesUserIds() {
	    Task t = createTask("Someone else's", true);
	    TagData td = createTagData("Someone else's tag", true);

	    t.setValue(Task.USER_ID, "1");
	    taskDao.save(t);
	    td.setValue(TagData.USER_ID, "1");
	    tagDataDao.saveExisting(td);

	    JSONObject userMigrated = new JSONObject();
	    try {
	        userMigrated.put("type", ServerToClientMessage.TYPE_USER_MIGRATED);
	        userMigrated.put("new_user_id", "1");
	    } catch (JSONException e) {
	        fail("JSONException");
	    }

	    new UserMigrated(userMigrated).processMessage(null);

	    t = taskDao.fetch(t.getId(), Task.USER_ID);
	    td = tagDataDao.fetch(td.getId(), TagData.USER_ID);
	    assertEquals("1", ActFmPreferenceService.userId());
	    assertEquals(Task.USER_ID_SELF, t.getValue(Task.USER_ID));
	    assertEquals(Task.USER_ID_SELF, td.getValue(TagData.USER_ID));
	}

}
