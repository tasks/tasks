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
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static org.tasks.dialogs.SupportThemePickerDialog.newSupportThemePickerDialog;

public class TagSettingsActivity extends ThemedInjectingAppCompatActivity implements ThemePickerDialog.ThemePickerCallback, PurchaseHelperCallback, Toolbar.OnMenuItemClickListener {

    private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";

    private static final int REQUEST_PURCHASE = 10109;

    public static final String TOKEN_NEW_FILTER = "newFilter"; //$NON-NLS-1$
    public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; //$NON-NLS-1$
    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$
    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    private boolean isNewTag;
    private TagData tagData;
    private int selectedTheme;

    @Inject TagService tagService;
    @Inject TagDataDao tagDataDao;
    @Inject MetadataDao metadataDao;
    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;
    @Inject ThemeCache themeCache;
    @Inject PurchaseHelper purchaseHelper;
    @Inject ThemeColor themeColor;
    @Inject Tracker tracker;

    @BindView(R.id.tag_name) EditText tagName;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.theme) TextView themeName;
    @BindView(R.id.clear) View clear;

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

        final boolean backButtonSavesTask = preferences.backButtonSavesTask();
        toolbar.setTitle(isNewTag ? getString(R.string.new_tag) : tagData.getName());
        toolbar.setNavigationIcon(getResources().getDrawable(
                backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backButtonSavesTask) {
                    discard();
                } else {
                    save();
                }
            }
        });
        toolbar.inflateMenu(R.menu.tag_settings_activity);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.showOverflowMenu();

        tagName.setText(tagData.getName());

        String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
        if (!isEmpty(autopopulateName)) {
            tagName.setText(autopopulateName);
            getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
        } else if (isNewTag) {
            toolbar.getMenu().findItem(R.id.delete).setVisible(false);
            tagName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(tagName, InputMethodManager.SHOW_IMPLICIT);
        }

        selectedTheme = tagData.getColor();
        updateTheme();
    }

    @OnClick(R.id.theme_row)
    protected void showThemePicker() {
        newSupportThemePickerDialog(ThemePickerDialog.ColorPalette.COLORS)
                .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
    }

    @OnClick(R.id.clear)
    void clearColor() {
        selectedTheme = -1;
        updateTheme();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private String getNewName() {
        return tagName.getText().toString().trim();
    }

    private boolean clashes(String newName) {
        TagData existing = tagDataDao.getTagByName(newName, TagData.PROPERTIES);
        return existing != null && tagData.getId() != existing.getId();
    }

    private void save() {
        String newName = getNewName();

        if (isEmpty(newName)) {
            Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (clashes(newName)) {
            Toast.makeText(this, R.string.tag_already_exists, Toast.LENGTH_LONG).show();
            return;
        }

        if (isNewTag) {
            tagData.setName(newName);
            tagData.setColor(selectedTheme);
            tagDataDao.persist(tagData);
            setResult(RESULT_OK, new Intent().putExtra(TOKEN_NEW_FILTER, TagFilterExposer.filterFromTag(tagData)));
        } else if (hasChanges()) {
            tagData.setName(newName);
            tagData.setColor(selectedTheme);
            tagService.rename(tagData.getUuid(), newName);
            tagDataDao.persist(tagData);
            Metadata m = new Metadata();
            m.setValue(TaskToTagMetadata.TAG_NAME, newName);
            metadataDao.update(Criterion.and(
                    MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                    TaskToTagMetadata.TAG_UUID.eq(tagData.getUUID())), m);
            setResult(RESULT_OK, new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED).putExtra(EXTRA_TAG_UUID, tagData.getUuid()));
        }

        finish();
    }

    private boolean hasChanges() {
        if (isNewTag) {
            return selectedTheme >= 0 || !isEmpty(getNewName());
        }
        return !(selectedTheme == tagData.getColor() && getNewName().equals(tagData.getName()));
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(tagName.getWindowToken(), 0);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (preferences.backButtonSavesTask()) {
            save();
        } else {
            discard();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
        if (!hasChanges()) {
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

    @Override
    public void themePicked(ThemePickerDialog.ColorPalette palette, int index) {
        tracker.reportEvent(Tracking.Events.SET_TAG_COLOR, Integer.toString(index));
        selectedTheme = index;
        updateTheme();
    }

    private void updateTheme() {
        ThemeColor color;
        if (selectedTheme < 0) {
            color = themeColor;
            themeName.setText(R.string.none);
            clear.setVisibility(View.GONE);

        } else {
            color = themeCache.getThemeColor(selectedTheme);
            themeName.setText(color.getName());
            clear.setVisibility(View.VISIBLE);
        }
        color.apply(toolbar);
        color.applyStatusBarColor(this);
    }

    @Override
    public void initiateThemePurchase() {
        purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_themes), getString(R.string.p_purchased_themes), REQUEST_PURCHASE, this);
    }

    @Override
    public void purchaseCompleted(boolean success, final String sku) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getString(R.string.sku_themes).equals(sku)) {
                    showThemePicker();
                } else {
                    Timber.d("Unhandled sku: %s", sku);
                }
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                deleteTag();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
