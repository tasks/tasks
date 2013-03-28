/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;

/**
 * Exposes Astrid's built in filters to the {@link FilterListFragment}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    private static final String TOKEN_FILTER_ID = "id"; //$NON-NLS-1$
    private static final String TOKEN_FILTER_NAME = "name"; //$NON-NLS-1$

    @Autowired TagDataService tagDataService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] list = prepareFilters(context);

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Context context) {
        DependencyInjectionService.getInstance().inject(this);
        Resources r = context.getResources();

        Filter[] savedFilters = buildSavedFilters(context, r);
        return savedFilters;
    }

    private Filter[] buildSavedFilters(Context context, Resources r) {
        int themeFlags = ThemeService.getFilterThemeFlags();

        boolean useCustomFilters = Preferences.getBoolean(R.string.p_use_filters, true);
        StoreObjectDao dao = PluginServices.getStoreObjectDao();
        TodorooCursor<StoreObject> cursor = null;
        if (useCustomFilters)
            cursor = dao.query(Query.select(StoreObject.PROPERTIES).where(
                StoreObject.TYPE.eq(SavedFilter.TYPE)).orderBy(Order.asc(SavedFilter.NAME)));
        try {
            ArrayList<Filter> list = new ArrayList<Filter>();

            // stock filters
            if (Preferences.getBoolean(R.string.p_show_recently_modified_filter, true)) {
                Filter recent = new Filter(r.getString(R.string.BFE_Recent),
                        r.getString(R.string.BFE_Recent),
                        new QueryTemplate().where(
                                TaskCriteria.ownedByMe()).orderBy(
                                        Order.desc(Task.MODIFICATION_DATE)).limit(15),
                                        null);
                recent.listingIcon = ((BitmapDrawable)r.getDrawable(
                        ThemeService.getDrawable(R.drawable.filter_pencil, themeFlags))).getBitmap();

                list.add(recent);
            }

            if (Preferences.getBoolean(R.string.p_show_ive_assigned_filter, true))
                list.add(getAssignedByMeFilter(r));

            if (useCustomFilters && cursor != null) {
                StoreObject savedFilter = new StoreObject();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    savedFilter.readFromCursor(cursor);
                    Filter f = SavedFilter.load(savedFilter);

                    Intent deleteIntent = new Intent(context, DeleteActivity.class);
                    deleteIntent.putExtra(TOKEN_FILTER_ID, savedFilter.getId());
                    deleteIntent.putExtra(TOKEN_FILTER_NAME, f.title);
                    f.contextMenuLabels = new String[] { context.getString(R.string.BFE_Saved_delete) };
                    f.contextMenuIntents = new Intent[] { deleteIntent };
                    f.listingIcon = ((BitmapDrawable)r.getDrawable(
                            ThemeService.getDrawable(R.drawable.filter_sliders, themeFlags))).getBitmap();
                    list.add(f);
                }
            }

            return list.toArray(new Filter[list.size()]);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static Filter getAssignedByMeFilter(Resources r) {
        int themeFlags = ThemeService.getFilterThemeFlags();
        Filter f = new Filter(r.getString(R.string.BFE_Assigned),
                r.getString(R.string.BFE_Assigned),
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        Criterion.or(Task.CREATOR_ID.eq(0), Task.CREATOR_ID.eq(ActFmPreferenceService.userId())),
                        Task.USER_ID.neq(0))),
                        null);
        f.listingIcon = ((BitmapDrawable)r.getDrawable(
                ThemeService.getDrawable(R.drawable.filter_assigned, themeFlags))).getBitmap();
        return f;
    }

    /**
     * Simple activity for deleting stuff
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class DeleteActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Dialog);

            final long id = getIntent().getLongExtra(TOKEN_FILTER_ID, -1);
            if(id == -1) {
                finish();
                return;
            }
            final String name = getIntent().getStringExtra(TOKEN_FILTER_NAME);

            DependencyInjectionService.getInstance().inject(this);
            DialogUtilities.okCancelDialog(this,
                    getString(R.string.DLG_delete_this_item_question, name),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PluginServices.getStoreObjectDao().delete(id);
                            setResult(RESULT_OK);
                            finish();
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    });
        }
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null)
            return null;

        return prepareFilters(ContextManager.getContext());
    }

}
