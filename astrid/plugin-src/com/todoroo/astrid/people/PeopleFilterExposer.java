package com.todoroo.astrid.people;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;

public class PeopleFilterExposer extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] listAsArray = prepareFilters();

        Intent broadcastIntent = new Intent(PeopleFilterAdapter.BROADCAST_SEND_PEOPLE_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, listAsArray);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, "people"); //$NON-NLS-1$
        context.sendBroadcast(broadcastIntent);
    }

    private FilterListItem[] prepareFilters() {
        TodorooCursor<User> users = PluginServices.getUserDao().query(Query.select(User.PROPERTIES)
                .orderBy(Order.asc(User.NAME), Order.asc(User.EMAIL)));
        try {
            FilterListItem[] items = new FilterListItem[users.getCount()];
            User user = new User();
            int i = 0;
            for (users.moveToFirst(); !users.isAfterLast(); users.moveToNext()) {
                user.readFromCursor(users);
                Filter currFilter = filterFromUserData(user);
                items[i] = currFilter;
                i++;
            }
            return items;
        } finally {
            users.close();
        }
    }

    @SuppressWarnings("nls")
    public static FilterWithCustomIntent filterFromUserData(User user) {
        String email = user.getValue(User.EMAIL);

        String title = user.getDisplayName();
        QueryTemplate userTemplate = new QueryTemplate().where(
                        Criterion.or(Task.USER.like("%" + email + "%"),
                                Task.USER_ID.eq(user.getValue(User.REMOTE_ID))));

        FilterWithUpdate filter = new FilterWithUpdate(title, title, userTemplate, null);

        filter.customTaskList = new ComponentName(ContextManager.getContext(), PersonViewFragment.class);

        ContentValues values = new ContentValues();
        values.put(Task.USER_ID.name, user.getValue(User.REMOTE_ID));
        try {
            JSONObject userJson = new JSONObject();
            ActFmSyncService.JsonHelper.jsonFromUser(userJson, user);
            values.put(Task.USER.name, userJson.toString());
        } catch (JSONException e) {
            // Ignored
        }
        filter.valuesForNewTasks = values;

        String imageUrl = user.getValue(User.PICTURE);
        filter.imageUrl = imageUrl;

        Bundle extras = new Bundle();
        extras.putLong(PersonViewFragment.EXTRA_USER_ID_LOCAL, user.getId());
        filter.customExtras = extras;

        return filter;
    }
}
