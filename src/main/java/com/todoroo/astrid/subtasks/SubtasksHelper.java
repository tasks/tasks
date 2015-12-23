package com.todoroo.astrid.subtasks;

import android.content.Context;
import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class SubtasksHelper {

    private final Context context;
    private final Preferences preferences;
    private final TaskService taskService;
    private final TagDataDao tagDataDao;
    private final TaskListMetadataDao taskListMetadataDao;

    @Inject
    public SubtasksHelper(@ForApplication Context context, Preferences preferences, TaskService taskService, TagDataDao tagDataDao, TaskListMetadataDao taskListMetadataDao) {
        this.context = context;
        this.preferences = preferences;
        this.taskService = taskService;
        this.tagDataDao = tagDataDao;
        this.taskListMetadataDao = taskListMetadataDao;
    }

    public boolean shouldUseSubtasksFragmentForFilter(Filter filter) {
        if(filter == null || BuiltInFilterExposer.isInbox(context, filter) || BuiltInFilterExposer.isTodayFilter(context, filter) || filter.isTagFilter()) {
            if(preferences.getBoolean(R.string.p_manual_sort, false)) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> subtasksClassForFilter(Filter filter) {
        if (filter.isTagFilter()) {
            return SubtasksTagListFragment.class;
        }
        return SubtasksListFragment.class;
    }

    public String applySubtasksToWidgetFilter(Filter filter, String query, String tagName, int limit) {
        if (shouldUseSubtasksFragmentForFilter(filter)) {
            // care for manual ordering
            TagData tagData = tagDataDao.getTagByName(tagName, TagData.UUID, TagData.TAG_ORDERING);
            TaskListMetadata tlm = null;
            if (tagData != null) {
                tlm = taskListMetadataDao.fetchByTagId(tagData.getUuid(), TaskListMetadata.TASK_IDS);
            } else if (BuiltInFilterExposer.isInbox(context, filter)) {
                tlm = taskListMetadataDao.fetchByTagId(TaskListMetadata.FILTER_ID_ALL, TaskListMetadata.TASK_IDS);
            } else if (BuiltInFilterExposer.isTodayFilter(context, filter)) {
                tlm = taskListMetadataDao.fetchByTagId(TaskListMetadata.FILTER_ID_TODAY, TaskListMetadata.TASK_IDS);
            }

            query = query.replaceAll("ORDER BY .*", "");
            query = query + String.format(" ORDER BY %s, %s, %s, %s",
                    Task.DELETION_DATE, Task.COMPLETION_DATE,
                    getOrderString(tagData, tlm), Task.CREATION_DATE);
            if (limit > 0) {
                query = query + " LIMIT " + limit;
            }
            query = query.replace(TaskCriteria.isVisible().toString(),
                    Criterion.all.toString());

            filter.setFilterQueryOverride(query);
        }
        return query;
    }

    private String getOrderString(TagData tagData, TaskListMetadata tlm) {
        String serialized;
        if (tlm != null) {
            serialized = tlm.getTaskIDs();
        } else if (tagData != null) {
            serialized = convertTreeToRemoteIds(taskService, tagData.getTagOrdering());
        } else {
            serialized = "[]"; //$NON-NLS-1$
        }

        return AstridOrderedListUpdater.buildOrderString(getStringIdArray(serialized));
    }

    @Deprecated
    private static List<Long> getIdList(String serializedTree) {
        ArrayList<Long> ids = new ArrayList<>();
        String[] digitsOnly = serializedTree.split("[\\[\\],\\s]"); // Split on [ ] , or whitespace chars
        for (String idString : digitsOnly) {
            try {
                if (!TextUtils.isEmpty(idString)) {
                    ids.add(Long.parseLong(idString));
                }
            } catch (NumberFormatException e) {
                Timber.e(e, e.getMessage());
            }
        }
        return ids;
    }

    public static String[] getStringIdArray(String serializedTree) {
        ArrayList<String> ids = new ArrayList<>();
        String[] values = serializedTree.split("[\\[\\],\"\\s]"); // Split on [ ] , or whitespace chars
        for (String idString : values) {
            if (!TextUtils.isEmpty(idString)) {
                ids.add(idString);
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    /**
     * Takes a subtasks string containing local ids and remaps it to one containing UUIDs
     */
    public static String convertTreeToRemoteIds(TaskService taskService, String localTree) {
        List<Long> localIds = getIdList(localTree);
        HashMap<Long, String> idMap = getIdMap(taskService, localIds, Task.ID, Task.UUID);
        idMap.put(-1L, "-1"); //$NON-NLS-1$

        Node tree = AstridOrderedListUpdater.buildTreeModel(localTree, null);
        remapLocalTreeToRemote(tree, idMap);
        return AstridOrderedListUpdater.serializeTree(tree);
    }

    public interface TreeRemapHelper<T> {
        T getKeyFromOldUuid(String uuid);
    }

    public static <T> void remapTree(Node root, HashMap<T, String> idMap, TreeRemapHelper<T> helper) {
        ArrayList<Node> children = root.children;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            T key = helper.getKeyFromOldUuid(child.uuid);
            String uuid = idMap.get(key);
            if (!RemoteModel.isValidUuid(uuid)) {
                children.remove(i);
                children.addAll(i, child.children);
                i--;
            } else {
                child.uuid = uuid;
                remapTree(child, idMap, helper);
            }
        }
    }

    private static void remapLocalTreeToRemote(Node root, HashMap<Long, String> idMap) {
        remapTree(root, idMap, new TreeRemapHelper<Long>() {
            @Override
            public Long getKeyFromOldUuid(String uuid) {
                Long localId = -1L;
                try {
                    localId = Long.parseLong(uuid);
                } catch (NumberFormatException e) {
                    Timber.e(e, e.getMessage());
                }
                return localId;
            }
        });
    }

    private static <A, B> HashMap<A, B> getIdMap(TaskService taskService, Iterable<A> keys, Property<A> keyProperty, Property<B> valueProperty) {
        HashMap<A, B> map = new HashMap<>();
        TodorooCursor<Task> tasks = taskService.query(Query.select(keyProperty, valueProperty).where(keyProperty.in(keys)));
        try {
            for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                A key = tasks.get(keyProperty);
                B value = tasks.get(valueProperty);
                map.put(key, value);
            }
        } finally {
            tasks.close();
        }
        return map;
    }

}
