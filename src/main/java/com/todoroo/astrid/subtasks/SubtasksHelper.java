package com.todoroo.astrid.subtasks;

import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SubtasksHelper {

    private static final Logger log = LoggerFactory.getLogger(SubtasksHelper.class);

    public static boolean shouldUseSubtasksFragmentForFilter(Preferences preferences, Filter filter) {
        if(filter == null || BuiltInFilterExposer.isInbox(filter) || BuiltInFilterExposer.isTodayFilter(filter) || SubtasksHelper.isTagFilter(filter)) {
            int sortFlags = preferences.getSortFlags();
            if(SortHelper.isManualSort(sortFlags)) {
                return true;
            }
        }
        return false;
    }

    public static Class<?> subtasksClassForFilter(Filter filter) {
        if (SubtasksHelper.isTagFilter(filter)) {
            return SubtasksTagListFragment.class;
        }
        return SubtasksListFragment.class;
    }

    public static boolean isTagFilter(Filter filter) {
        if (filter instanceof FilterWithCustomIntent) {
            String className = ((FilterWithCustomIntent) filter).customTaskList.getClassName();
            if (TagViewFragment.class.getName().equals(className)
                    || SubtasksTagListFragment.class.getName().equals(className)) // Need to check this subclass because some shortcuts/widgets may have been saved with it
            {
                return true;
            }
        }
        return false;
    }

    public static String applySubtasksToWidgetFilter(Preferences preferences, TaskService taskService, TagDataDao tagDataDao, TaskListMetadataDao tlmd, Filter filter, String query, String tagName, int limit) {
        if (SubtasksHelper.shouldUseSubtasksFragmentForFilter(preferences, filter)) {
            // care for manual ordering
            TagData tagData = tagDataDao.getTagByName(tagName, TagData.UUID, TagData.TAG_ORDERING);
            TaskListMetadata tlm = null;
            if (tagData != null) {
                tlm = tlmd.fetchByTagId(tagData.getUuid(), TaskListMetadata.TASK_IDS);
            } else if (BuiltInFilterExposer.isInbox(filter)) {
                tlm = tlmd.fetchByTagId(TaskListMetadata.FILTER_ID_ALL, TaskListMetadata.TASK_IDS);
            } else if (BuiltInFilterExposer.isTodayFilter(filter)) {
                tlm = tlmd.fetchByTagId(TaskListMetadata.FILTER_ID_TODAY, TaskListMetadata.TASK_IDS);
            }

            query = query.replaceAll("ORDER BY .*", "");
            query = query + String.format(" ORDER BY %s, %s, %s, %s",
                    Task.DELETION_DATE, Task.COMPLETION_DATE,
                    getOrderString(taskService, tagData, tlm), Task.CREATION_DATE);
            if (limit > 0) {
                query = query + " LIMIT " + limit;
            }
            query = query.replace(TaskCriteria.isVisible().toString(),
                    Criterion.all.toString());

            filter.setFilterQueryOverride(query);
        }
        return query;
    }

    private static String getOrderString(TaskService taskService, TagData tagData, TaskListMetadata tlm) {
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
                log.error(e.getMessage(), e);
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

    public static interface TreeRemapHelper<T> {
        public T getKeyFromOldUuid(String uuid);
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
                    log.error(e.getMessage(), e);
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
