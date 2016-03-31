/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.ui.MenuColorizer;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.text.TextUtils.isEmpty;

public class TagSettingsActivity extends ThemedInjectingAppCompatActivity {

    public static final String TOKEN_NEW_FILTER = "newFilter"; //$NON-NLS-1$
    public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; //$NON-NLS-1$
    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$
    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    private boolean isNewTag;
    private TagData tagData;

    @Inject TagService tagService;
    @Inject TagDataDao tagDataDao;
    @Inject MetadataDao metadataDao;
    @Inject DialogBuilder dialogBuilder;

    @Bind(R.id.tag_name) EditText tagName;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.tag_error) TextView tagError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tag_settings_activity);
        ButterKnife.bind(this);

        tagData = getIntent().getParcelableExtra(EXTRA_TAG_DATA);
        if (tagData == null) {
            isNewTag = true;
            tagData = new TagData();
            tagData.setUUID(UUIDHelper.newUUID());
        }

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            Drawable drawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_close_24dp));
            DrawableCompat.setTint(drawable, getResources().getColor(android.R.color.white));
            supportActionBar.setHomeAsUpIndicator(drawable);
            supportActionBar.setTitle(isNewTag ? getString(R.string.new_tag) : tagData.getName());
        }

        tagName.setText(tagData.getName());

        tagName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tagError.setVisibility(clashes() ? View.VISIBLE : View.GONE);
            }
        });

        String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
        if (!isEmpty(autopopulateName)) {
            tagName.setText(autopopulateName);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private String getNewName() {
        return tagName.getText().toString().trim();
    }

    private boolean clashes() {
        String newName = getNewName();
        TagData existing = tagDataDao.getTagByName(newName, TagData.PROPERTIES);
        return existing != null && tagData.getId() != existing.getId();
    }

    private void save() {
        String oldName = tagData.getName();
        String newName = getNewName();

        if (isEmpty(newName)) {
            return;
        }

        if (clashes()) {
            return;
        }

        if (isNewTag) {
            tagData.setName(newName);
            tagDataDao.persist(tagData);
            setResult(RESULT_OK, new Intent().putExtra(TOKEN_NEW_FILTER, TagFilterExposer.filterFromTag(tagData)));
        } else if (!oldName.equals(newName)) {
            tagData.setName(newName);
            tagService.rename(tagData.getUuid(), newName);
            tagDataDao.persist(tagData);
            setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED).putExtra(EXTRA_TAG_UUID, tagData.getUuid()));
        }

        finish();
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);
        super.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tag_settings_activity, menu);
        MenuColorizer.colorMenu(this, menu, getResources().getColor(android.R.color.white));
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
        dialogBuilder.newMessageDialog(R.string.delete_tag_confirmation, tagData.getName())
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tagData != null) {
                            String uuid = tagData.getUuid();
                            metadataDao.deleteWhere(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)));
                            tagDataDao.delete(tagData.getId());
                            setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED).putExtra(EXTRA_TAG_UUID, uuid));
                        }
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void discard() {
        String tagName = getNewName();
        if ((isNewTag && isEmpty(tagName)) ||
                (!isNewTag && tagData.getName().equals(tagName))) {
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
