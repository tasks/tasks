/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.dao.StoreObjectDao;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static android.text.TextUtils.isEmpty;

public class FilterSettingsActivity extends InjectingAppCompatActivity {

    public static final String TOKEN_FILTER = "token_filter";

    private CustomFilter filter;

    @Inject ActivityPreferences preferences;
    @Inject StoreObjectDao storeObjectDao;
    @Inject DialogBuilder dialogBuilder;

    @InjectView(R.id.tag_name) EditText filterName;
    @InjectView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyThemeAndStatusBarColor();
        setContentView(R.layout.filter_settings_activity);
        ButterKnife.inject(this);

        filter = getIntent().getParcelableExtra(TOKEN_FILTER);

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            supportActionBar.setTitle(filter.listingTitle);
        }

        filterName.setText(filter.listingTitle);
    }

    private void save() {
        String oldName = filter.listingTitle;
        String newName = filterName.getText().toString().trim();

        if (isEmpty(newName)) {
            return;
        }

        boolean nameChanged = !oldName.equals(newName);
        if (nameChanged) {
            filter.listingTitle = newName;
            storeObjectDao.update(filter.toStoreObject());
            setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_FILTER_RENAMED).putExtra(TOKEN_FILTER, filter));
        }

        finish();
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(filterName.getWindowToken(), 0);
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_settings_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        discard();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                discard();
                break;
            case R.id.menu_save:
                save();
                break;
            case R.id.delete:
                deleteTag();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteTag() {
        dialogBuilder.newMessageDialog(R.string.delete_tag_confirmation, filter.listingTitle)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storeObjectDao.delete(filter.getId());
                        setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_FILTER_DELETED).putExtra(TOKEN_FILTER, filter));
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void discard() {
        String tagName = this.filterName.getText().toString().trim();
        if (filter.listingTitle.equals(tagName)) {
            finish();
        } else {
            dialogBuilder.newMessageDialog(R.string.discard_changes)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }
}
