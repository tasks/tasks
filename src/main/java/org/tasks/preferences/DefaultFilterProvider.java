package org.tasks.preferences;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.BuiltInFilterExposer;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;

public class DefaultFilterProvider {

    private static final int TYPE_FILTER = 0;
    private static final int TYPE_CUSTOM_FILTER = 1;
    private static final int TYPE_FILTER_WITH_CUSTOM_INTENT = 2;

    private final Context context;
    private final Preferences preferences;

    @Inject
    public DefaultFilterProvider(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    public Filter getDefaultFilter() {
        String listName = preferences.getStringValue(R.string.p_default_list_name);
        if (!isNullOrEmpty(listName)) {
            try {
                ContentValues valuesForNewTasks = AndroidUtilities.contentValuesFromSerializedString(preferences.getStringValue(R.string.p_default_list_values));
                String sqlQuery = preferences.getStringValue(R.string.p_default_list_sql);
                switch (preferences.getInt(R.string.p_default_list_type)) {
                    case TYPE_FILTER_WITH_CUSTOM_INTENT:
                        FilterWithCustomIntent filterWithCustomIntent = new FilterWithCustomIntent(listName, sqlQuery, valuesForNewTasks);
                        filterWithCustomIntent.customExtras = AndroidUtilities.bundleFromSerializedString(preferences.getStringValue(R.string.p_default_list_extras));
                        filterWithCustomIntent.customTaskList = ComponentName.unflattenFromString(preferences.getStringValue(R.string.p_default_list_class));
                        return filterWithCustomIntent;
                    case TYPE_CUSTOM_FILTER:
                        return new CustomFilter(listName, sqlQuery, valuesForNewTasks, preferences.getLong(R.string.p_default_list_id, 0L));
                }
                return new Filter(listName, sqlQuery, valuesForNewTasks);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
        return BuiltInFilterExposer.getMyTasksFilter(context.getResources());
    }

    public void setDefaultFilter(Filter filter) {
        preferences.setString(R.string.p_default_list_name, filter.listingTitle);
        preferences.setString(R.string.p_default_list_sql, filter.getSqlQuery());
        preferences.setInt(R.string.p_default_list_type, getFilterType(filter));
        preferences.setString(R.string.p_default_list_values, filter.valuesForNewTasks != null
                ? AndroidUtilities.contentValuesToSerializedString(filter.valuesForNewTasks)
                : null);
        if (filter instanceof FilterWithCustomIntent) {
            preferences.setString(R.string.p_default_list_class, ((FilterWithCustomIntent) filter).customTaskList.flattenToString());
            preferences.setString(R.string.p_default_list_extras, AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent) filter).customExtras));
        } else if (filter instanceof CustomFilter) {
            preferences.setLong(R.string.p_default_list_id, ((CustomFilter) filter).getId());
        }
    }

    private int getFilterType(Filter filter) {
        if (filter instanceof FilterWithCustomIntent) {
            return TYPE_FILTER_WITH_CUSTOM_INTENT;
        } else if (filter instanceof CustomFilter) {
            return TYPE_CUSTOM_FILTER;
        }
        return TYPE_FILTER;
    }
}
