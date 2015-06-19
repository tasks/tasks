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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static android.text.TextUtils.isEmpty;

public class TagSettingsActivity extends InjectingAppCompatActivity {

    public static final String TOKEN_NEW_FILTER = "newFilter"; //$NON-NLS-1$
    public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; //$NON-NLS-1$

    private boolean isNewTag;
    private TagData tagData;

    @Inject TagService tagService;
    @Inject TagDataDao tagDataDao;
    @Inject ActivityPreferences preferences;
    @Inject MetadataDao metadataDao;

    @InjectView(R.id.tag_name) EditText tagName;
    @InjectView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyThemeAndStatusBarColor();
        setContentView(R.layout.tag_settings_activity);
        ButterKnife.inject(this);

        tagData = getIntent().getParcelableExtra(TagViewFragment.EXTRA_TAG_DATA);
        if (tagData == null) {
            isNewTag = true;
            tagData = new TagData();
            tagData.setUUID(UUIDHelper.newUUID());
        }

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            supportActionBar.setTitle(isNewTag ? getString(R.string.new_tag) : tagData.getName());
        }

        tagName.setText(tagData.getName());

        String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
        if (!isEmpty(autopopulateName)) {
            tagName.setText(autopopulateName);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
        }
    }

    private void save() {
        String oldName = tagData.getName();
        String newName = tagName.getText().toString().trim();

        if (isEmpty(newName)) {
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

            tagDataDao.persist(tagData);

            if (isNewTag) {
                setResult(RESULT_OK, new Intent().putExtra(TOKEN_NEW_FILTER,
                        TagFilterExposer.filterFromTagData(TagSettingsActivity.this, tagData)));
            } else {
                setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED).putExtra(TagViewFragment.EXTRA_TAG_UUID, tagData.getUuid()));
            }
        }

        finish();
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);
        super.finish();
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_settings_activity, menu);
        if (isNewTag) {
            menu.findItem(R.id.delete).setVisible(false);
        }
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
        new AlertDialog.Builder(this, R.style.Tasks_Dialog)
                .setMessage(getString(R.string.delete_tag_confirmation, tagData.getName()))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tagData != null) {
                            String uuid = tagData.getUuid();
                            metadataDao.deleteWhere(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)));
                            tagDataDao.delete(tagData.getId());
                            setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED).putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid));
                        }
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void discard() {
        String tagName = this.tagName.getText().toString().trim();
        if ((isNewTag && isEmpty(tagName)) ||
                (!isNewTag && tagData.getName().equals(tagName))) {
            finish();
        } else {
            new AlertDialog.Builder(this, R.style.Tasks_Dialog)
                    .setMessage(R.string.discard_changes)
                    .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        }
    }
}
