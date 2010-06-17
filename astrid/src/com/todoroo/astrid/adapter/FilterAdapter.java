/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.ArrayList;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;

public class FilterAdapter extends BaseExpandableListAdapter {

    private final ArrayList<FilterListItem> items;
    protected final Activity activity;

    public FilterAdapter(Activity activity) {
        super();
        this.activity = activity;
        this.items = new ArrayList<FilterListItem>();
    }

    public boolean hasStableIds() {
        return true;
    }

    public void add(FilterListItem item) {
        items.add(item);
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
        FilterListItem item = (FilterListItem)getChild(groupPosition, childPosition);
        View textView = getFilterView((Filter)item, true);
        return textView;
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
        View view = getView((FilterListItem)getGroup(groupPosition), false, isExpanded);
        return view;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    public View getView(FilterListItem item, boolean isChild, boolean isExpanded) {
        if(item instanceof FilterListHeader)
            return getHeaderView((FilterListHeader)item, isChild);
        else if(item instanceof FilterCategory)
            return getCategoryView((FilterCategory)item, isExpanded);
        else if(item instanceof Filter)
            return getFilterView((Filter)item, isChild);
        else
            throw new UnsupportedOperationException("unknown item type"); //$NON-NLS-1$
    }

    public View getCategoryView(FilterCategory filter, boolean isExpanded) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        FrameLayout layout = new FrameLayout(activity);
        layout.setLayoutParams(lp);

        ImageView image = new ImageView(activity);
        if(isExpanded)
            image.setImageResource(R.drawable.expander_ic_maximized);
        else
            image.setImageResource(R.drawable.expander_ic_minimized);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.FILL_PARENT);
        llp.gravity = Gravity.CENTER_VERTICAL;
        image.setLayoutParams(llp);
        layout.addView(image);

        TextView textView = new TextView(activity);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setLayoutParams(llp);
        textView.setPadding(40, 0, 0, 0);
        textView.setText(filter.listingTitle);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Category);
        layout.addView(textView);

        return layout;
    }

    public TextView getFilterView(Filter filter, boolean isChild) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        TextView textView = new TextView(activity);
        textView.setBackgroundDrawable(null);
        textView.setLayoutParams(lp);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setPadding(isChild ? 40 : 10, 0, 0, 0);
        textView.setText(filter.listingTitle);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Filter);

        return textView;
    }

    public TextView getHeaderView(FilterListHeader header, boolean isChild) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 40);

        TextView textView = new TextView(activity);
        textView.setLayoutParams(lp);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setPadding(isChild ? 40 : 10, 0, 0, 0);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Header);
        textView.setBackgroundResource(R.drawable.edit_titlebar);
        textView.setText(header.listingTitle);

        return textView;
    }
}
