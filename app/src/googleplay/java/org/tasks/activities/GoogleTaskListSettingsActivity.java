package org.tasks.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.CreateListDialog;
import org.tasks.gtasks.DeleteListDialog;
import org.tasks.gtasks.RenameListDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.MenuColorizer;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;

import static android.text.TextUtils.isEmpty;
import static org.tasks.gtasks.CreateListDialog.newCreateListDialog;
import static org.tasks.gtasks.DeleteListDialog.newDeleteListDialog;
import static org.tasks.gtasks.RenameListDialog.newRenameListDialog;

public class GoogleTaskListSettingsActivity extends ThemedInjectingAppCompatActivity
        implements Toolbar.OnMenuItemClickListener, CreateListDialog.CreateListDialogCallback,
        DeleteListDialog.DeleteListDialogCallback, RenameListDialog.RenameListDialogCallback {

    private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
    private static final String FRAG_TAG_CREATE_LIST_DIALOG = "frag_tag_create_list_dialog";
    private static final String FRAG_TAG_DELETE_LIST_DIALOG = "frag_tag_delete_list_dialog";
    private static final String FRAG_TAG_RENAME_LIST_DIALOG = "frag_tag_rename_list_dialog";
    private static final int REQUEST_COLOR_PICKER = 10109;

    public static final String EXTRA_STORE_DATA = "extra_store_data";
    public static final String ACTION_DELETED = "action_deleted";
    public static final String ACTION_RELOAD = "action_reload";

    private boolean isNewList;
    private GtasksList gtasksList;
    private int selectedTheme;

    @Inject StoreObjectDao storeObjectDao;
    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;
    @Inject GtasksListService gtasksListService;
    @Inject Tracker tracker;
    @Inject ThemeCache themeCache;
    @Inject ThemeColor themeColor;

    @BindView(R.id.name) TextInputEditText name;
    @BindView(R.id.color) TextInputEditText color;
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_google_task_list_settings);
        ButterKnife.bind(this);

        StoreObject storeObject = getIntent().getParcelableExtra(EXTRA_STORE_DATA);
        if (storeObject == null) {
            isNewList = true;
            storeObject = new StoreObject();
            storeObject.setType(GtasksList.TYPE);
        }
        gtasksList = new GtasksList(storeObject);
        if (savedInstanceState == null) {
            selectedTheme = gtasksList.getColor();
        } else {
            selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
        }

        final boolean backButtonSavesTask = preferences.backButtonSavesTask();
        toolbar.setTitle(isNewList ? getString(R.string.new_list) : gtasksList.getName());
        toolbar.setNavigationIcon(ContextCompat.getDrawable(this,
                backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
        toolbar.setNavigationOnClickListener(v -> {
            if (backButtonSavesTask) {
                discard();
            } else {
                save();
            }
        });
        toolbar.inflateMenu(R.menu.menu_tag_settings);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.showOverflowMenu();

        color.setInputType(InputType.TYPE_NULL);

        MenuColorizer.colorToolbar(this, toolbar);

        if (isNewList) {
            toolbar.getMenu().findItem(R.id.delete).setVisible(false);
            name.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
        } else {
            name.setText(gtasksList.getName());
        }

        updateTheme();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_SELECTED_THEME, selectedTheme);
    }

    @OnFocusChange(R.id.color)
    void onFocusChange(boolean focused) {
        if (focused) {
            color.clearFocus();
            showThemePicker();
        }
    }

    @OnClick(R.id.color)
    protected void showThemePicker() {
        Intent intent = new Intent(GoogleTaskListSettingsActivity.this, ColorPickerActivity.class);
        intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerDialog.ColorPalette.COLORS);
        intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
        startActivityForResult(intent, REQUEST_COLOR_PICKER);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    private void save() {
        String newName = getNewName();

        if (isEmpty(newName)) {
            Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (isNewList) {
            newCreateListDialog(newName)
                    .show(getSupportFragmentManager(), FRAG_TAG_CREATE_LIST_DIALOG);
        } else if (nameChanged()) {
            newRenameListDialog(gtasksList.getRemoteId(), newName)
                    .show(getSupportFragmentManager(), FRAG_TAG_RENAME_LIST_DIALOG);
        } else {
            if (colorChanged()) {
                gtasksList.setColor(selectedTheme);
                storeObjectDao.persist(gtasksList);
                setResult(RESULT_OK, new Intent(ACTION_RELOAD).putExtra(TaskListActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
            }
            finish();
        }
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
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

    private void deleteTag() {
        dialogBuilder.newMessageDialog(R.string.delete_tag_confirmation, gtasksList.getName())
                .setPositiveButton(R.string.delete, (dialog, which) -> newDeleteListDialog(gtasksList.getRemoteId())
                        .show(getSupportFragmentManager(), FRAG_TAG_DELETE_LIST_DIALOG))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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

    private void discard() {
        if (hasChanges()) {
            dialogBuilder.newMessageDialog(R.string.discard_changes)
                    .setPositiveButton(R.string.keep_editing, null)
                    .setNegativeButton(R.string.discard, (dialog, which) -> finish())
                    .show();
        } else {
            finish();
        }
    }

    private String getNewName() {
        return name.getText().toString().trim();
    }

    private boolean hasChanges() {
        if (isNewList) {
            return selectedTheme >= 0 || !isEmpty(getNewName());
        }
        return colorChanged() || nameChanged();
    }

    private boolean colorChanged() {
        return selectedTheme != gtasksList.getColor();
    }

    private boolean nameChanged() {
        return !getNewName().equals(gtasksList.getName());
    }

    @Override
    public void onListCreated(TaskList taskList) {
        tracker.reportEvent(Tracking.Events.GTASK_NEW_LIST);
        gtasksList = new GtasksList(taskList.getId());
        gtasksList.setName(taskList.getTitle());
        gtasksList.setColor(selectedTheme);
        storeObjectDao.persist(gtasksList);
        setResult(RESULT_OK, new Intent().putExtra(TaskListActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
        finish();
    }

    @Override
    public void onListDeleted() {
        tracker.reportEvent(Tracking.Events.GTASK_DELETE_LIST);
        gtasksListService.deleteList(gtasksList);
        setResult(RESULT_OK, new Intent(ACTION_DELETED));
        finish();
    }

    @Override
    public void onListRenamed(TaskList taskList) {
        tracker.reportEvent(Tracking.Events.GTASK_RENAME_LIST);
        gtasksList.setName(taskList.getTitle());
        gtasksList.setColor(selectedTheme);
        storeObjectDao.persist(gtasksList);
        setResult(RESULT_OK, new Intent(ACTION_RELOAD).putExtra(TaskListActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COLOR_PICKER) {
            if (resultCode == RESULT_OK) {
                int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
                tracker.reportEvent(Tracking.Events.GTASK_SET_COLOR, Integer.toString(index));
                selectedTheme = index;
                updateTheme();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void requestFailed() {
        Toast.makeText(this, R.string.gtasks_GLA_errorIOAuth, Toast.LENGTH_LONG).show();
    }

    private void updateTheme() {
        ThemeColor themeColor;
        if (selectedTheme < 0) {
            themeColor = this.themeColor;
            color.setText(R.string.none);
        } else {
            themeColor = themeCache.getThemeColor(selectedTheme);
            color.setText(themeColor.getName());
        }
        themeColor.apply(toolbar);
        themeColor.applyToStatusBar(this);
    }
}
