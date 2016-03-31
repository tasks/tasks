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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterActivity;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.filters.NavigationDrawerSeparator;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.HelpAndFeedbackActivity;
import org.tasks.preferences.Theme;
import org.tasks.ui.NavigationDrawerFragment;

import java.util.List;

import static org.tasks.preferences.ResourceResolver.getData;

public class FilterAdapter extends ArrayAdapter<FilterListItem> {

    private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;

    // --- instance variables

    public static final int REQUEST_SETTINGS = 10123;

    private final FilterProvider filterProvider;
    private final FilterCounter filterCounter;
    private final Activity activity;
    private final ListView listView;
    private boolean navigationDrawer;
    private final FilterListUpdateReceiver filterListUpdateReceiver = new FilterListUpdateReceiver();

    /** layout inflater */
    private final LayoutInflater inflater;

    public FilterAdapter(FilterProvider filterProvider, FilterCounter filterCounter, Activity activity,
                         Theme theme, ListView listView, boolean navigationDrawer) {
        super(activity, 0);
        this.filterProvider = filterProvider;
        this.filterCounter = filterCounter;
        this.activity = activity;
        this.listView = listView;
        this.navigationDrawer = navigationDrawer;

        inflater = theme.getThemedLayoutInflater();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void add(FilterListItem item) {
        if (getPosition(item) >= 0) {
            return;
        }

        super.add(item);
        // load sizes
        if (item instanceof Filter) {
            filterCounter.registerFilter((Filter) item);
        }
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

    public void refreshFilterCount() {
        filterCounter.refreshFilterCounts(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    /**
     * Create or reuse a view
     */
    private View newView(View convertView, ViewGroup parent, FilterListItem.Type viewType) {
        if(convertView == null) {
            ViewHolder viewHolder = new ViewHolder();
            switch(viewType) {
                case ITEM:
                    convertView = inflater.inflate(R.layout.filter_adapter_row, parent, false);
                    viewHolder.name = (TextView)convertView.findViewById(R.id.name);
                    viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    viewHolder.size = (TextView)convertView.findViewById(R.id.size);
                    break;
                case SEPARATOR:
                    convertView = inflater.inflate(R.layout.filter_adapter_separator, parent, false);
                    break;
                case SUBHEADER:
                    convertView = inflater.inflate(R.layout.filter_adapter_subheader, parent, false);
                    viewHolder.name = (TextView) convertView.findViewById(R.id.subheader_text);
                    break;
            }
            viewHolder.view = convertView;
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public TextView name;
        public ImageView icon;
        public TextView size;
        public View view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FilterListItem item = getItem(position);

        convertView = newView(convertView, parent, item.getItemType());
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = getItem(position);
        switch(item.getItemType()) {
            case ITEM:
                populateItem(viewHolder);

                Filter selected = null;
                if (activity instanceof TaskListActivity) {
                    TaskListFragment tlf = ((TaskListActivity) activity).getTaskListFragment();
                    selected = tlf.getFilter();
                }

                if (selected != null && selected.equals(viewHolder.item)) {
                    convertView.setBackgroundColor(getData(activity, R.attr.drawer_background_selected));
                }
                break;
            case SUBHEADER:
                populateHeader(viewHolder);
                break;
            case SEPARATOR:
                break;
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).getItemType() == FilterListItem.Type.ITEM;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getItemType().ordinal();
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

    private void addSubMenu(final int titleResource, List<Filter> filters, boolean hideIfEmpty) {
        if (hideIfEmpty && filters.isEmpty()) {
            return;
        }

        add(new NavigationDrawerSubheader(activity.getResources().getString(titleResource)));

        for (FilterListItem filterListItem : filters) {
            add(filterListItem);
        }
    }

    public void populateList() {
        clear();

        add(filterProvider.getMyTasksFilter());

        addSubMenu(R.string.filters, filterProvider.getFilters(), false);

        if (navigationDrawer) {
            add(new NavigationDrawerAction(
                    activity.getResources().getString(R.string.FLA_new_filter),
                    R.drawable.ic_add_24dp,
                    new Intent(activity, CustomFilterActivity.class),
                    TaskListFragment.ACTIVITY_REQUEST_NEW_FILTER));
        }

        addSubMenu(R.string.tags, filterProvider.getTags(), false);

        if (navigationDrawer) {
            add(new NavigationDrawerAction(
                    activity.getResources().getString(R.string.new_tag),
                    R.drawable.ic_add_24dp,
                    new Intent(activity, TagSettingsActivity.class),
                    NavigationDrawerFragment.REQUEST_NEW_LIST));
        }

        addSubMenu(R.string.gtasks_GPr_header, filterProvider.getGoogleTaskFilters(), true);

        if (navigationDrawer) {
            add(new NavigationDrawerSeparator());

            add(new NavigationDrawerAction(
                    activity.getResources().getString(R.string.TLA_menu_settings),
                    R.drawable.ic_settings_24dp,
                    new Intent(activity, BasicPreferences.class),
                    REQUEST_SETTINGS));
            add(new NavigationDrawerAction(
                    activity.getResources().getString(R.string.help_and_feedback),
                    R.drawable.ic_help_24dp,
                    new Intent(activity, HelpAndFeedbackActivity.class),
                    0));
        }

        notifyDataSetChanged();

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

        populateList();

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

    private void populateItem(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if(filter == null) {
            return;
        }

        viewHolder.view.setBackgroundResource(0);
        int icon = filter.icon;
        viewHolder.icon.setImageResource(icon);
        viewHolder.icon.setVisibility(icon == 0 ? View.INVISIBLE : View.VISIBLE);

        String title = filter.listingTitle;
        if(!title.equals(viewHolder.name.getText())) {
            viewHolder.name.setText(title);
        }

        int countInt = 0;
        if(filterCounter.containsKey(filter)) {
            countInt = filterCounter.get(filter);
            viewHolder.size.setText(Integer.toString(countInt));
        }
        viewHolder.size.setVisibility(countInt > 0 ? View.VISIBLE : View.INVISIBLE);

        if (selection == viewHolder.item) {
            viewHolder.view.setBackgroundColor(getData(activity, R.attr.drawer_background_selected));
        }
    }

    private void populateHeader(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if (filter == null) {
            return;
        }

        viewHolder.name.setText(filter.listingTitle);
    }
}
