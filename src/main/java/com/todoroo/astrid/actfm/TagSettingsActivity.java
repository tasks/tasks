/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;

import org.tasks.R;
import org.tasks.helper.UUIDHelper;
import org.tasks.injection.InjectingActionBarActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class TagSettingsActivity extends InjectingActionBarActivity {

    public static final String TOKEN_NEW_FILTER = "newFilter"; //$NON-NLS-1$

    public static final String TOKEN_AUTOPOPULATE_MEMBERS = "autopopulateMembers"; //$NON-NLS-1$

    public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; //$NON-NLS-1$

    private TagData tagData;

    @Inject TagService tagService;
    @Inject TagDataDao tagDataDao;
    @Inject ActivityPreferences preferences;

    private EditText tagName;

    private boolean isNewTag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyTheme();
        setContentView(R.layout.tag_settings_activity);

        tagData = getIntent().getParcelableExtra(TagViewFragment.EXTRA_TAG_DATA);
        if (tagData == null) {
            isNewTag = true;
            tagData = new TagData();
            tagData.setUUID(UUIDHelper.newUUID());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setUpSettingsPage();
    }

    protected void setUpSettingsPage() {
        tagName = (EditText) findViewById(R.id.tag_name);

        refreshSettingsPage();

        String autopopulateMembers = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_MEMBERS);
        if (!TextUtils.isEmpty(autopopulateMembers)) {
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_MEMBERS);
        }

        String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
        if (!TextUtils.isEmpty(autopopulateName)) {
            tagName.setText(autopopulateName);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
        }
    }

    private void saveSettings() {
        String oldName = tagData.getName();
        String newName = tagName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            return;
        }

        boolean nameChanged = !oldName.equals(newName);
        if (nameChanged) {
            if (oldName.equalsIgnoreCase(newName)) { // Change the capitalization of a list manually
                tagData.setName(newName);
                tagService.rename(tagData.getUuid(), newName);
            } else { // Rename list--check for existing name
                newName = tagService.getTagWithCase(newName);
                tagName.setText(newName);
                if (!newName.equals(oldName)) {
                    tagData.setName(newName);
                    tagService.rename(tagData.getUuid(), newName);
                }
            }
        }

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);

        tagDataDao.persist(tagData);

        if (isNewTag) {
            setResult(RESULT_OK, new Intent().putExtra(TOKEN_NEW_FILTER,
                    TagFilterExposer.filterFromTagData(TagSettingsActivity.this, tagData)));
        } else {
            setResult(RESULT_OK);
        }

        refreshSettingsPage();
        finish();
    }

    @Override
    public void finish() {
        finishWithAnimation(true);
    }

    private void finishWithAnimation(boolean backAnimation) {
        super.finish();
        if (backAnimation) {
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
        }
    }

    private void refreshSettingsPage() {
        tagName.setText(tagData.getName());
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            if (isNewTag) {
                ab.setTitle(getString(R.string.tag_new_list));
            } else {
                ab.setTitle(getString(R.string.tag_settings_title));
            }
        } else {
            if (isNewTag) {
                setTitle(getString(R.string.tag_new_list));
            } else {
                setTitle(getString(R.string.tag_settings_title));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_settings_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (tagName.getText().length() == 0) {
            finish();
        } else {
            saveSettings();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.menu_discard:
            finish();
            break;
        case R.id.menu_save:
            saveSettings();
            break;
        case android.R.id.home:
            saveSettings();
            if (!isFinishing()) {
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
