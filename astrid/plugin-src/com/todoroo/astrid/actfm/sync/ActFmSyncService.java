/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.DatabaseDao.ModelUpdateListener;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.MetadataApiDao.MetadataCriteria;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.Flags;

/**
 * Service for synchronizing data on Astrid.com server with local.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class ActFmSyncService {

    // --- instance variables

    @Autowired TagDataService tagDataService;
    @Autowired MetadataService metadataService;
    @Autowired TaskService taskService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired ActFmInvoker actFmInvoker;
    @Autowired ActFmDataService actFmDataService;
    @Autowired TaskDao taskDao;
    @Autowired TagDataDao tagDataDao;
    @Autowired UpdateDao updateDao;
    @Autowired MetadataDao metadataDao;

    private String token;

    public ActFmSyncService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void initialize() {
        taskDao.addListener(new ModelUpdateListener<Task>() {
            @Override
            public void onModelUpdated(final Task model) {
                if(Flags.checkAndClear(Flags.ACTFM_SUPPRESS_SYNC))
                    return;
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken() || setValues.containsKey(RemoteModel.REMOTE_ID_PROPERTY_NAME))
                    return;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // sleep so metadata associated with task is saved
                        AndroidUtilities.sleepDeep(1000L);
                        pushTaskOnSave(model, setValues);
                    }
                }).start();
            }
        });

        updateDao.addListener(new ModelUpdateListener<Update>() {
            @Override
            public void onModelUpdated(final Update model) {
                if(Flags.checkAndClear(Flags.ACTFM_SUPPRESS_SYNC))
                    return;
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken() || model.getValue(Update.REMOTE_ID) > 0)
                    return;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pushUpdateOnSave(model, setValues);
                    }
                }).start();
            }
        });

        tagDataDao.addListener(new ModelUpdateListener<TagData>() {
            @Override
            public void onModelUpdated(final TagData model) {
                if(Flags.checkAndClear(Flags.ACTFM_SUPPRESS_SYNC))
                    return;
                final ContentValues setValues = model.getSetValues();
                if(setValues == null || !checkForToken() || setValues.containsKey(RemoteModel.REMOTE_ID_PROPERTY_NAME))
                    return;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pushTagDataOnSave(model, setValues);
                    }
                }).start();
            }
        });
    }

    // --- data push methods

    /**
     * Synchronize with server when data changes
     */
    public void pushUpdateOnSave(Update update, ContentValues values) {
        if(!values.containsKey(Update.MESSAGE.name))
            return;

        ArrayList<Object> params = new ArrayList<Object>();
        params.add("message"); params.add(update.getValue(Update.MESSAGE));

        if(update.getValue(Update.TAGS).length() > 0) {
            String tagId = update.getValue(Update.TAGS);
            tagId = tagId.substring(1, tagId.indexOf(',', 1));
            params.add("tag_id"); params.add(tagId);
        }

        if(update.getValue(Update.TASK) > 0) {
            params.add("task_id"); params.add(update.getValue(Update.TASK));
        }
        if(!checkForToken())
            return;

        try {
            params.add("token"); params.add(token);
            JSONObject result = actFmInvoker.invoke("comment_add", params.toArray(new Object[params.size()]));
            update.setValue(Update.REMOTE_ID, result.optLong("id"));
            updateDao.saveExisting(update);
        } catch (IOException e) {
            handleException("task-save", e);
        }
    }

    /**
     * Synchronize with server when data changes
     */
    public void pushTaskOnSave(Task task, ContentValues values) {
        long remoteId;
        if(task.containsValue(Task.REMOTE_ID))
            remoteId = task.getValue(Task.REMOTE_ID);
        else {
            Task taskForRemote = taskService.fetchById(task.getId(), Task.REMOTE_ID);
            if(taskForRemote == null)
                return;
            remoteId = taskForRemote.getValue(Task.REMOTE_ID);
        }
        boolean newlyCreated = remoteId == 0;

        ArrayList<Object> params = new ArrayList<Object>();

        if(values.containsKey(Task.TITLE.name)) {
            params.add("title"); params.add(task.getValue(Task.TITLE));
        }
        if(values.containsKey(Task.DUE_DATE.name)) {
            params.add("due"); params.add(task.getValue(Task.DUE_DATE) / 1000L);
            params.add("has_due_time"); params.add(task.hasDueTime() ? 1 : 0);
        }
        if(values.containsKey(Task.NOTES.name)) {
            params.add("notes"); params.add(task.getValue(Task.NOTES));
        }
        if(values.containsKey(Task.DELETION_DATE.name)) {
            params.add("deleted_at"); params.add(task.getValue(Task.DELETION_DATE) / 1000L);
        }
        if(values.containsKey(Task.COMPLETION_DATE.name)) {
            params.add("completed"); params.add(task.getValue(Task.COMPLETION_DATE) / 1000L);
        }
        if(values.containsKey(Task.IMPORTANCE.name)) {
            params.add("importance"); params.add(task.getValue(Task.IMPORTANCE));
        }
        if(values.containsKey(Task.RECURRENCE.name)) {
            params.add("repeat"); params.add(task.getValue(Task.RECURRENCE));
        }
        if(values.containsKey(Task.USER_ID.name) && task.getValue(Task.USER_ID) >= 0) {
            params.add("user_id");
            if(task.getValue(Task.USER_ID) == 0)
                params.add(ActFmPreferenceService.userId());
            else
                params.add(task.getValue(Task.USER_ID));
        }
        if(Flags.checkAndClear(Flags.TAGS_CHANGED) || newlyCreated) {
            TodorooCursor<Metadata> cursor = TagService.getInstance().getTags(task.getId());
            try {
                if(cursor.getCount() == 0) {
                    params.add("tags");
                    params.add("");
                } else {
                    Metadata metadata = new Metadata();
                    for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        metadata.readFromCursor(cursor);
                        if(metadata.containsNonNullValue(TagService.REMOTE_ID) &&
                                metadata.getValue(TagService.REMOTE_ID) > 0) {
                            params.add("tag_ids[]");
                            params.add(metadata.getValue(TagService.REMOTE_ID));
                        } else {
                            params.add("tags[]");
                            params.add(metadata.getValue(TagService.TAG));
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if(params.size() == 0 || !checkForToken())
            return;

        System.err.println("PUSHN ON SAVE: " + task.getMergedValues());
        System.err.println("SETVALUES: " + values);

        if(!newlyCreated) {
            params.add("id"); params.add(remoteId);
        } else if(!params.contains(Task.TITLE.name))
            return;

        try {
            params.add("token"); params.add(token);
            JSONObject result = actFmInvoker.invoke("task_save", params.toArray(new Object[params.size()]));
            ArrayList<Metadata> metadata = new ArrayList<Metadata>();
            JsonHelper.taskFromJson(result, task, metadata);
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
            task.setValue(Task.LAST_SYNC, DateUtilities.now());
            Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
            taskDao.saveExisting(task);
        } catch (JSONException e) {
            handleException("task-save-json", e);
        } catch (IOException e) {
            handleException("task-save-io", e);
        }
    }

    /**
     * Synchronize complete task with server
     * @param task
     */
    public void pushTask(long taskId) {
        Task task = taskService.fetchById(taskId, Task.PROPERTIES);
        pushTaskOnSave(task, task.getMergedValues());
    }

    /**
     * Send tagData changes to server
     * @param setValues
     */
    public void pushTagDataOnSave(TagData tagData, ContentValues values) {
        long remoteId;
        if(tagData.containsValue(TagData.REMOTE_ID))
            remoteId = tagData.getValue(TagData.REMOTE_ID);
        else {
            TagData forRemote = tagDataService.fetchById(tagData.getId(), TagData.REMOTE_ID);
            if(forRemote == null)
                return;
            remoteId = forRemote.getValue(TagData.REMOTE_ID);
        }
        boolean newlyCreated = remoteId == 0;

        ArrayList<Object> params = new ArrayList<Object>();

        if(values.containsKey(TagData.NAME.name)) {
            params.add("name"); params.add(tagData.getValue(TagData.NAME));
        }

        if(values.containsKey(TagData.DELETION_DATE.name)) {
            params.add("deleted_at"); params.add(tagData.getValue(TagData.DELETION_DATE));
        }

        if(values.containsKey(TagData.MEMBERS.name)) {
            params.add("members");
            try {
                JSONArray members = new JSONArray(tagData.getValue(TagData.MEMBERS));
                if(members.length() == 0)
                    params.add("");
                else {
                    ArrayList<Object> array = new ArrayList<Object>(members.length());
                    for(int i = 0; i < members.length(); i++) {
                        JSONObject person = members.getJSONObject(i);
                        if(person.has("id"))
                            array.add(person.getLong("id"));
                        else {
                            if(person.has("name"))
                                array.add(person.getString("name") + " <" +
                                        person.getString("email") + ">");
                            else
                                array.add(person.getString("email"));
                        }
                    }
                    params.add(array);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        if(values.containsKey(TagData.FLAGS.name)) {
            params.add("is_silent");
            boolean silenced = tagData.getFlag(TagData.FLAGS, TagData.FLAG_SILENT);
            params.add(silenced ? "1" : "0");
        }

        if(params.size() == 0 || !checkForToken())
            return;

        if(!newlyCreated) {
            params.add("id"); params.add(remoteId);
        }

        boolean success;
        try {
            params.add("token"); params.add(token);
            JSONObject result = actFmInvoker.invoke("tag_save", params.toArray(new Object[params.size()]));
            if(newlyCreated) {
                tagData.setValue(TagData.REMOTE_ID, result.optLong("id"));
                Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                tagDataDao.saveExisting(tagData);
            }
            success = true;
        } catch (IOException e) {
            handleException("tag-save", e);
            success = false;
        }
        if(!Flags.checkAndClear(Flags.TOAST_ON_SAVE))
            return;

        final boolean finalSuccess = success;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(finalSuccess)
                    Toast.makeText(ContextManager.getContext(),
                            R.string.actfm_toast_success, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(ContextManager.getContext(),
                            R.string.actfm_toast_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- data fetch methods

    /**
     * Fetch tagData listing asynchronously
     */
    public void fetchTagDataDashboard(boolean manual, final Runnable done) {
        invokeFetchList("goal", manual, new ListItemProcessor<TagData>() {
            @Override
            protected void mergeAndSave(JSONArray list, HashMap<Long,Long> locals) throws JSONException {
                TagData remote = new TagData();
                for(int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    readIds(locals, item, remote);
                    JsonHelper.tagFromJson(item, remote);
                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    tagDataService.save(remote);
                }
            }

            @Override
            protected HashMap<Long, Long> getLocalModels() {
                TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.ID,
                        TagData.REMOTE_ID).where(TagData.REMOTE_ID.in(remoteIds)).orderBy(
                                Order.asc(TagData.REMOTE_ID)));
                return cursorToMap(cursor, taskDao, TagData.REMOTE_ID, TagData.ID);
            }
        }, done, "goals");
    }

    /**
     * Get details for this tag
     * @param tagData
     * @throws IOException
     * @throws JSONException
     */
    public void fetchTag(final TagData tagData) throws IOException, JSONException {
        JSONObject result;
        if(!checkForToken())
            return;

        if(tagData.getValue(TagData.REMOTE_ID) == 0) {
            if(TextUtils.isEmpty(tagData.getValue(TagData.NAME)))
                return;
            result = actFmInvoker.invoke("tag_show", "name", tagData.getValue(TagData.NAME),
                    "token", token);
        } else
            result = actFmInvoker.invoke("tag_show", "id", tagData.getValue(TagData.REMOTE_ID),
                    "token", token);

        JsonHelper.tagFromJson(result, tagData);
        Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
        tagDataService.save(tagData);
    }

    /**
     * Get details for this task
     * @param task
     * @throws IOException
     * @throws JSONException
     */
    public void fetchTask(Task task) throws IOException, JSONException {
        JSONObject result;
        if(!checkForToken())
            return;

        if(task.getValue(TagData.REMOTE_ID) == 0)
            return;
        result = actFmInvoker.invoke("task_show", "id", task.getValue(Task.REMOTE_ID),
                    "token", token);

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        JsonHelper.taskFromJson(result, task, metadata);
        Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
        taskService.save(task);
        metadataService.synchronizeMetadata(task.getId(), metadata, Metadata.KEY.eq(TagService.KEY));
    }

    /**
     * Fetch all tags
     */
    public void fetchTags() throws JSONException, IOException {
        if(!checkForToken())
            return;

        JSONObject result = actFmInvoker.invoke("tag_list", "token", token);
        JSONArray tags = result.getJSONArray("list");
        HashSet<Long> remoteIds = new HashSet<Long>(tags.length());
        for(int i = 0; i < tags.length(); i++) {
            JSONObject tagObject = tags.getJSONObject(i);
            actFmDataService.saveTagData(tagObject);
            remoteIds.add(tagObject.getLong("id"));
        }

        Long[] remoteIdArray = remoteIds.toArray(new Long[remoteIds.size()]);
        tagDataService.deleteWhere(Criterion.not(TagData.REMOTE_ID.in(remoteIdArray)));
    }

    /**
     * Fetch tasks for the given tagData asynchronously
     * @param tagData
     * @param manual
     * @param done
     */
    public void fetchTasksForTag(final TagData tagData, final boolean manual, Runnable done) {
        invokeFetchList("task", manual, new ListItemProcessor<Task>() {
            @Override
            protected void mergeAndSave(JSONArray list, HashMap<Long,Long> locals) throws JSONException {
                Task remote = new Task();

                ArrayList<Metadata> metadata = new ArrayList<Metadata>();
                for(int i = 0; i < list.length(); i++) {

                    JSONObject item = list.getJSONObject(i);
                    readIds(locals, item, remote);
                    JsonHelper.taskFromJson(item, remote, metadata);

                    if(remote.getValue(Task.USER_ID) == 0) {
                        if(!remote.isSaved())
                            StatisticsService.reportEvent("actfm-task-created");
                        else if(remote.isCompleted())
                            StatisticsService.reportEvent("actfm-task-completed");
                    }


                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    taskService.save(remote);
                    metadataService.synchronizeMetadata(remote.getId(), metadata, MetadataCriteria.withKey(TagService.KEY));
                    remote.clear();
                }

                if(manual) {
                    for(Long localId : locals.values())
                        taskDao.delete(localId);
                }
            }

            @Override
            protected HashMap<Long, Long> getLocalModels() {
                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID,
                        Task.REMOTE_ID).where(Task.REMOTE_ID.in(remoteIds)).orderBy(
                                Order.asc(Task.REMOTE_ID)));
                return cursorToMap(cursor, taskDao, Task.REMOTE_ID, Task.ID);
            }
        }, done, "tasks:" + tagData.getId(), "tag_id", tagData.getValue(TagData.REMOTE_ID));
    }

    /**
     * Fetch updates for the given tagData asynchronously
     * @param tagData
     * @param manual
     * @param done
     */
    public void fetchUpdatesForTag(final TagData tagData, final boolean manual, Runnable done) {
        invokeFetchList("activity", manual, new ListItemProcessor<Update>() {
            @Override
            protected void mergeAndSave(JSONArray list, HashMap<Long,Long> locals) throws JSONException {
                Update remote = new Update();
                for(int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    readIds(locals, item, remote);
                    JsonHelper.updateFromJson(item, remote);

                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    if(remote.getId() == AbstractModel.NO_ID)
                        updateDao.createNew(remote);
                    else
                        updateDao.saveExisting(remote);
                    remote.clear();
                }
            }

            @Override
            protected HashMap<Long, Long> getLocalModels() {
                TodorooCursor<Update> cursor = updateDao.query(Query.select(Update.ID,
                        Update.REMOTE_ID).where(Update.REMOTE_ID.in(remoteIds)).orderBy(
                                Order.asc(Update.REMOTE_ID)));
                return cursorToMap(cursor, updateDao, Update.REMOTE_ID, Update.ID);
            }
        }, done, "updates:" + tagData.getId(), "tag_id", tagData.getValue(TagData.REMOTE_ID));
    }

    /**
     * Fetch updates for the given task asynchronously
     * @param task
     * @param manual
     * @param runnable
     */
    public void fetchUpdatesForTask(final Task task, boolean manual, Runnable done) {
        invokeFetchList("activity", manual, new ListItemProcessor<Update>() {
            @Override
            protected void mergeAndSave(JSONArray list, HashMap<Long,Long> locals) throws JSONException {
                Update remote = new Update();
                for(int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    readIds(locals, item, remote);
                    JsonHelper.updateFromJson(item, remote);

                    Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
                    if(remote.getId() == AbstractModel.NO_ID)
                        updateDao.createNew(remote);
                    else
                        updateDao.saveExisting(remote);
                    remote.clear();
                }
            }

            @Override
            protected HashMap<Long, Long> getLocalModels() {
                TodorooCursor<Update> cursor = updateDao.query(Query.select(Update.ID,
                        Update.REMOTE_ID).where(Update.REMOTE_ID.in(remoteIds)).orderBy(
                                Order.asc(Update.REMOTE_ID)));
                return cursorToMap(cursor, updateDao, Update.REMOTE_ID, Update.ID);
            }
        }, done, "comments:" + task.getId(), "task_id", task.getValue(Task.REMOTE_ID));
    }

    /**
     * Update tag picture
     * @param path
     * @throws IOException
     * @throws ActFmServiceException
     */
    public String setTagPicture(long tagId, Bitmap bitmap) throws ActFmServiceException, IOException {
        if(!checkForToken())
            return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(bitmap.getWidth() > 512 || bitmap.getHeight() > 512) {
            float scale = Math.min(512f / bitmap.getWidth(), 512f / bitmap.getHeight());
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(scale * bitmap.getWidth()),
                    (int)(scale * bitmap.getHeight()), false);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] bytes = baos.toByteArray();
        MultipartEntity data = new MultipartEntity();
        data.addPart("picture", new ByteArrayBody(bytes, "image/jpg", "image.jpg"));
        JSONObject result = actFmInvoker.post("tag_save", data, "id", tagId, "token", token);
        return result.optString("picture");
    }

    // --- generic invokation

    /** invoke authenticated method against the server */
    public JSONObject invoke(String method, Object... getParameters) throws IOException,
            ActFmServiceException {
        if(!checkForToken())
            throw new ActFmServiceException("not logged in");
        Object[] parameters = new Object[getParameters.length + 2];
        parameters[0] = "token";
        parameters[1] = token;
        for(int i = 0; i < getParameters.length; i++)
            parameters[i+2] = getParameters[i];
        return actFmInvoker.invoke(method, parameters);
    }

    // --- helpers

    private abstract class ListItemProcessor<TYPE extends AbstractModel> {
        protected Long[] remoteIds = null;

        abstract protected HashMap<Long, Long> getLocalModels();

        abstract protected void mergeAndSave(JSONArray list,
                HashMap<Long,Long> locals) throws JSONException;

        public void process(JSONArray list) throws JSONException {
            readRemoteIds(list);
            HashMap<Long, Long> locals = getLocalModels();
            mergeAndSave(list, locals);
        }


        protected void readRemoteIds(JSONArray list) throws JSONException {
            remoteIds = new Long[list.length()];
            for(int i = 0; i < list.length(); i++)
                remoteIds[i] = list.getJSONObject(i).getLong("id");
        }

        protected void readIds(HashMap<Long, Long> locals, JSONObject json, RemoteModel model) throws JSONException {
            long remoteId = json.getLong("id");
            model.setValue(RemoteModel.REMOTE_ID_PROPERTY, remoteId);
            if(locals.containsKey(remoteId)) {
                model.setId(locals.remove(remoteId));
            } else {
                model.clearValue(AbstractModel.ID_PROPERTY);
            }
        }

        protected HashMap<Long, Long> cursorToMap(TodorooCursor<TYPE> cursor, DatabaseDao<?> dao,
                LongProperty remoteIdProperty, LongProperty localIdProperty) {
            try {
                HashMap<Long, Long> map = new HashMap<Long, Long>(cursor.getCount());
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    long remoteId = cursor.get(remoteIdProperty);
                    long localId = cursor.get(localIdProperty);

                    if(map.containsKey(remoteId))
                        dao.delete(map.get(remoteId));
                    map.put(remoteId, localId);
                }
                return map;
            } finally {
                cursor.close();
            }
        }

    }

    /** Call sync method */
    private void invokeFetchList(final String model, final boolean manual,
            final ListItemProcessor<?> processor, final Runnable done, final String lastSyncKey,
            Object... params) {
        if(!checkForToken())
            return;

        long serverFetchTime = manual ? 0 : Preferences.getLong("actfm_time_" + lastSyncKey, 0);
        final Object[] getParams = AndroidUtilities.concat(new Object[params.length + 4], params, "token", token,
                "modified_after", serverFetchTime);

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject result = null;
                try {
                    result = actFmInvoker.invoke(model + "_list", getParams);
                    JSONArray list = result.getJSONArray("list");
                    processor.process(list);
                    Preferences.setLong("actfm_time_" + lastSyncKey, result.optLong("time", 0));
                    Preferences.setLong("actfm_last_" + lastSyncKey, DateUtilities.now());

                    if(done != null)
                        done.run();
                } catch (IOException e) {
                    handleException("io-exception-list-" + model, e);
                } catch (JSONException e) {
                    handleException("json: " + result.toString(), e);
                }
            }
        }).start();
    }

    protected void handleException(String message, Exception exception) {
        Log.w("actfm-sync", message, exception);
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
        token = actFmPreferenceService.getToken();
        return true;
    }

    // --- json reader helper

    /**
     * Read data models from JSON
     */
    public static class JsonHelper {

        protected static long readDate(JSONObject item, String key) {
            return item.optLong(key, 0) * 1000L;
        }

        public static void updateFromJson(JSONObject json, Update model) throws JSONException {
            model.setValue(Update.REMOTE_ID, json.getLong("id"));
            readUser(json.getJSONObject("user"), model, Update.USER_ID, Update.USER);
            model.setValue(Update.ACTION, json.getString("action"));
            model.setValue(Update.ACTION_CODE, json.getString("action_code"));
            model.setValue(Update.TARGET_NAME, json.getString("target_name"));
            if(json.isNull("message"))
                model.setValue(Update.MESSAGE, "");
            else
                model.setValue(Update.MESSAGE, json.getString("message"));
            model.setValue(Update.PICTURE, json.getString("picture"));
            model.setValue(Update.CREATION_DATE, readDate(json, "created_at"));
            String tagIds = "," + json.optString("tag_ids", "") + ",";
            model.setValue(Update.TAGS, tagIds);
            model.setValue(Update.TASK, json.optLong("task_id", 0));
        }

        public static void readUser(JSONObject user, AbstractModel model, LongProperty idProperty,
                StringProperty userProperty) throws JSONException {
            long id = user.getLong("id");
            if(id == ActFmPreferenceService.userId()) {
                model.setValue(idProperty, 0L);
                if(userProperty != null)
                    model.setValue(userProperty, "");
            } else {
                model.setValue(idProperty, id);
                if(userProperty != null)
                    model.setValue(userProperty, user.toString());
            }
        }

        /**
         * Read tagData from JSON
         * @param model
         * @param json
         * @throws JSONException
         */
        public static void tagFromJson(JSONObject json, TagData model) throws JSONException {
            model.clearValue(TagData.REMOTE_ID);
            model.setValue(TagData.REMOTE_ID, json.getLong("id"));
            model.setValue(TagData.NAME, json.getString("name"));
            readUser(json.getJSONObject("user"), model, TagData.USER_ID, TagData.USER);

            if(json.has("picture"))
                model.setValue(TagData.PICTURE, json.optString("picture", ""));
            if(json.has("thumb"))
                model.setValue(TagData.THUMB, json.optString("thumb", ""));

            if(json.has("is_silent"))
                model.setFlag(TagData.FLAGS, TagData.FLAG_SILENT,json.getBoolean("is_silent"));

            if(json.has("emergent"))
                model.setFlag(TagData.FLAGS, TagData.FLAG_EMERGENT,json.getBoolean("emergent"));

            if(json.has("members")) {
                JSONArray members = json.getJSONArray("members");
                model.setValue(TagData.MEMBERS, members.toString());
                model.setValue(TagData.MEMBER_COUNT, members.length());
            }

            if(json.has("tasks"))
                model.setValue(TagData.TASK_COUNT, json.getInt("tasks"));
        }

        /**
         * Read task from json
         * @param json
         * @param model
         * @param metadata
         * @throws JSONException
         */
        public static void taskFromJson(JSONObject json, Task model, ArrayList<Metadata> metadata) throws JSONException {
            metadata.clear();
            model.clearValue(Task.REMOTE_ID);
            model.setValue(Task.REMOTE_ID, json.getLong("id"));
            readUser(json.getJSONObject("user"), model, Task.USER_ID, Task.USER);
            readUser(json.getJSONObject("creator"), model, Task.CREATOR_ID, null);
            model.setValue(Task.COMMENT_COUNT, json.getInt("comment_count"));
            model.setValue(Task.TITLE, json.getString("title"));
            model.setValue(Task.IMPORTANCE, json.getInt("importance"));
            model.setValue(Task.DUE_DATE,
                    Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, readDate(json, "due")));
            model.setValue(Task.COMPLETION_DATE, readDate(json, "completed_at"));
            model.setValue(Task.CREATION_DATE, readDate(json, "created_at"));
            model.setValue(Task.DELETION_DATE, readDate(json, "deleted_at"));
            model.setValue(Task.RECURRENCE, json.optString("repeat", ""));
            model.setValue(Task.NOTES, json.optString("notes", ""));
            model.setValue(Task.DETAILS_DATE, 0L);

            JSONArray tags = json.getJSONArray("tags");
            for(int i = 0; i < tags.length(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                String name = tag.getString("name");
                if(TextUtils.isEmpty(name))
                    continue;
                Metadata tagMetadata = new Metadata();
                tagMetadata.setValue(Metadata.KEY, TagService.KEY);
                tagMetadata.setValue(TagService.TAG, name);
                tagMetadata.setValue(TagService.REMOTE_ID, tag.getLong("id"));
                metadata.add(tagMetadata);
            }
        }
    }

}
