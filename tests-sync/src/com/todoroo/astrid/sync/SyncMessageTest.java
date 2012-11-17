package com.todoroo.astrid.sync;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.todoroo.astrid.actfm.sync.ActFmSyncThread.ModelType;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;

public class SyncMessageTest extends NewSyncTestCase {
	
	public void testTaskChangesHappenedConstructor() {
		Task t = createTask();
		try {
			ChangesHappened<?, ?> changes = ChangesHappened.instantiateChangesHappened(t.getId(), ModelType.TYPE_TASK);
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
		makeChanges.put("table", NameMaps.TABLE_ID_TASKS);
		JSONArray changes = new JSONArray();
		
		JSONArray change1 = new JSONArray();
		change1.put("title"); 
		change1.put(MAKE_CHANGES_TITLE);
		
		JSONArray change2 = new JSONArray();
		change2.put("importance");
		change2.put(Task.IMPORTANCE_DO_OR_DIE);
		
		changes.put(change1);
		changes.put(change2);
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
