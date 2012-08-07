/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.AddOnActivity;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.twofortyfouram.SharedResources;

/**
 * Activity to edit alerts from Locale
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class LocaleEditAlerts extends ListActivity {

    // --- locale constants

    /** key name for filter title in bundle */
    @SuppressWarnings("nls")
    public static final String KEY_FILTER_TITLE = "title";

    /** key name for filter SQL in bundle */
    @SuppressWarnings("nls")
    public static final String KEY_SQL = "sql";

    /** key name for filter content-values in bundle */
    @SuppressWarnings("nls")
    public static final String KEY_VALUES = "val";

    /** key name for interval (integer, # of seconds) */
    @SuppressWarnings("nls")
    public static final String KEY_INTERVAL = "interval";

    // --- activity constants

    /**
     * Indices for interval setting
     */
    public static final int INTERVAL_ONE_HOUR = 0;
    public static final int INTERVAL_SIX_HOURS = 1;
    public static final int INTERVAL_TWELVE_HOURS = 2;
    public static final int INTERVAL_ONE_DAY = 3;
    public static final int INTERVAL_THREE_DAYS = 4;
    public static final int INTERVAL_ONE_WEEK = 5;

    /**
     * Intervals in seconds
     */
    public static final int[] INTERVALS = new int[] {
        3600, 6 * 3600, 12 * 3600, 24 * 3600, 3 * 24 * 3600, 7 * 24 * 3600
    };

    /**
     * Menu ID of the save item.
     */
    private static final int MENU_SAVE = 1;

    /**
     * Menu ID of the don't save item.
     */
    private static final int MENU_DONT_SAVE = 2;

    // --- implementation

    FilterAdapter adapter = null;
    Spinner interval = null;

    /**
     * Flag boolean that can only be set to true via the "Don't Save" menu item in {@link #onMenuItemSelected(int, MenuItem)}. If
     * true, then this {@code Activity} should return {@link Activity#RESULT_CANCELED} in {@link #finish()}.
     * <p>
     * There is no need to save/restore this field's state when the {@code Activity} is paused.
     */
    private boolean isCancelled = false;
    private boolean isRemoved = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.locale_edit_alerts);

        ContextManager.setContext(this);

        /*
         * Locale guarantees that the breadcrumb string will be present, but checking for null anyway makes your Activity more
         * robust and re-usable
         */
        final String breadcrumbString = getIntent().getStringExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
        if (breadcrumbString != null)
            setTitle(String.format("%s%s%s", breadcrumbString, com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR, //$NON-NLS-1$
                    getString(R.string.locale_edit_alerts_title)));

        /*
         * Load the Locale background frame from Locale
         */
