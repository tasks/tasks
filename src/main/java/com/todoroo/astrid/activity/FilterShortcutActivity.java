/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.InjectingListActivity;
import org.tasks.injection.Injector;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.ui.NavigationDrawerFragment;

import javax.inject.Inject;

public class FilterShortcutActivity extends InjectingListActivity {

    @Inject FilterCounter filterCounter;
    @Inject Injector injector;
    @Inject ActivityPreferences preferences;

    private FilterAdapter adapter = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        preferences.applyTheme();

        // Set the result to CANCELED unless a filter is selected.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.widget_config_activity);

        // set up ui
        adapter = new FilterAdapter(injector, filterCounter, this, getListView(),
                R.layout.filter_adapter_row, true);
        adapter.filterStyle = R.style.TextAppearance_FLA_Filter_Widget;
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);
    }

    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Filter filter = (Filter) adapter.getSelection();
            if (filter == null) {
                DialogUtilities.okDialog(FilterShortcutActivity.this, getString(R.string.FLA_no_filter_selected), null);
                return;
            }
            Intent shortcutIntent = ShortcutActivity.createIntent(filter);

            Bitmap bitmap = NavigationDrawerFragment.superImposeListIcon(FilterShortcutActivity.this);
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.title);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            setResult(RESULT_OK, intent);
            finish();
        }
   };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Filter item = adapter.getItem(position);
        adapter.setSelection(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.registerRecevier();
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.unregisterRecevier();
    }
}
