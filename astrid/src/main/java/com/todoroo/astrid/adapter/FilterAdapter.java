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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.Injector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterAdapter extends ArrayAdapter<Filter> {

    private static final Logger log = LoggerFactory.getLogger(FilterAdapter.class);

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;

    // --- instance variables

    private Injector injector;
    private final FilterCounter filterCounter;

    /** parent activity */
    protected final Activity activity;

    /** owner listview */
    protected ListView listView;

    /** display metrics for scaling icons */
    protected final DisplayMetrics metrics = new DisplayMetrics();

    /** receiver for new filters */
    protected final FilterReceiver filterReceiver = new FilterReceiver();

    private final FilterListUpdateReceiver filterListUpdateReceiver = new FilterListUpdateReceiver();

    /** row layout to inflate */
    private final int layout;

    /** layout inflater */
    private final LayoutInflater inflater;

    /** whether to skip Filters that launch intents instead of being real filters */
    private final boolean skipIntentFilters;

    /** whether rows are selectable */
    private final boolean selectable;

    public FilterAdapter(Injector injector, FilterCounter filterCounter, Activity activity, ListView listView,
            int rowLayout, boolean skipIntentFilters, boolean selectable) {
        super(activity, 0);
        this.injector = injector;
        this.filterCounter = filterCounter;
        this.activity = activity;
        this.listView = listView;
        this.layout = rowLayout;
        this.skipIntentFilters = skipIntentFilters;
        this.selectable = selectable;

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    private void offerFilter(final Filter filter) {
        if(selectable && selection == null) {
            setSelection(filter);
        }
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
        offerFilter(item);
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

    public void addOrLookup(Filter filter) {
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
    protected View newView(View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.view = convertView;
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.selected = (ImageView)convertView.findViewById(R.id.selected);
            viewHolder.size = (TextView)convertView.findViewById(R.id.size);
            viewHolder.decoration = null;
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public TextView name;
        public TextView size;
        public ImageView selected;
        public View view;
        public View decoration;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = newView(convertView, parent);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = getItem(position);
        populateView(viewHolder);

        Filter selected = null;
        if (activity instanceof AstridActivity) {
            boolean shouldHighlightSelected = ((AstridActivity) activity).getFragmentLayout() != AstridActivity.LAYOUT_SINGLE;
            if (shouldHighlightSelected) {
                TaskListFragment tlf = ((AstridActivity) activity).getTaskListFragment();
                selected = tlf.getFilter();
            }
        }

        if (selected != null && selected.equals(viewHolder.item)) {
//            convertView.setBackgroundColor(activity.getResources().getColor(R.color.tablet_list_selected));
        } else {
            convertView.setBackgroundColor(activity.getResources().getColor(android.R.color.transparent));
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

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class FilterReceiver extends BroadcastReceiver {
        private final List<ResolveInfo> filterExposerList;

        public FilterReceiver() {
            // query astrids AndroidManifest.xml for all registered default-receivers to expose filters
            PackageManager pm = ContextManager.getContext().getPackageManager();
            filterExposerList = pm.queryBroadcastReceivers(
                    new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS),
                    PackageManager.MATCH_DEFAULT_ONLY);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                for (ResolveInfo filterExposerInfo : filterExposerList) {
                    String className = filterExposerInfo.activityInfo.name;
                    AstridFilterExposer filterExposer;
                    filterExposer = (AstridFilterExposer) Class.forName(className, true, FilterAdapter.class.getClassLoader()).newInstance();

                    if (filterExposer != null) {
                        populateFiltersToAdapter(filterExposer.getFilters(injector));
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        protected void populateFiltersToAdapter(final Parcelable[] filters) {
            if (filters == null) {
                return;
            }

            for (Parcelable item : filters) {
                FilterListItem filter = (FilterListItem) item;
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
    }

    /**
     * Broadcast a request for lists. The request is sent to every
     * application registered to listen for this broadcast. Each application
     * can then add lists to this activity
     */
    public void getLists() {
        filterReceiver.onReceive(activity, null);
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        IntentFilter regularFilter = new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS);
        regularFilter.setPriority(2);
        activity.registerReceiver(filterReceiver, regularFilter);
        activity.registerReceiver(filterListUpdateReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_FILTER_LIST_UPDATED));
        getLists();

        refreshFilterCount();
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
        activity.unregisterReceiver(filterListUpdateReceiver);
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    /** Pattern for matching filter counts in listing titles */
    private final Pattern countPattern = Pattern.compile(".* \\((\\d+)\\)$"); //$NON-NLS-1$

    public void populateView(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if(filter == null) {
            return;
        }

        viewHolder.view.setBackgroundResource(0);

        if(viewHolder.decoration != null) {
            ((ViewGroup)viewHolder.view).removeView(viewHolder.decoration);
            viewHolder.decoration = null;
        }

        viewHolder.name.setTextAppearance(activity, filterStyle);
        viewHolder.name.setShadowLayer(0, 0, 0, 0);

        String title = filter.listingTitle;
        Matcher match = countPattern.matcher(filter.listingTitle);
        if(match.matches()) {
            title = title.substring(0, title.lastIndexOf(' '));
        }
        if(!title.equals(viewHolder.name.getText())) {
            viewHolder.name.setText(title);
        }

        // title / size
        int countInt = -1;
        if(filterCounter.containsKey(filter) || (!TextUtils.isEmpty(filter.listingTitle) && filter.listingTitle.matches(".* \\(\\d+\\)$"))) { //$NON-NLS-1$
            viewHolder.size.setVisibility(View.VISIBLE);
            String count = "";
            if (filterCounter.containsKey(filter)) {
                Integer c = filterCounter.get(filter);
                countInt = c;
                count = c.toString();
            }
            if(!count.equals(viewHolder.size.getText())) {
                viewHolder.size.setText(count);
            }
        } else {
            viewHolder.size.setVisibility(View.GONE);
            countInt = -1;
        }

        if(countInt == 0 && filter instanceof FilterWithCustomIntent) {
            viewHolder.name.setTextColor(Color.GRAY);
        }

        viewHolder.name.getLayoutParams().height = (int) (58 * metrics.density);

        if (filter.color != 0) {
            viewHolder.name.setTextColor(filter.color);
        }

        // selection
        if(selection == viewHolder.item) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        } else {
            viewHolder.selected.setVisibility(View.GONE);
        }
    }
}