//        ((LinearLayout) findViewById(R.id.frame)).setBackgroundDrawable(
//                SharedResources.getDrawableResource(getPackageManager(),
//                        SharedResources.DRAWABLE_LOCALE_BORDER));

        // set up UI components
        interval = (Spinner) findViewById(R.id.intervalSpinner);
        interval.setSelection(INTERVAL_ONE_DAY);
        String selectionToMatch = null;

        try {
            /*
             * if savedInstanceState == null, then we are entering the Activity directly from Locale and we need to check whether the
             * Intent has forwarded a Bundle extra (e.g. whether we editing an old setting or creating a new one)
             */
            if (savedInstanceState == null)
            {
                final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE);

                /*
                 * the forwardedBundle would be null if this was a new setting
                 */
                if (forwardedBundle != null)
                {
                    final int intervalValue = forwardedBundle.getInt(KEY_INTERVAL, INTERVALS[interval.getSelectedItemPosition()]);
                    for(int i = 0; i < INTERVALS.length; i++) {
                        if(intervalValue == INTERVALS[i]) {
                            interval.setSelection(i);
                            break;
                        }
                    }
                    selectionToMatch = forwardedBundle.getString(KEY_SQL);
                }
            }
        } catch (Exception e) {
            selectionToMatch = null;
            Log.e("astrid-locale", "Error loading bundle", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // if we match a selection, make it selected
        final String finalSelection = selectionToMatch;
        adapter = new FilterAdapter(this, getListView(), R.layout.filter_adapter_row, true) {
            @Override
            public void onReceiveFilter(FilterListItem item) {
                if(adapter.getSelection() != null || finalSelection == null)
                    return;
                if(item instanceof Filter) {
                    if(finalSelection.equals(((Filter)item).getSqlQuery()))
                        adapter.setSelection(item);
                } else if(item instanceof FilterCategory) {
                    Filter[] filters = ((FilterCategory)item).children;
                    if(filters == null)
                        return;
                    for(Filter filter : filters)
                        if(finalSelection.equals(filter.getSqlQuery())) {
                            adapter.setSelection(filter);
                            break;
                        }
                }
            }
        };
        adapter.filterStyle = R.style.TextAppearance_LEA_Filter;
        adapter.headerStyle = R.style.TextAppearance_LEA_Header;
        setListAdapter(adapter);

        // check for plugin
        if(!PluginServices.getAddOnService().hasLocalePlugin()) {
            isRemoved = true;
            new AlertDialog.Builder(this)
            .setTitle(R.string.DLG_information_title)
            .setMessage(R.string.locale_plugin_required)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok,
                    AddOnActivity.createAddOnClicker(LocaleEditAlerts.this, true))
            .show();
            StatisticsService.reportEvent(StatisticsConstants.LOCALE_EDIT_ALERTS_NO_PLUGIN);
        } else {
            StatisticsService.reportEvent(StatisticsConstants.LOCALE_EDIT_ALERTS);
        }
    }

    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        Filter item = adapter.getItem(position);
        adapter.setSelection(item);
    }

    /**
     * Called when the {@code Activity} is being terminated. This method determines the state of the {@code Activity} and what
     * sort of result should be returned to <i>Locale</i>.
     */
    @Override
    public void finish()
    {
        if(isRemoved)
            setResult(com.twofortyfouram.Intent.RESULT_REMOVE);
        else if (isCancelled)
            setResult(RESULT_CANCELED);
        else
        {
            final FilterListItem selected = adapter.getSelection();
            final int intervalIndex = interval.getSelectedItemPosition();

            if (selected == null)
            {
                /*
                 * If nothing is selected, return as if user had canceled
                 */
                setResult(RESULT_CANCELED);
            }
            else
            {
                /*
                 * This is the return Intent, into which we'll put all the required extras
                 */
                final Intent returnIntent = new Intent();

                /*
                 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything
                 * placed in this bundle must be available to Locale's class loader. So storing String, int, and other basic
                 * objects will work just fine. You cannot store an object that only exists in your project, as Locale will be
                 * unable to serialize it.
                 */
                final Bundle storeAndForwardExtras = new Bundle();

                Filter filterItem = (Filter) selected;
                storeAndForwardExtras.putString(KEY_FILTER_TITLE, filterItem.title);
                storeAndForwardExtras.putString(KEY_SQL, filterItem.getSqlQuery());
                if(filterItem.valuesForNewTasks != null)
                    storeAndForwardExtras.putString(KEY_VALUES, AndroidUtilities.contentValuesToSerializedString(filterItem.valuesForNewTasks));
                storeAndForwardExtras.putInt(KEY_INTERVAL, INTERVALS[intervalIndex]);

                returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE, storeAndForwardExtras);

                /*
                 * This is the blurb concisely describing what your setting's state is. This is simply used for display in the UI.
                 */
                if (filterItem.title != null && filterItem.title.length() > com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH)
                    returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, filterItem.title.substring(0, com.twofortyfouram.Intent.MAXIMUM_BLURB_LENGTH));
                else
                    returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB, filterItem.title);

                setResult(RESULT_OK, returnIntent);
            }
        }

        super.finish();
    }

    // --- boring stuff

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
        adapter.registerRecevier();
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
        adapter.unregisterRecevier();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        final PackageManager manager = getPackageManager();

        /*
         * We are dynamically loading resources from Locale's APK. This will only work if Locale is actually installed
         */
        menu.add(0, MENU_DONT_SAVE, 0, SharedResources.getTextResource(manager, SharedResources.STRING_MENU_DONTSAVE))
            .setIcon(SharedResources.getDrawableResource(manager, SharedResources.DRAWABLE_MENU_DONTSAVE)).getItemId();

        menu.add(0, MENU_SAVE, 0, SharedResources.getTextResource(manager, SharedResources.STRING_MENU_SAVE))
            .setIcon(SharedResources.getDrawableResource(manager, SharedResources.DRAWABLE_MENU_SAVE)).getItemId();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_SAVE:
            {
                finish();
                return true;
            }
            case MENU_DONT_SAVE:
            {
                isCancelled = true;
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

}
