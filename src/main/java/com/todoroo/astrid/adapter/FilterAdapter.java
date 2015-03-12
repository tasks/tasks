/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithUpdate;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;

import static org.tasks.preferences.ResourceResolver.getData;

public class FilterAdapter extends ArrayAdapter<Filter> {

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;

    // --- instance variables

    private final FilterProvider filterProvider;
    private final FilterCounter filterCounter;

    /** parent activity */
    private final Activity activity;

    /** owner listview */
    private ListView listView;

    /** display metrics for scaling icons */
    private final DisplayMetrics metrics = new DisplayMetrics();

    private final FilterListUpdateReceiver filterListUpdateReceiver = new FilterListUpdateReceiver();

    /** row layout to inflate */
    private final int layout;

    /** layout inflater */
    private final LayoutInflater inflater;

    /** whether to skip Filters that launch intents instead of being real filters */
    private final boolean skipIntentFilters;

    public FilterAdapter(FilterProvider filterProvider, FilterCounter filterCounter, Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters) {
        super(activity, 0);
        this.filterProvider = filterProvider;
        this.filterCounter = filterCounter;
        this.activity = activity;
        this.listView = listView;
        this.layout = rowLayout;
        this.skipIntentFilters = skipIntentFilters;

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void add(Filter item) {
        super.add(item);
        // load sizes
        filterCounter.registerFilter(item);
        notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FilterAdapter.super.notifyDataSetChanged();
            }
        });
    }

    private void addOrLookup(Filter filter) {
        int index = getPosition(filter);
        if (index >= 0) {
            Filter existing = getItem(index);
            transferImageReferences(filter, existing);
            return;
        }
        add(filter);
    }

    // Helper function: if a filter was created from serialized extras, it may not
    // have the same image data we can get from the in-app broadcast
    private void transferImageReferences(Filter from, Filter to) {
        if (from instanceof FilterWithUpdate && to instanceof FilterWithUpdate) {
            ((FilterWithUpdate) to).imageUrl = ((FilterWithUpdate) from).imageUrl;
        }
    }

    public void refreshFilterCount() {
        filterCounter.refreshFilterCounts(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    /**
     * Create or reuse a view
     */
    private View newView(View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.view = convertView;
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.size = (TextView)convertView.findViewById(R.id.size);
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public TextView name;
        public TextView size;
        public View view;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = newView(convertView, parent);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = getItem(position);
        populateView(viewHolder);

        Filter selected = null;
        if (activity instanceof AstridActivity) {
            TaskListFragment tlf = ((AstridActivity) activity).getTaskListFragment();
            selected = tlf.getFilter();
        }

        if (selected != null && selected.equals(viewHolder.item)) {
            convertView.setBackgroundColor(getData(activity, R.attr.drawer_background_selected));
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
    /* ======================================================================
     * ============================================================ selection
     * ====================================================================== */

    private FilterListItem selection = null;

    /**
     * Sets the selected item to this one
     */
    public void setSelection(FilterListItem picked) {
        selection = picked;
        int scroll = listView.getScrollY();
        notifyDataSetInvalidated();
        listView.scrollTo(0, scroll);
    }

    /**
     * Gets the currently selected item
     * @return null if no item is to be selected
     */
    public FilterListItem getSelection() {
        return selection;
    }

    /* ======================================================================
     * ============================================================= receiver
     * ====================================================================== */

    public class FilterListUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifyDataSetChanged();
        }
    }

    public void getLists() {
        for (FilterListItem filter : filterProvider.getFilters()) {
            if(skipIntentFilters && !(filter instanceof Filter)) {
                continue;
            }

            if (filter instanceof Filter){
                addOrLookup((Filter) filter);
            }
        }
        filterCounter.refreshFilterCounts(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        activity.registerReceiver(filterListUpdateReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
        getLists();

        refreshFilterCount();
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterListUpdateReceiver);
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    private void populateView(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if(filter == null) {
            return;
        }

        viewHolder.view.setBackgroundResource(0);
        viewHolder.name.setTextAppearance(activity, filterStyle);
        viewHolder.name.setShadowLayer(0, 0, 0, 0);

        String title = filter.listingTitle;
        if(!title.equals(viewHolder.name.getText())) {
            viewHolder.name.setText(title);
        }

        int countInt;
        if(filterCounter.containsKey(filter)) {
            viewHolder.size.setVisibility(View.VISIBLE);
            countInt = filterCounter.get(filter);
            viewHolder.size.setText(Integer.toString(countInt));
        } else {
            viewHolder.size.setVisibility(View.GONE);
            countInt = -1;
        }

        if(countInt == 0) {
            viewHolder.size.setVisibility(View.GONE);
        }

        viewHolder.name.getLayoutParams().height = (int) (58 * metrics.density);

        if (filter.color != 0) {
            viewHolder.name.setTextColor(filter.color);
        }

        // selection
        if(selection == viewHolder.item) {
            // TODO: convert to color
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        }
    }
}
