/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.ArrayList;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
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
        View textView = getStandardView(item, true);
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
        else
            return getStandardView(item, isChild);
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
        FrameLayout.LayoutParams expansionImageLayout = new FrameLayout.LayoutParams(
                32, 32);
        expansionImageLayout.gravity = Gravity.CENTER_VERTICAL;
        image.setLayoutParams(expansionImageLayout);
        image.setScaleType(ScaleType.FIT_CENTER);
        layout.addView(image);

        TextView textView = new TextView(activity);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setText(filter.listingTitle);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Category);

        View view = augmentView(textView, filter);
        view.setPadding(60, 2, 0, 2);
        FrameLayout.LayoutParams rowLayout = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLayout.gravity = Gravity.CENTER_VERTICAL;
        view.setLayoutParams(rowLayout);

        layout.addView(view);

        return layout;
    }

    /**
     * Decorate textview and add an image if the filter requests it
     * @param textView
     * @param filter
     * @return final view ready to be added
     */
    private View augmentView(TextView textView, FilterListItem filter) {
        if(filter.listingIcon != null) {
            LinearLayout layout = new LinearLayout(activity);
            layout.setGravity(textView.getGravity());
            layout.setOrientation(LinearLayout.HORIZONTAL);

            ImageView icon = new ImageView(activity);
            icon.setImageBitmap(filter.listingIcon);
            icon.setScaleType(ScaleType.CENTER);
            icon.setPadding(0, 0, 15, 0);
            layout.addView(icon);
            layout.addView(textView);
            return layout;
        }

        return textView;
    }

    public View getStandardView(FilterListItem filter, boolean isChild) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        TextView textView = new TextView(activity);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setText(filter.listingTitle);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Filter);

        View view = augmentView(textView, filter);
        view.setBackgroundDrawable(null);
        view.setLayoutParams(lp);
        view.setPadding(isChild ? 50 : 10, 0, 0, 0);

        return view;
    }

    public View getHeaderView(FilterListHeader header, boolean isChild) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 40);

        TextView textView = new TextView(activity);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        textView.setTextAppearance(activity, R.style.TextAppearance_FLA_Header);
        textView.setText(header.listingTitle);

        View view = augmentView(textView, header);
        view.setBackgroundResource(R.drawable.edit_titlebar);
        view.setLayoutParams(lp);
        view.setPadding(isChild ? 50 : 10, 0, 0, 0);

        return view;
    }
}
