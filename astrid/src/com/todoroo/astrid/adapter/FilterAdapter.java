/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.service.TaskService;

public class FilterAdapter extends BaseExpandableListAdapter {

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;
    public int categoryStyle = R.style.TextAppearance_FLA_Category;
    public int headerStyle = R.style.TextAppearance_FLA_Header;

    // --- instance variables

    @Autowired
    private TaskService taskService;

    /** parent activity */
    protected final Activity activity;

    /** owner listview */
    protected final ExpandableListView listView;

    /** list of filters */
    private final ArrayList<FilterListItem> items;

    /** display metrics for scaling icons */
    private final DisplayMetrics metrics = new DisplayMetrics();

    /** receiver for new filters */
    private final FilterReceiver filterReceiver = new FilterReceiver();

    /** row layout to inflate */
    private final int layout;

    /** layout inflater */
    private final LayoutInflater inflater;

    /** whether to skip Filters that launch intents instead of being real filters */
    private final boolean skipIntentFilters;

    // Previous solution involved a queue of filters and a filterSizeLoadingThread. The filterSizeLoadingThread had
    // a few problems: how to make sure that the thread is resumed when the controlling activity is resumed, and
    // how to make sure that the the filterQueue does not accumulate filters without being processed. I am replacing
    // both the queue and a the thread with a thread pool, which will shut itself off after a second if it has
    // nothing to do (corePoolSize == 0, which makes it available for garbage collection), and will wake itself up
    // if new filters are queued (obviously it cannot be garbage collected if it is possible for new filters to
    // be added).
    private final ThreadPoolExecutor filterExecutor = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    public FilterAdapter(Activity activity, ExpandableListView listView,
            int rowLayout, boolean skipIntentFilters) {
        super();

        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        this.items = new ArrayList<FilterListItem>();
        this.listView = listView;
        this.layout = rowLayout;
        this.skipIntentFilters = skipIntentFilters;

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        listView.setGroupIndicator(
                activity.getResources().getDrawable(R.drawable.expander_group));
    }

