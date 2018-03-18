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
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.caldav.CalDAVSettingsActivity;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.filters.NavigationDrawerAction;
import org.tasks.filters.NavigationDrawerSeparator;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.HelpAndFeedbackActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.NavigationDrawerFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.support.v4.content.ContextCompat.getColor;
import static com.todoroo.andlib.utility.AndroidUtilities.preLollipop;

public class FilterAdapter extends ArrayAdapter<FilterListItem> {

    private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;

    // --- instance variables

    public static final int REQUEST_SETTINGS = 10123;

    private final FilterProvider filterProvider;
    private final FilterCounter filterCounter;
    private final Activity activity;
    private final Theme theme;
    private final Locale locale;
    private final Preferences preferences;
    private final FilterListUpdateReceiver filterListUpdateReceiver = new FilterListUpdateReceiver();
    private final List<FilterListItem> items = new ArrayList<>();

    private boolean navigationDrawer;
    private Filter selected;

    private final LayoutInflater inflater;
    private final ThemeCache themeCache;

    @Inject
    public FilterAdapter(FilterProvider filterProvider, FilterCounter filterCounter, Activity activity,
                         Theme theme, ThemeCache themeCache, Locale locale, Preferences preferences) {
        super(activity, 0);
        this.filterProvider = filterProvider;
        this.filterCounter = filterCounter;
        this.activity = activity;
        this.theme = theme;
        this.locale = locale;
        this.preferences = preferences;
        this.inflater = theme.getLayoutInflater(activity);
        this.themeCache = themeCache;
    }

    public void setNavigationDrawer() {
        navigationDrawer = true;
    }

    public FilterListUpdateReceiver getFilterListUpdateReceiver() {
        return filterListUpdateReceiver;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void add(FilterListItem item) {
        super.add(item);

        items.add(item);

        if (navigationDrawer && item instanceof Filter) {
            filterCounter.registerFilter((Filter) item);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        activity.runOnUiThread(FilterAdapter.super::notifyDataSetChanged);
    }

    public void refreshFilterCount() {
        filterCounter.refreshFilterCounts(this::notifyDataSetChanged);
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
                    viewHolder.name = convertView.findViewById(R.id.name);
                    if (navigationDrawer) {
                        viewHolder.name.setCheckMarkDrawable(null);
                    }
                    viewHolder.icon = convertView.findViewById(R.id.icon);
                    viewHolder.size = convertView.findViewById(R.id.size);
                    if (preLollipop()) {
                        ColorStateList tintList = new ColorStateList(new int[][]{
                                new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                                new int[]{ResourcesCompat.getColor(activity.getResources(), android.R.color.transparent, null), theme.getThemeAccent().getAccentColor()});
                        Drawable original = ContextCompat.getDrawable(activity, R.drawable.ic_check_black_24dp);
                        Drawable wrapped = DrawableCompat.wrap(original.mutate());
                        DrawableCompat.setTintList(wrapped, tintList);
                        viewHolder.name.setCheckMarkDrawable(wrapped);
                    }
                    break;
                case SEPARATOR:
                    convertView = inflater.inflate(R.layout.filter_adapter_separator, parent, false);
                    break;
                case SUBHEADER:
                    convertView = inflater.inflate(R.layout.filter_adapter_subheader, parent, false);
                    viewHolder.name = convertView.findViewById(R.id.subheader_text);
                    break;
            }
            viewHolder.view = convertView;
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public void setSelected(Filter selected) {
        this.selected = selected;
    }

    public Filter getSelected() {
        return selected;
    }

    public int indexOf(FilterListItem item) {
        return items.indexOf(item);
    }

    public static class ViewHolder {
        public FilterListItem item;
        public CheckedTextView name;
        public ImageView icon;
        public TextView size;
        public View view;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        FilterListItem item = getItem(position);

        convertView = newView(convertView, parent, item.getItemType());
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.item = getItem(position);
        switch(item.getItemType()) {
            case ITEM:
                populateItem(viewHolder);
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
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
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
        addSubMenu(activity.getResources().getString(titleResource), filters, hideIfEmpty);
    }

    private void addSubMenu(String title, List<Filter> filters, boolean hideIfEmpty) {
        if (hideIfEmpty && filters.isEmpty()) {
            return;
        }

        add(new NavigationDrawerSubheader(title));

        for (FilterListItem filterListItem : filters) {
            add(filterListItem);
        }
    }

    @Override
    public void clear() {
        super.clear();
        items.clear();
    }

    public void populateRemoteListPicker() {
        clear();

        Filter item = new Filter(activity.getString(R.string.dont_sync), null);
        item.icon = R.drawable.ic_cloud_off_black_24dp;
        add(item);

        String title = preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME);
        if (Strings.isNullOrEmpty(title)) {
            title = activity.getResources().getString(R.string.gtasks_GPr_header);
        }
        addSubMenu(title, filterProvider.getGoogleTaskFilters(), true);

        addSubMenu(R.string.CalDAV, filterProvider.getCalDAVFilters(), true);
        
        notifyDataSetChanged();
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
                    NavigationDrawerFragment.ACTIVITY_REQUEST_NEW_FILTER));
        }

        addSubMenu(R.string.tags, filterProvider.getTags(), false);

        if (navigationDrawer) {
            add(new NavigationDrawerAction(
                    activity.getResources().getString(R.string.new_tag),
                    R.drawable.ic_add_24dp,
                    new Intent(activity, TagSettingsActivity.class),
                    NavigationDrawerFragment.REQUEST_NEW_LIST));
        }

        List<Filter> googleTaskFilters = filterProvider.getGoogleTaskFilters();
        if (!googleTaskFilters.isEmpty()) {
            String title = preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME);
            if (Strings.isNullOrEmpty(title)) {
                title = activity.getResources().getString(R.string.gtasks_GPr_header);
            }
            addSubMenu(title, googleTaskFilters, true);

            if (navigationDrawer) {
                add(new NavigationDrawerAction(
                        activity.getResources().getString(R.string.new_list),
                        R.drawable.ic_add_24dp,
                        new Intent(activity, GoogleTaskListSettingsActivity.class),
                        NavigationDrawerFragment.REQUEST_NEW_GTASK_LIST));
            }
        }

