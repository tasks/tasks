package com.todoroo.astrid.tags.reusable;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ListView;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.utility.Constants;

public class FeaturedListFilterAdapter extends FilterAdapter {
    public static final String BROADCAST_REQUEST_FEATURED_LISTS = Constants.PACKAGE + ".REQUEST_FEATURED_LISTS"; //$NON-NLS-1$
    public static final String BROADCAST_SEND_FEATURED_LISTS = Constants.PACKAGE + ".SEND_FEATURED_LISTS"; //$NON-NLS-1$

    public FeaturedListFilterAdapter(Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters) {
        super(activity, listView, rowLayout, skipIntentFilters);
    }

    @Override
    public void getLists() {
        Intent broadcastIntent = new Intent(BROADCAST_REQUEST_FEATURED_LISTS);
        activity.sendBroadcast(broadcastIntent);
    }

    @Override
    public void registerRecevier() {
        IntentFilter peopleFilter = new IntentFilter(BROADCAST_SEND_FEATURED_LISTS);
        activity.registerReceiver(filterReceiver, peopleFilter);
        getLists();
    }

    @Override
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
    }
}
