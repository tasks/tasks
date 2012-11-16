package com.todoroo.astrid.sync;

import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;

public class SyncMessageTest extends NewSyncTestCase {
	
	public void testTaskChangesHappenedConstructor() {
		Task t = createTask();
		try {
			ChangesHappened<?, ?> changes = ClientToServerMessage.instantiateChangesHappened(t.getId(), ModelType.TYPE_TASK);
			assertTrue(changes.numChanges() > 0);
			assertFalse(RemoteModel.NO_UUID.equals(changes.getUUID()));
			assertEquals(t.getValue(Task.UUID), changes.getUUID());
		} catch (Exception e) {
			fail("ChangesHappened constructor threw exception " + e);
		}
	}
	
	private static final String MAKE_CHANGES_TITLE = "Made changes to title";
	private JSONObject getMakeChanges() throws JSONException {
		JSONObject makeChanges = new JSONObject();
		makeChanges.put("type", ServerToClientMessage.TYPE_MAKE_CHANGES);
		makeChanges.put("table", NameMaps.SERVER_TABLE_TASKS);
		JSONObject changes = new JSONObject();
		changes.put("title", MAKE_CHANGES_TITLE);
		changes.put("importance", Task.IMPORTANCE_DO_OR_DIE);
		makeChanges.put("changes", changes);
		return makeChanges;
	}
	
	public void testMakeChangesMakesChanges() {
		Task t = createTask();
		try {
			JSONObject makeChanges = getMakeChanges();
			makeChanges.put("uuid", t.getValue(Task.UUID));
			
			ServerToClientMessage message = ServerToClientMessage.instantiateMessage(makeChanges);
			message.processMessage();
			
			t = taskDao.fetch(t.getId(), Task.TITLE, Task.IMPORTANCE);
			assertEquals(MAKE_CHANGES_TITLE, t.getValue(Task.TITLE));
			assertEquals(Task.IMPORTANCE_DO_OR_DIE, t.getValue(Task.IMPORTANCE).intValue());
		} catch (JSONException e) {
			e.printStackTrace();
			fail("JSONException");
		}
	}
	
}