        List<Filter> calDAVFilters = filterProvider.getCalDAVFilters();
        if (!calDAVFilters.isEmpty()) {
            addSubMenu(R.string.CalDAV, calDAVFilters, false);

            if (navigationDrawer) {
                add(new NavigationDrawerAction(
                        activity.getResources().getString(R.string.add_account),
                        R.drawable.ic_add_24dp,
                        new Intent(activity, CalDAVSettingsActivity.class),
                        NavigationDrawerFragment.REQUEST_NEW_CALDAV_ACCOUNT));
            }
        }

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

        filterCounter.refreshFilterCounts(this::notifyDataSetChanged);
    }

    /* ======================================================================
     * ================================================================ views
     * ====================================================================== */

    private void populateItem(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if(filter == null) {
            return;
        }

        if (selected != null && selected.equals(filter)) {
            viewHolder.view.setBackgroundColor(getColor(activity, R.color.drawer_color_selected));
        } else {
            viewHolder.view.setBackgroundResource(0);
        }

        viewHolder.icon.setImageResource(filter.icon);
        viewHolder.icon.setColorFilter(filter.tint >= 0
                ? themeCache.getThemeColor(filter.tint).getPrimaryColor()
                : getColor(activity, R.color.text_primary));

        String title = filter.listingTitle;
        if(!title.equals(viewHolder.name.getText())) {
            viewHolder.name.setText(title);
        }

        int countInt = 0;
        if(filterCounter.containsKey(filter)) {
            countInt = filterCounter.get(filter);
            viewHolder.size.setText(locale.formatNumber(countInt));
        }
        viewHolder.size.setVisibility(countInt > 0 ? View.VISIBLE : View.GONE);
    }

    private void populateHeader(ViewHolder viewHolder) {
        FilterListItem filter = viewHolder.item;
        if (filter == null) {
            return;
        }

        viewHolder.name.setText(filter.listingTitle);
    }
}
