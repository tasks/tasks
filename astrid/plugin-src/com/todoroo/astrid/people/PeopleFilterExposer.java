/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.people;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.AstridPreferences;

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
                .where(Criterion.not(Criterion.or(User.STATUS.eq(User.STATUS_BLOCKED),
                                User.STATUS.eq(User.STATUS_IGNORED), User.STATUS.eq(User.STATUS_RENOUNCE), User.STATUS.eq(User.STATUS_IGNORE))))
                .orderBy(Order.asc(User.FIRST_NAME), Order.asc(User.LAST_NAME), Order.asc(User.NAME)));
        try {
            List<FilterListItem> items = new ArrayList<FilterListItem>();
            items.add(mySharedTasks(context));
            User user = new User();
            for (users.moveToFirst(); !users.isAfterLast(); users.moveToNext()) {
                user.readFromCursor(users);
                if (ActFmPreferenceService.userId().equals(user.getValue(User.UUID)))
                    continue;
                Filter currFilter = filterFromUserData(user);
                if (currFilter != null)
                    items.add(currFilter);
            }
            return items.toArray(new FilterListItem[items.size()]);
        } finally {
            users.close();
        }
    }

    @SuppressWarnings({ "nls", "deprecation" })
    private static FilterWithCustomIntent filterFromUserData(User user) {
        String title = user.getDisplayName();
        if (TextUtils.isEmpty(title) || "null".equals(title))
            return null;

        String email = user.getValue(User.EMAIL);
        Criterion criterion;
        if (TextUtils.isEmpty(email) || "null".equals(email))
            criterion = Task.USER_ID.eq(user.getValue(User.UUID));
        else
            criterion = Criterion.or(Task.USER_ID.eq(user.getValue(User.UUID)),
                    Task.USER.like("%" + email + "%")); // Deprecated field OK for backwards compatibility

        criterion = Criterion.and(TaskCriteria.activeAndVisible(), criterion);

        QueryTemplate userTemplate = new QueryTemplate().where(criterion);

        FilterWithUpdate filter = new FilterWithUpdate(title, title, userTemplate, null);

        filter.customTaskList = new ComponentName(ContextManager.getContext(), PersonViewFragment.class);

        ContentValues values = new ContentValues();
        values.put(Task.USER_ID.name, user.getValue(User.UUID));
        filter.valuesForNewTasks = values;

        String imageUrl = user.getPictureUrl(User.PICTURE, RemoteModel.PICTURE_THUMB); //user.getValue(User.PICTURE);
        filter.imageUrl = imageUrl;

        Bundle extras = new Bundle();
        extras.putLong(PersonViewFragment.EXTRA_USER_ID_LOCAL, user.getId());
        filter.customExtras = extras;

        return filter;
    }

    @SuppressWarnings("nls")
    public static FilterWithCustomIntent mySharedTasks(Context context) {
        TodorooCursor<TagData> tagsWithMembers = PluginServices.getTagDataService()
                .query(Query.select(TagData.NAME).where(TagData.MEMBER_COUNT.gt(0)));
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
                    i++;
                }
            }
        } finally {
            tagsWithMembers.close();
        }

        boolean isTablet = AstridPreferences.useTabletLayout(context);
        int themeFlags = isTablet ? ThemeService.FLAG_FORCE_LIGHT : 0;

        String title = context.getString(R.string.actfm_my_shared_tasks_title);
        QueryTemplate template = new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"),
                Criterion.and(Task.ID.eq(Field.field("mtags." + Metadata.TASK.name)),
                        Field.field("mtags." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY),
                        Field.field("mtags." + TaskToTagMetadata.TAG_NAME.name).in(names),
                        TaskCriteria.activeVisibleMine())));

        FilterWithCustomIntent filter = new FilterWithCustomIntent(title, title, template, null);

        filter.customTaskList = new ComponentName(ContextManager.getContext(), PersonViewFragment.class);

        Bundle extras = new Bundle();
        extras.putBoolean(PersonViewFragment.EXTRA_HIDE_QUICK_ADD, true);
        filter.customExtras = extras;

        filter.listingIcon = ((BitmapDrawable)context.getResources().getDrawable(
                ThemeService.getDrawable(R.drawable.icn_menu_friends, themeFlags))).getBitmap();

        return filter;
    }
}
