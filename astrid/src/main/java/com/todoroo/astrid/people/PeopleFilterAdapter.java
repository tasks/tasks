/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.people;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ListView;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.utility.Constants;

public class PeopleFilterAdapter extends FilterAdapter {

    public static final String BROADCAST_REQUEST_PEOPLE_FILTERS = Constants.PACKAGE + ".REQUEST_PEOPLE_FILTERS"; //$NON-NLS-1$
    public static final String BROADCAST_SEND_PEOPLE_FILTERS = Constants.PACKAGE + ".SEND_PEOPLE_FILTERS"; //$NON-NLS-1$

    public PeopleFilterAdapter(Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters) {
        super(activity, listView, rowLayout, skipIntentFilters);
    }

    @Override
    public void getLists() {
        Intent broadcastIntent = new Intent(BROADCAST_REQUEST_PEOPLE_FILTERS);
        activity.sendBroadcast(broadcastIntent);
    }

    @Override
    public void registerRecevier() {
        IntentFilter peopleFilter = new IntentFilter(BROADCAST_SEND_PEOPLE_FILTERS);
        activity.registerReceiver(filterReceiver, peopleFilter);
        getLists();
    }

    @Override
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
    }

    @Override
    protected boolean shouldDirectlyPopulateFilters() {
        return false;
    }
}
