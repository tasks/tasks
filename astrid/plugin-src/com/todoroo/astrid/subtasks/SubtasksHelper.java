package com.todoroo.astrid.subtasks;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;
import com.todoroo.astrid.utility.AstridPreferences;

public class SubtasksHelper {

    public static boolean shouldUseSubtasksFragmentForFilter(Filter filter) {
        if(filter == null || CoreFilterExposer.isInbox(filter) || CustomFilterExposer.isTodayFilter(filter) || SubtasksHelper.isTagFilter(filter)) {
            SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(ContextManager.getContext());
            int sortFlags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);
            if(SortHelper.isManualSort(sortFlags))
                return true;
        }
        return false;
    }

    public static Class<?> subtasksClassForFilter(Filter filter) {
        if (SubtasksHelper.isTagFilter(filter))
            return SubtasksTagListFragment.class;
        return SubtasksListFragment.class;
    }

    public static boolean isTagFilter(Filter filter) {
        if (filter instanceof FilterWithCustomIntent) {
            String className = ((FilterWithCustomIntent) filter).customTaskList.getClassName();
            if (TagViewFragment.class.getName().equals(className)
                    || SubtasksTagListFragment.class.getName().equals(className)) // Need to check this subclass because some shortcuts/widgets may have been saved with it
                return true;
        }
        return false;
    }

    @SuppressWarnings("nls")
    public static String serverFilterOrderId(String localFilterOrderId) {
        if (SubtasksUpdater.ACTIVE_TASKS_ORDER.equals(localFilterOrderId))
            return "all";
        else if (SubtasksUpdater.TODAY_TASKS_ORDER.equals(localFilterOrderId))
            return "today";
        return null;
    }

    @SuppressWarnings("nls")
    public static String applySubtasksToWidgetFilter(Filter filter, String query, String tagName, int limit) {
        if (SubtasksHelper.shouldUseSubtasksFragmentForFilter(filter)) {
            // care for manual ordering
            TagData tagData = PluginServices.getTagDataService().getTag(tagName, TagData.TAG_ORDERING);

            query = query.replaceAll("ORDER BY .*", "");
            query = query + String.format(" ORDER BY %s, %s, %s, %s",
                    Task.DELETION_DATE, Task.COMPLETION_DATE,
                    getOrderString(tagData), Task.CREATION_DATE);
            if (limit > 0)
                query = query + " LIMIT " + limit;
            query = query.replace(TaskCriteria.isVisible().toString(),
                    Criterion.all.toString());

            filter.setFilterQueryOverride(query);
        }
        return query;
    }

    private static String getOrderString(TagData tagData) {
        String serialized;
        if (tagData != null)
            serialized = tagData.getValue(TagData.TAG_ORDERING);
        else
            serialized = Preferences.getStringValue(SubtasksUpdater.ACTIVE_TASKS_ORDER);

        return AstridOrderedListUpdater.buildOrderString(getStringIdArray(serialized));
    }

    @SuppressWarnings("nls")
    @Deprecated
    private static Long[] getIdArray(String serializedTree) {
        ArrayList<Long> ids = new ArrayList<Long>();
        String[] digitsOnly = serializedTree.split("[\\[\\],\\s]"); // Split on [ ] , or whitespace chars
        for (String idString : digitsOnly) {
            try {
                if (!TextUtils.isEmpty(idString))
                    ids.add(Long.parseLong(idString));
            } catch (NumberFormatException e) {
                Log.e("widget-subtasks", "error parsing id " + idString, e);
            }
        }
        return ids.toArray(new Long[ids.size()]);
    }

    @SuppressWarnings("nls")
    public static String[] getStringIdArray(String serializedTree) {
        ArrayList<String> ids = new ArrayList<String>();
        String[] values = serializedTree.split("[\\[\\],\\s]"); // Split on [ ] , or whitespace chars
        for (String idString : values) {
            if (!TextUtils.isEmpty(idString))
                ids.add(idString);
        }
        return ids.toArray(new String[ids.size()]);
    }

    /**
     * Takes a subtasks string containing local ids and remaps it to one containing UUIDs
     * @param localTree
     * @return
     */
    public static String convertTreeToRemoteIds(String localTree) {
        Long[] localIds = getIdArray(localTree);
        HashMap<Long, String> idMap = getIdMap(localIds, Task.ID, Task.UUID);
        idMap.put(-1L, "-1"); //$NON-NLS-1$

        Node tree = AstridOrderedListUpdater.buildTreeModel(localTree, null);
        remapLocalTreeToRemote(tree, idMap);
        return AstridOrderedListUpdater.serializeTree(tree);
    }

    private static void remapLocalTreeToRemote(Node root, HashMap<Long, String> idMap) {
        ArrayList<Node> children = root.children;
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);

            long localId = -1L;
            try {
                localId = Long.parseLong(child.uuid);
            } catch (NumberFormatException e) {/**/}
            String uuid = idMap.get(localId);
            if (!RemoteModel.isValidUuid(uuid)) {
                children.remove(i);
                children.addAll(i, child.children);
                i--;
            } else {
                child.uuid = uuid;
                remapLocalTreeToRemote(child, idMap);
            }
        }
    }

    private static <A, B> HashMap<A, B> getIdMap(A[] keys, Property<A> keyProperty, Property<B> valueProperty) {
        HashMap<A, B> map = new HashMap<A, B>();
        TodorooCursor<Task> tasks = PluginServices.getTaskService().query(Query.select(keyProperty, valueProperty).where(keyProperty.in(keys)));
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
