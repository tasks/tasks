/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import greendroid.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterCategoryWithNewButton;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;

public class FilterAdapter extends BaseExpandableListAdapter {

    private static List<ResolveInfo> filterExposerList;

    static {
        // query astrids AndroidManifest.xml for all registered default-receivers to expose filters
        PackageManager pm = ContextManager.getContext().getPackageManager();
        filterExposerList = pm.queryBroadcastReceivers(
                new Intent(AstridApiConstants.BROADCAST_REQUEST_FILTERS),
                PackageManager.MATCH_DEFAULT_ONLY);
    }

    // --- style constants

    public int filterStyle = R.style.TextAppearance_FLA_Filter;
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
    private final BladeFilterReceiver bladeFilterReceiver = new BladeFilterReceiver();

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
    private final Drawable headerBackground;

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

        TypedArray a = activity.obtainStyledAttributes(new int[] { R.attr.asFilterHeaderBackground });
        headerBackground = a.getDrawable(0);
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
        notifyDataSetInvalidated();
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
            viewHolder.urlImage = (AsyncImageView)convertView.findViewById(R.id.url_image);
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.activity = (TextView)convertView.findViewById(R.id.activity);
            viewHolder.selected = (ImageView)convertView.findViewById(R.id.selected);
            viewHolder.size = (TextView)convertView.findViewById(R.id.size);
            viewHolder.decoration = null;
            convertView.setTag(viewHolder);
        }
        return convertView;
    }

    public static class ViewHolder {
        public FilterListItem item;
        public ImageView expander;
        public ImageView icon;
        public AsyncImageView urlImage;
        public TextView name;
        public TextView activity;
        public TextView size;
        public ImageView selected;
        public View view;
        public View decoration;
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
        populateView(viewHolder, false);

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
        populateView(viewHolder, isExpanded);
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
            if (Constants.DEBUG_BLADE) {
                // emulate the bug in the zte blade to not load the parcelable-array due to classloader-problems
                try {
                    // normally caused by (with another message of course):
                    // final Parcelable[] filters = extras.getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                    throw new BadParcelableException(new ClassNotFoundException("ZTE Blade debug test!"));
                } catch (Exception e) {
                    Log.e("receive-filter-" +  //$NON-NLS-1$
                            intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                            e.toString(), e);
                }

//                setResultCode(Activity.RESULT_OK);
                return;
            }

            try {
                Bundle extras = intent.getExtras();
                extras.setClassLoader(FilterListHeader.class.getClassLoader());
                final Parcelable[] filters = extras.getParcelableArray(AstridApiConstants.EXTRAS_RESPONSE);
                populateFiltersToAdapter(filters);
            } catch (Exception e) {
                Log.e("receive-filter-" +  //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                        e.toString(), e);
            }
        }

        protected void populateFiltersToAdapter(final Parcelable[] filters) {
            if (filters == null)
                return;

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
        }
    }

    /**
     * Receiver which gets called after the FilterReceiver
     * and checks if the filters are populated.
     * If they aren't (e.g. due to the bug in the ZTE Blade's Parcelable-system throwing a
     * ClassNotFoundExeption), the filters are fetched manually.
     *
     * @author Arne Jans <arne@astrid.com>
     *
     */
    public class BladeFilterReceiver extends FilterReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getGroupCount() == 0 && filterExposerList != null && filterExposerList.size()>0) {
                try {
                    for (ResolveInfo filterExposerInfo : filterExposerList) {
                        Log.d("BladeFilterReceiver", filterExposerInfo.toString());
                        String className = filterExposerInfo.activityInfo.name;
                        AstridFilterExposer filterExposer = null;
                            filterExposer = (AstridFilterExposer) Class.forName(className, true, FilterAdapter.class.getClassLoader()).newInstance();

                        if (filterExposer != null) {
                            populateFiltersToAdapter(filterExposer.getFilters());
                        }
                    }
                } catch (Exception e) {
                    Log.e("receive-bladefilter-" +  //$NON-NLS-1$
                            intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                            e.toString(), e);
                }
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
//        activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        // the bladeFilterReceiver will be called after the usual FilterReceiver has finished (to handle the empty list)
        activity.sendOrderedBroadcast(broadcastIntent,
                AstridApiConstants.PERMISSION_READ,
                bladeFilterReceiver,
                null,
                Activity.RESULT_OK,
                null,
                null);
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void registerRecevier() {
        activity.registerReceiver(filterReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS));
        activity.registerReceiver(bladeFilterReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_FILTERS));
        if(getGroupCount() == 0)
            getLists();
    }

    /**
     * Call this method from your activity's onResume() method
     */
    public void unregisterRecevier() {
        activity.unregisterReceiver(filterReceiver);
        activity.unregisterReceiver(bladeFilterReceiver);
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

    public void populateView(ViewHolder viewHolder, boolean isExpanded) {
        FilterListItem filter = viewHolder.item;
        if(filter == null)
            return;

        viewHolder.view.setBackgroundResource(0);

        if(viewHolder.decoration != null) {
            ((ViewGroup)viewHolder.view).removeView(viewHolder.decoration);
            viewHolder.decoration = null;
        }

        if(viewHolder.item instanceof FilterListHeader || viewHolder.item instanceof FilterCategory) {
            viewHolder.name.setTextAppearance(activity, headerStyle);
            viewHolder.name.setShadowLayer(1, 1, 1, Color.BLACK);
            viewHolder.view.setBackgroundDrawable(headerBackground);
            viewHolder.view.setPadding((int) (7 * metrics.density), 5, 0, 5);
            viewHolder.view.getLayoutParams().height = (int) (40 * metrics.density);
        } else {
            viewHolder.name.setTextAppearance(activity, filterStyle);
            viewHolder.name.setShadowLayer(0, 0, 0, 0);
            viewHolder.view.setPadding((int) (7 * metrics.density), 8, 0, 8);
            viewHolder.view.getLayoutParams().height = (int) (58 * metrics.density);
        }

        if(viewHolder.item instanceof FilterCategory) {
            viewHolder.expander.setVisibility(View.VISIBLE);
            viewHolder.expander.setImageResource(isExpanded ?
                    R.drawable.expander_ic_maximized : R.drawable.expander_ic_minimized);
        } else
            viewHolder.expander.setVisibility(View.GONE);

        // update with filter attributes (listing icon, url, update text, size)

        viewHolder.urlImage.setVisibility(View.GONE);
        viewHolder.activity.setVisibility(View.GONE);
        viewHolder.icon.setVisibility(View.GONE);

        if(filter.listingIcon != null) {
            viewHolder.icon.setVisibility(View.VISIBLE);
            viewHolder.icon.setImageBitmap(filter.listingIcon);
        }

        // title / size
        if(filter.listingTitle.matches(".* \\(\\d+\\)$")) { //$NON-NLS-1$
            viewHolder.size.setVisibility(View.VISIBLE);
            viewHolder.size.setText(filter.listingTitle.substring(filter.listingTitle.lastIndexOf('(') + 1,
                    filter.listingTitle.length() - 1));
            viewHolder.name.setText(filter.listingTitle.substring(0, filter.listingTitle.lastIndexOf(' ')));
        } else {
            viewHolder.name.setText(filter.listingTitle);
            viewHolder.size.setVisibility(View.GONE);
        }

        viewHolder.name.getLayoutParams().height = (int) (58 * metrics.density);
        if(filter instanceof FilterWithUpdate) {
            viewHolder.urlImage.setVisibility(View.VISIBLE);
            viewHolder.urlImage.setDefaultImageResource(R.drawable.gl_list);
            viewHolder.urlImage.setUrl(((FilterWithUpdate)filter).imageUrl);
            if(!TextUtils.isEmpty(((FilterWithUpdate)filter).updateText)) {
                viewHolder.activity.setText(((FilterWithUpdate)filter).updateText);
                viewHolder.name.getLayoutParams().height = (int) (25 * metrics.density);
                viewHolder.activity.setVisibility(View.VISIBLE);
            }
        }

        if(filter.color != 0)
            viewHolder.name.setTextColor(filter.color);

        // selection
        if(selection == viewHolder.item) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.view.setBackgroundColor(Color.rgb(128, 230, 0));
        } else
            viewHolder.selected.setVisibility(View.GONE);

        if(filter instanceof FilterCategoryWithNewButton)
            setupCustomHeader(viewHolder, (FilterCategoryWithNewButton) filter);
    }

    private void setupCustomHeader(ViewHolder viewHolder, final FilterCategoryWithNewButton filter) {
        Button add = new Button(activity);
        add.setBackgroundResource(R.drawable.filter_btn_background);
        add.setCompoundDrawablesWithIntrinsicBounds(R.drawable.filter_new,0,0,0);
        add.setTextColor(Color.WHITE);
        add.setShadowLayer(1, 1, 1, Color.BLACK);
        add.setText(filter.label);
        add.setFocusable(false);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                (int)(32 * metrics.density));
        lp.rightMargin = (int) (4 * metrics.density);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        add.setLayoutParams(lp);
        ((ViewGroup)viewHolder.view).addView(add);
        viewHolder.decoration = add;

        add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            filter.intent.send(FilterListActivity.REQUEST_NEW_BUTTON, new PendingIntent.OnFinished() {
                                @Override
                                public void onSendFinished(PendingIntent pendingIntent, Intent intent,
                                        int resultCode, String resultData, Bundle resultExtras) {
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            clear();
                                        }
                                    });
                                }
                            }, null);
                        } catch (CanceledException e) {
                            // do nothing
                        }
                    }
                });
    }

}
