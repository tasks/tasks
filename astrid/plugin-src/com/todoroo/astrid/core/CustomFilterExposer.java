/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.model.StoreObject;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        PendingIntent customFilterIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, CustomFilterActivity.class), 0);
        IntentFilter customFilter = new IntentFilter(r.getString(R.string.BFE_Custom),
                customFilterIntent);
        customFilter.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.gnome_filter)).getBitmap();

        Filter[] customFilters = buildSavedFilters();

        FilterListItem[] list;
        if(customFilters.length == 0) {
            list = new FilterListItem[1];
        } else {
            list = new FilterListItem[customFilters.length + 2];
            list[1] = new FilterListHeader(r.getString(R.string.BFE_Saved));
            for(int i = 0; i < customFilters.length; i++)
                list[i + 2] = customFilters[i];
        }

        list[0] = customFilter;

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private Filter[] buildSavedFilters() {
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
            }

            return list;
        } finally {
            cursor.close();
        }
    }

}
