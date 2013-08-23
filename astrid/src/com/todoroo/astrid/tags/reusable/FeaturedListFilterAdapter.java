package com.todoroo.astrid.tags.reusable;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    protected boolean shouldDirectlyPopulateFilters() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);

        ViewHolder viewHolder = (ViewHolder) v.getTag();
        viewHolder.size.setVisibility(View.GONE);
        viewHolder.name.setSingleLine(false);
        viewHolder.name.setLines(2);
        viewHolder.name.setMaxLines(2);

        int right = (int) (metrics.density * 10);
        int top = (int) (metrics.density * 2);
        viewHolder.name.setPadding(0, top, right, 0);
        viewHolder.name.setTextSize(14);
        viewHolder.name.setLineSpacing(0.0f, 1.2f);

        return v;
    }
}
