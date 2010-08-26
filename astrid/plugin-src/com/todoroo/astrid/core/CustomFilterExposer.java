/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer extends BroadcastReceiver {

    private static final String TOKEN_FILTER_ID = "id"; //$NON-NLS-1$
    private static final String TOKEN_FILTER_NAME = "name"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        PendingIntent customFilterIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, CustomFilterActivity.class), 0);
        IntentFilter customFilter = new IntentFilter(r.getString(R.string.BFE_Custom),
                customFilterIntent);
        customFilter.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.gnome_filter)).getBitmap();

        Filter[] savedFilters = buildSavedFilters(context);

        FilterListItem[] list;
        if(savedFilters.length == 0) {
            list = new FilterListItem[1];
        } else {
            list = new FilterListItem[2];
            list[1] = new FilterCategory(r.getString(R.string.BFE_Saved), savedFilters);
        }

        list[0] = customFilter;

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private Filter[] buildSavedFilters(Context context) {
        StoreObjectDao dao = PluginServices.getStoreObjectDao();
        TodorooCursor<StoreObject> cursor = dao.query(Query.select(StoreObject.PROPERTIES).where(
                StoreObject.TYPE.eq(SavedFilter.TYPE)).orderBy(Order.asc(SavedFilter.NAME)));
        try {
            Filter[] list = new Filter[cursor.getCount()];

            StoreObject savedFilter = new StoreObject();
            for(int i = 0; i < list.length; i++) {
                cursor.moveToNext();
                savedFilter.readFromCursor(cursor);
                list[i] = SavedFilter.load(savedFilter);

                Intent deleteIntent = new Intent(context, DeleteActivity.class);
                deleteIntent.putExtra(TOKEN_FILTER_ID, savedFilter.getId());
                deleteIntent.putExtra(TOKEN_FILTER_NAME, list[i].title);
                list[i].contextMenuLabels = new String[] { context.getString(R.string.BFE_Saved_delete) };
                list[i].contextMenuIntents = new Intent[] { deleteIntent };
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

}
