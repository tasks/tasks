package com.todoroo.astrid.subtasks;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.AstridPreferences;

public class SubtasksHelper {

    public static boolean shouldUseSubtasksFragmentForFilter(Filter filter) {
        if(filter == null || CoreFilterExposer.isInbox(filter) || SubtasksHelper.isTagFilter(filter)) {
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

        ArrayList<Long> ids = getIdArray(serialized);
        return AstridOrderedListUpdater.buildOrderString(ids.toArray(new Long[ids.size()]));
    }

    @SuppressWarnings("nls")
    private static ArrayList<Long> getIdArray(String serializedTree) {
        ArrayList<Long> ids = new ArrayList<Long>();
        String[] digitsOnly = serializedTree.split("\\D+");
        for (String idString : digitsOnly) {
            try {
                if (!TextUtils.isEmpty(idString))
                    ids.add(Long.parseLong(idString));
            } catch (NumberFormatException e) {
                Log.e("widget-subtasks", "error parsing id " + idString, e);
            }
        }
        return ids;
    }

}
