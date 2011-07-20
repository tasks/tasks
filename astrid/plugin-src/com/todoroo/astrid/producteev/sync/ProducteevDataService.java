/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.StoreObjectDao.StoreObjectCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.api.ApiUtilities;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.tags.TagService;

public final class ProducteevDataService {

    // --- constants

    /** Utility for joining tasks with metadata */
    public static final Join METADATA_JOIN = Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK));

    /** NoteMetadata provider string */
    public static final String NOTE_PROVIDER = "producteev"; //$NON-NLS-1$

    // --- singleton

    private static ProducteevDataService instance = null;

    public static synchronized ProducteevDataService getInstance() {
        if(instance == null)
            instance = new ProducteevDataService(ContextManager.getContext());
        return instance;
    }

    // --- instance variables

    protected final Context context;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private StoreObjectDao storeObjectDao;

    private final ProducteevUtilities preferences = ProducteevUtilities.INSTANCE;

    static final Random random = new Random();

    private ProducteevDataService(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- task and metadata methods

    /**
     * Clears metadata information. Used when user logs out of service
     */
    public void clearMetadata() {
        PluginServices.getTaskService().clearDetails(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                where(MetadataCriteria.withKey(ProducteevTask.METADATA_KEY))));
        metadataService.deleteWhere(Metadata.KEY.eq(ProducteevTask.METADATA_KEY));
        storeObjectDao.deleteWhere(StoreObject.TYPE.eq(ProducteevDashboard.TYPE));
    }

    /**
     * Gets tasks that were created since last sync
     * @param properties
     * @return
     */
    public TodorooCursor<Task> getLocallyCreated(Property<?>[] properties) {
        return
            taskDao.query(Query.select(properties).join(ProducteevDataService.METADATA_JOIN).where(
                    Criterion.and(
                            Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                                    where(Criterion.and(MetadataCriteria.withKey(ProducteevTask.METADATA_KEY), ProducteevTask.ID.gt(0))))),
                            TaskCriteria.isActive())).
                    groupBy(Task.ID));
    }

    /**
     * Gets tasks that were modified since last sync
     * @param properties
     * @return null if never sync'd
     */
    public TodorooCursor<Task> getLocallyUpdated(Property<?>[] properties) {
        long lastSyncDate = preferences.getLastSyncDate();
        if(lastSyncDate == 0)
            return taskDao.query(Query.select(Task.ID).where(Criterion.none));
        return
            taskDao.query(Query.select(properties).join(ProducteevDataService.METADATA_JOIN).where(
                    Criterion.and(
                            MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                            ProducteevTask.ID.gt(0),
                            Task.MODIFICATION_DATE.gt(lastSyncDate))).
                    groupBy(Task.ID));
    }

    /**
     * Searches for a local task with same remote id, updates this task's id
     * @param remoteTask
     * @return true if found local match
     */
    public boolean findLocalMatch(ProducteevTaskContainer remoteTask) {
        if(remoteTask.task.getId() != Task.NO_ID)
            return true;
        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.ID).
                join(ProducteevDataService.METADATA_JOIN).where(Criterion.and(MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                        ProducteevTask.ID.eq(remoteTask.pdvTask.getValue(ProducteevTask.ID)))));
        try {
            if(cursor.getCount() == 0)
                return false;
            cursor.moveToFirst();
            remoteTask.task.setId(cursor.get(Task.ID));
            return true;
        } finally {
            cursor.close();
        }
    }

    /**
     * Saves a task and its metadata
     * @param task
     */
    public void saveTaskAndMetadata(ProducteevTaskContainer task) {
        taskDao.save(task.task);

        task.metadata.add(task.pdvTask);
        metadataService.synchronizeMetadata(task.task.getId(), task.metadata,
                Criterion.or(MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                        MetadataCriteria.withKey(TagService.KEY),
                        MetadataCriteria.withKey(NoteMetadata.METADATA_KEY)));
    }

    /**
     * Reads a task and its metadata
     * @param task
     * @return
     */
    public ProducteevTaskContainer readTaskAndMetadata(TodorooCursor<Task> taskCursor) {
        Task task = new Task(taskCursor);

        // read tags, notes, etc
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        TodorooCursor<Metadata> metadataCursor = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(Criterion.and(MetadataCriteria.byTask(task.getId()),
                        Criterion.or(MetadataCriteria.withKey(TagService.KEY),
                                MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                                MetadataCriteria.withKey(NoteMetadata.METADATA_KEY)))));
        try {
            for(metadataCursor.moveToFirst(); !metadataCursor.isAfterLast(); metadataCursor.moveToNext()) {
                metadata.add(new Metadata(metadataCursor));
            }
        } finally {
            metadataCursor.close();
        }

        return new ProducteevTaskContainer(task, metadata);
    }

    /**
     * Reads metadata out of a task
     * @return null if no metadata found
     */
    public Metadata getTaskMetadata(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(
                Metadata.PROPERTIES).where(
                MetadataCriteria.byTaskAndwithKey(taskId, ProducteevTask.METADATA_KEY)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new Metadata(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Reads task notes out of a task
     */
    public TodorooCursor<Metadata> getTaskNotesCursor(long taskId) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.PROPERTIES).
                where(MetadataCriteria.byTaskAndwithKey(taskId, NoteMetadata.METADATA_KEY)));
        return cursor;
    }

    private void readDashboards() {
        if (dashboards == null) {
            dashboards = readStoreObjects(ProducteevDashboard.TYPE);
        }
    }

    /**
     * Reads store objects.
     */
    public StoreObject[] readStoreObjects(String type) {
        StoreObject[] ret;
        TodorooCursor<StoreObject> cursor = storeObjectDao.query(Query.select(StoreObject.PROPERTIES).
                where(StoreObjectCriteria.byType(type)));
        try {
            ret = new StoreObject[cursor.getCount()];
            for(int i = 0; i < ret.length; i++) {
                cursor.moveToNext();
                StoreObject dashboard = new StoreObject(cursor);
                ret[i] = dashboard;
            }
        } finally {
            cursor.close();
        }

        return ret;
    }

    // --- dashboard methods

    private StoreObject[] dashboards = null;

    /**
     * @return a list of dashboards
     */
    public StoreObject[] getDashboards() {
        readDashboards();
        return dashboards;
    }

    /**
     * Reads dashboards
     * @throws JSONException
     */
    @SuppressWarnings("nls")
    public void updateDashboards(JSONArray changedDashboards) throws JSONException {
        readDashboards();
        for(int i = 0; i < changedDashboards.length(); i++) {
            JSONObject remote = changedDashboards.getJSONObject(i).getJSONObject("dashboard");
            updateDashboards(remote, false);
        }

        // clear dashboard cache
        dashboards = null;
    }

    @SuppressWarnings("nls")
    public StoreObject updateDashboards(JSONObject remote, boolean reinitCache) throws JSONException {
        if (reinitCache)
            readDashboards();
        long id = remote.getLong("id_dashboard");
        StoreObject local = null;
        for(StoreObject dashboard : dashboards) {
            if(dashboard.getValue(ProducteevDashboard.REMOTE_ID).equals(id)) {
                local = dashboard;
                break;
            }
        }

        if(remote.getInt("deleted") != 0) {
            if(local != null)
                storeObjectDao.delete(local.getId());
        }

        if(local == null)
            local = new StoreObject();
        local.setValue(StoreObject.TYPE, ProducteevDashboard.TYPE);
        local.setValue(ProducteevDashboard.REMOTE_ID, id);
        local.setValue(ProducteevDashboard.NAME, ApiUtilities.decode(remote.getString("title")));

        StringBuilder users = new StringBuilder();
        JSONArray accessList = remote.getJSONArray("accesslist");
        for(int j = 0; j < accessList.length(); j++) {
            JSONObject user = accessList.getJSONObject(j).getJSONObject("user");
            users.append(user.getLong("id_user")).append(',');
            String name = ApiUtilities.decode(user.optString("firstname", "") + ' ' +
                    user.optString("lastname", ""));
            users.append(name.trim()).append(';');
        }
        local.setValue(ProducteevDashboard.USERS, users.toString());
        storeObjectDao.persist(local);
        if (reinitCache)
            dashboards = null;
        return local;
    }
}
