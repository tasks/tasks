package com.todoroo.astrid.people;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
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
        String[] tags;

        TodorooCursor<TagData> tagsWithUser = PluginServices.getTagDataService().query(Query.select(TagData.NAME)
                .where(Criterion.or(
                        TagData.MEMBERS.like("%" + email + "%"),
                        TagData.USER.like("%" + email + "%"),
                        TagData.USER_ID.eq(user.getId()))));
        try {
            if (tagsWithUser.getCount() == 0) {
                tags = new String[1];
                tags[0] = "\"\"";
            } else {
                tags = new String[tagsWithUser.getCount()];
                int i = 0;
                TagData curr = new TagData();
                for (tagsWithUser.moveToFirst(); !tagsWithUser.isAfterLast(); tagsWithUser.moveToNext()) {
                    curr.readFromCursor(tagsWithUser);
                    tags[i] = "\"" + curr.getValue(TagData.NAME) + "\"";
                    i++;
                }
            }
        } finally {
            tagsWithUser.close();
        }

        String title = user.getDisplayName();
        QueryTemplate userTemplate = new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"),
                Criterion.and(Task.ID.eq(Field.field("mtags." + Metadata.TASK.name)),
                        Field.field("mtags." + Metadata.KEY.name).eq(TagService.KEY),
                        Field.field("mtags." + TagService.TAG.name).in(tags),
                        Criterion.or(Task.USER.like("%" + email + "%"),
                                Task.USER_ID.eq(user.getValue(User.REMOTE_ID))))));

        FilterWithUpdate filter = new FilterWithUpdate(title, title, userTemplate, null);

        filter.customTaskList = new ComponentName(ContextManager.getContext(), PersonViewFragment.class);

        ContentValues values = new ContentValues();
        values.put(Task.USER_ID.name, user.getValue(User.REMOTE_ID));
        filter.valuesForNewTasks = values;

        String imageUrl = user.getValue(User.PICTURE);
        filter.imageUrl = imageUrl;

        Bundle extras = new Bundle();
        extras.putLong(PersonViewFragment.EXTRA_USER_ID_LOCAL, user.getId());
        filter.customExtras = extras;

        return filter;
    }
}
