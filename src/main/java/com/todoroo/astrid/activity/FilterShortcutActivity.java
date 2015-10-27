/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingListActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class FilterShortcutActivity extends InjectingListActivity {

    @Inject FilterCounter filterCounter;
    @Inject ActivityPreferences preferences;
    @Inject FilterProvider filterProvider;
    @Inject @ForApplication Context context;
    @Inject DialogBuilder dialogBuilder;

    private FilterAdapter adapter = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        preferences.applyLightStatusBarColor();
        // Set the view layout resource to use.
        setContentView(R.layout.filter_shortcut_activity);

        // set up ui
        adapter = new FilterAdapter(filterProvider, filterCounter, this, getListView(), false);
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);
    }

    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Filter filter = (Filter) adapter.getSelection();
            if (filter == null) {
                dialogBuilder.newMessageDialog(R.string.FLA_no_filter_selected)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            Intent shortcutIntent = ShortcutActivity.createIntent(context, filter);

            Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, filter.listingTitle);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            setResult(RESULT_OK, intent);
            finish();
        }
   };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FilterListItem item = adapter.getItem(position);
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
