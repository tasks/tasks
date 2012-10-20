package com.todoroo.astrid.subtasks;

import android.content.SharedPreferences;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
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
            if(tagName == null)
                tagName = SubtasksMetadata.LIST_ACTIVE_TASKS;
            else {
                TagData tag = PluginServices.getTagDataService().getTag(tagName, TagData.PROPERTIES);
                if (tag != null)
                    tagName = "td:"+tag.getId();
                else
                    tagName = SubtasksMetadata.LIST_ACTIVE_TASKS;
            }
            String subtaskJoin = String.format("LEFT JOIN %s ON (%s = %s AND %s = '%s' AND %s = '%s') ",
                    Metadata.TABLE, Task.ID, Metadata.TASK,
                    Metadata.KEY, SubtasksMetadata.METADATA_KEY,
                    SubtasksMetadata.TAG, tagName);

            if(!query.contains(subtaskJoin)) {
                query = subtaskJoin + query;
                query = query.replaceAll("ORDER BY .*", "");
                query = query + String.format(" ORDER BY %s, %s, IFNULL(CAST(%s AS LONG), %s)",
                        Task.DELETION_DATE, Task.COMPLETION_DATE,
                        SubtasksMetadata.ORDER, Task.CREATION_DATE);
                if (limit > 0)
                    query = query + " LIMIT " + limit;
                query = query.replace(TaskCriteria.isVisible().toString(),
                        Criterion.all.toString());

                filter.setFilterQueryOverride(query);
            }
        }
        return query;
    }

}
