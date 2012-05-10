package com.todoroo.astrid.people;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.tags.TagService;

public class PeopleFilterExposer extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] listAsArray = prepareFilters(context);

        Intent broadcastIntent = new Intent(PeopleFilterAdapter.BROADCAST_SEND_PEOPLE_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, listAsArray);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, "people"); //$NON-NLS-1$
        context.sendBroadcast(broadcastIntent);
    }

    private FilterListItem[] prepareFilters(Context context) {
        TodorooCursor<User> users = PluginServices.getUserDao().query(Query.select(User.PROPERTIES)
                .orderBy(Order.asc(User.NAME), Order.asc(User.EMAIL)));
        try {
            FilterListItem[] items = new FilterListItem[users.getCount() + 1];
            items[0] = mySharedTasks(context);
            User user = new User();
            int i = 1;
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

    @SuppressWarnings("nls")
    public static FilterWithCustomIntent mySharedTasks(Context context) {
        AndroidUtilities.copyDatabases(context, "/sdcard/databases");
        TodorooCursor<TagData> tagsWithMembers = PluginServices.getTagDataService()
                .query(Query.select(TagData.NAME, TagData.MEMBERS).where(TagData.MEMBER_COUNT.gt(0)));
        String[] names;
        try {
            if (tagsWithMembers.getCount() == 0) {
                names = new String[1];
                names[0] = "\"\"";
            } else {
                names = new String[tagsWithMembers.getCount()];
                TagData curr = new TagData();
                int i = 0;
                for (tagsWithMembers.moveToFirst(); !tagsWithMembers.isAfterLast(); tagsWithMembers.moveToNext()) {
                    curr.readFromCursor(tagsWithMembers);
                    names[i] = "\"" + curr.getValue(TagData.NAME) + "\"";
                    System.err.println("Tag data " + curr.getValue(TagData.NAME) + " has members " + curr.getValue(TagData.MEMBERS));
                    i++;
                }
            }
        } finally {
            tagsWithMembers.close();
        }

        String title = context.getString(R.string.actfm_my_shared_tasks_title);
        QueryTemplate template = new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"),
                Criterion.and(Task.ID.eq(Field.field("mtags." + Metadata.TASK.name)),
                        Field.field("mtags." + Metadata.KEY.name).eq(TagService.KEY),
                        Field.field("mtags." + TagService.TAG.name).in(names))));

        FilterWithUpdate filter = new FilterWithUpdate(title, title, template, null);

        filter.customTaskList = new ComponentName(ContextManager.getContext(), PersonViewFragment.class);

        Bundle extras = new Bundle();
        extras.putBoolean(PersonViewFragment.EXTRA_HIDE_QUICK_ADD, true);
        filter.customExtras = extras;

        return filter;
    }
}