    private void offerFilter(final Filter filter) {
        filterExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if(filter.listingTitle.matches(".* \\(\\d+\\)$")) //$NON-NLS-1$
                        return;
                    int size = taskService.countTasks(filter);
                    filter.listingTitle = filter.listingTitle + (" (" + //$NON-NLS-1$
                        size + ")"); //$NON-NLS-1$
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            notifyDataSetInvalidated();
                        }
                    });
                } catch (Exception e) {
                    Log.e("astrid-filter-adapter", "Error loading filter size", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        });
    }

    public boolean hasStableIds() {
        return true;
    }

    public void add(FilterListItem item) {
        items.add(item);

        // load sizes
        if(item instanceof Filter) {
            offerFilter((Filter)item);
        } else if(item instanceof FilterCategory) {
            for(Filter filter : ((FilterCategory)item).children)
                offerFilter(filter);
        }
    }

    public void clear() {
        items.clear();
    }

    /**
     * Create or reuse a view
     * @param convertView
     * @param parent
     * @return
     */
    protected View newView(View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.view = convertView;
            viewHolder.expander = (ImageView)convertView.findViewById(R.id.expander);
            viewHolder.icon = (ImageView)convertView.findViewById(R.id.icon);
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.selected = (ImageView)convertView.findViewById(R.id.selected);
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public ImageView expander;
        public ImageView icon;
        public TextView name;
        public ImageView selected;
        public View view;
    }

    /* ======================================================================
     * ========================================================== child nodes
     * ====================================================================== */

    public Object getChild(int groupPosition, int childPosition) {
        FilterListItem item = items.get(groupPosition);
        if(!(item instanceof FilterCategory) || ((FilterCategory)item).children == null)
            return null;

        return ((FilterCategory)item).children[childPosition];
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        FilterListItem item = items.get(groupPosition);
        if(!(item instanceof FilterCategory))
            return 0;
        return ((FilterCategory)item).children.length;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {

        convertView = newView(convertView, parent);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = (FilterListItem) getChild(groupPosition, childPosition);
        populateView(viewHolder, true, false);

        return convertView;
    }

    /* ======================================================================
     * ========================================================= parent nodes
     * ====================================================================== */

    public Object getGroup(int groupPosition) {
        if(groupPosition >= items.size())
            return null;
        return items.get(groupPosition);
    }

    public int getGroupCount() {
        return items.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
        convertView = newView(convertView, parent);
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = (FilterListItem) getGroup(groupPosition);
        populateView(viewHolder, false, isExpanded);
        return convertView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /* ======================================================================
     * ============================================================ selection
     * ====================================================================== */

    private FilterListItem selection = null;

    /**
     * Sets the selected item to this one
     * @param picked
     */
    public void setSelection(FilterListItem picked) {
        if(picked == selection)
            selection = null;
        else
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

    private static final String createExpansionPreference(FilterCategory category) {
        return "Expansion:" + category.listingTitle; //$NON-NLS-1$
    }

    /**
     * Receiver which receives intents to add items to the filter list
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public class FilterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final Parcelable[] filters = intent.getExtras().
                    getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                for (Parcelable item : filters) {
                    FilterListItem filter = (FilterListItem) item;
                    if(skipIntentFilters && !(filter instanceof Filter ||
                                filter instanceof FilterListHeader ||
                                filter instanceof FilterCategory))
                        continue;

                    add((FilterListItem)item);
                    onReceiveFilter((FilterListItem)item);
                }
                notifyDataSetChanged();

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        expandList(filters);
                    }
                });
            } catch (Exception e) {
                Log.e("receive-filter-" +  //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                        e.toString(), e);
            }
        }
    }

    /**
     * Expand the category filters in this group according to preference
     * @param filters
     */
    protected void expandList(Parcelable[] filters) {
        for(Parcelable filter : filters) {
            if(filter instanceof FilterCategory) {
                String preference = createExpansionPreference((FilterCategory) filter);
                if(!Preferences.getBoolean(preference, true))
                    continue;

                int count = getGroupCount();
                for(int i = 0; i < count; i++)
                    if(getGroup(i) == filter) {
                        listView.expandGroup(i);
                        break;
                    }
            }
        }
    }

    /**
     * Call to save user preference for whether a node is expanded
     * @param category
     * @param expanded
     */
    public void saveExpansionSetting(FilterCategory category, boolean expanded) {
        String preference = createExpansionPreference(category);
        Preferences.setBoolean(preference, expanded);
    }

    /**
     * Broadcast a request for lists. The request is sent to every
     * application registered to listen for this broadcast. Each application
     * can then add lists to this activity
     */
    public void getLists() {
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS);
        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        activity.registerReceiver(filterReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS));
        if(getGroupCount() == 0)
            getLists();
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
    }

    /**
     * Called when an item comes through. Override if you like
     * @param item
     */
    public void onReceiveFilter(FilterListItem item) {
        // do nothing
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    public void populateView(ViewHolder viewHolder, boolean isChild, boolean isExpanded) {
        FilterListItem filter = viewHolder.item;
        if(filter == null)
            return;

        viewHolder.view.setBackgroundResource(0);
        viewHolder.expander.setVisibility(View.GONE);

        if(viewHolder.item instanceof FilterListHeader) {
            viewHolder.name.setTextAppearance(activity, headerStyle);
            viewHolder.view.setBackgroundResource(R.drawable.edit_titlebar);
            viewHolder.view.setPadding((int) ((isChild ? 33 : 7) * metrics.density), 5, 0, 5);
        } else if(viewHolder.item instanceof FilterCategory) {
            viewHolder.expander.setVisibility(View.VISIBLE);
            if(isExpanded)
                viewHolder.expander.setImageResource(R.drawable.expander_ic_maximized);
            else
                viewHolder.expander.setImageResource(R.drawable.expander_ic_minimized);
            viewHolder.name.setTextAppearance(activity, categoryStyle);
            viewHolder.view.setPadding((int)(7 * metrics.density), 8, 0, 8);
        } else {
            viewHolder.name.setTextAppearance(activity, filterStyle);
            viewHolder.view.setPadding((int) ((isChild ? 27 : 7) * metrics.density), 8, 0, 8);
        }

        viewHolder.icon.setVisibility(filter.listingIcon != null ? View.VISIBLE : View.GONE);
        viewHolder.icon.setImageBitmap(filter.listingIcon);

        viewHolder.name.setText(filter.listingTitle);
        if(filter.color != 0)
            viewHolder.name.setTextColor(filter.color);

        // selection
        if(selection == viewHolder.item) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        } else
            viewHolder.selected.setVisibility(View.GONE);
    }

}
