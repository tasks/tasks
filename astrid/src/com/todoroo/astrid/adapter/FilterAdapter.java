/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.ArrayList;

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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.utility.Preferences;

public class FilterAdapter extends BaseExpandableListAdapter {

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;
    public int categoryStyle = R.style.TextAppearance_FLA_Category;
    public int headerStyle = R.style.TextAppearance_FLA_Header;

    // --- instance variables

    protected final Activity activity;
    protected final ExpandableListView listView;
    private final ArrayList<FilterListItem> items;
    private final DisplayMetrics metrics = new DisplayMetrics();
    private final FilterReceiver filterReceiver = new FilterReceiver();
    private final int layout;
    private final LayoutInflater inflater;

    public FilterAdapter(Activity activity, ExpandableListView listView,
            int rowLayout) {
        super();
        this.activity = activity;
        this.items = new ArrayList<FilterListItem>();
        this.listView = listView;
        this.layout = rowLayout;

        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        listView.setGroupIndicator(
                activity.getResources().getDrawable(R.drawable.expander_group));

        getLists();
    }

    public boolean hasStableIds() {
        return true;
    }

    public void add(FilterListItem item) {
        items.add(item);
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

    private class ViewHolder {
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
        if(!(item instanceof FilterCategory))
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
        viewHolder.item = (FilterListItem)getChild(groupPosition, childPosition);
        populateView(viewHolder, true, false);

        return convertView;
    }

    /* ======================================================================
     * ========================================================= parent nodes
     * ====================================================================== */

    public Object getGroup(int groupPosition) {
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
    protected void getLists() {
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS);
        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        activity.registerReceiver(filterReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS));
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

        // selection
        if(selection == viewHolder.item) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        } else
            viewHolder.selected.setVisibility(View.GONE);
    }

}
