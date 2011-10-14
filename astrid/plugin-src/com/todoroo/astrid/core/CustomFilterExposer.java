/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategoryWithNewButton;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    private static final String TOKEN_FILTER_ID = "id"; //$NON-NLS-1$
    private static final String TOKEN_FILTER_NAME = "name"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] list = prepareFilters(context);

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Context context) {
        Resources r = context.getResources();

        PendingIntent customFilterIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, CustomFilterActivity.class), 0);

        Filter[] savedFilters = buildSavedFilters(context, r);

        FilterCategoryWithNewButton heading = new FilterCategoryWithNewButton(r.getString(R.string.BFE_Saved), savedFilters);
        heading.label = r.getString(R.string.tag_FEx_add_new);
        heading.intent = customFilterIntent;


        FilterListItem[] list = new FilterListItem[1];
        list[0] = heading;
        return list;
    }

    private Filter[] buildSavedFilters(Context context, Resources r) {
        StoreObjectDao dao = PluginServices.getStoreObjectDao();
        TodorooCursor<StoreObject> cursor = dao.query(Query.select(StoreObject.PROPERTIES).where(
                StoreObject.TYPE.eq(SavedFilter.TYPE)).orderBy(Order.asc(SavedFilter.NAME)));
        try {
            Filter[] list = new Filter[cursor.getCount() + 2];

            // stock filters
            String todayTitle = AndroidUtilities.capitalize(r.getString(R.string.today));
            ContentValues todayValues = new ContentValues();
            todayValues.put(Task.DUE_DATE.name, PermaSql.VALUE_EOD);
            list[0] = new Filter(todayTitle,
                    todayTitle,
                    new QueryTemplate().where(
                            Criterion.and(TaskCriteria.activeVisibleMine(),
                                    Task.DUE_DATE.gt(0),
                                    Task.DUE_DATE.lte(PermaSql.VALUE_EOD))),
                    todayValues);
            list[0].listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_calendar)).getBitmap();

            list[1] = new Filter(r.getString(R.string.BFE_Recent),
                    r.getString(R.string.BFE_Recent),
                    new QueryTemplate().where(
                            TaskCriteria.ownedByMe()).orderBy(
                                    Order.desc(Task.MODIFICATION_DATE)).limit(15),
                    null);
            list[1].listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_pencil)).getBitmap();

            StoreObject savedFilter = new StoreObject();
            for(int i = 2; i < list.length; i++) {
                cursor.moveToNext();
                savedFilter.readFromCursor(cursor);
                list[i] = SavedFilter.load(savedFilter);

                Intent deleteIntent = new Intent(context, DeleteActivity.class);
                deleteIntent.putExtra(TOKEN_FILTER_ID, savedFilter.getId());
                deleteIntent.putExtra(TOKEN_FILTER_NAME, list[i].title);
                list[i].contextMenuLabels = new String[] { context.getString(R.string.BFE_Saved_delete) };
                list[i].contextMenuIntents = new Intent[] { deleteIntent };
                list[i].listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_sliders)).getBitmap();
            }

            return list;
        } finally {
            cursor.close();
        }
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
