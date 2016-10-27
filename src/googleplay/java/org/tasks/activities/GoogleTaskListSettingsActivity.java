package org.tasks.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.CreateListDialog;
import org.tasks.gtasks.DeleteListDialog;
import org.tasks.gtasks.RenameListDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.text.TextUtils.isEmpty;
import static org.tasks.gtasks.CreateListDialog.newCreateListDialog;
import static org.tasks.gtasks.DeleteListDialog.newDeleteListDialog;
import static org.tasks.gtasks.RenameListDialog.newRenameListDialog;

public class GoogleTaskListSettingsActivity extends ThemedInjectingAppCompatActivity
        implements Toolbar.OnMenuItemClickListener, CreateListDialog.CreateListDialogCallback,
        DeleteListDialog.DeleteListDialogCallback, RenameListDialog.RenameListDialogCallback {

    private static final String FRAG_TAG_CREATE_LIST_DIALOG = "frag_tag_create_list_dialog";
    private static final String FRAG_TAG_DELETE_LIST_DIALOG = "frag_tag_delete_list_dialog";
    private static final String FRAG_TAG_RENAME_LIST_DIALOG = "frag_tag_rename_list_dialog";

    public static final String EXTRA_STORE_DATA = "extra_store_data";
    public static final String ACTION_DELETED = "action_deleted";
    public static final String ACTION_RENAMED = "action_renamed";

    private GtasksList gtasksList;
    private boolean isNewList;

    @Inject StoreObjectDao storeObjectDao;
    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;
    @Inject GtasksListService gtasksListService;
    @Inject Tracker tracker;

    @BindView(R.id.tag_name) EditText listName;
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter_settings_activity);
        ButterKnife.bind(this);

        StoreObject storeObject = getIntent().getParcelableExtra(EXTRA_STORE_DATA);
        if (storeObject == null) {
            isNewList = true;
            storeObject = new StoreObject();
            storeObject.setType(GtasksList.TYPE);
        }
        gtasksList = new GtasksList(storeObject);

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
        toolbar.inflateMenu(R.menu.tag_settings_activity);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.showOverflowMenu();

        MenuColorizer.colorToolbar(this, toolbar);

        if (isNewList) {
            toolbar.getMenu().findItem(R.id.delete).setVisible(false);
            listName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(listName, InputMethodManager.SHOW_IMPLICIT);
        } else {
            listName.setText(gtasksList.getName());
        }
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
            finish();
        }
    }

    @Override
    public void finish() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(listName.getWindowToken(), 0);
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
        return listName.getText().toString().trim();
    }

    private boolean hasChanges() {
        if (isNewList) {
            return !isEmpty(getNewName());
        }
        return nameChanged();
    }

    private boolean nameChanged() {
        return !getNewName().equals(gtasksList.getName());
    }

    @Override
    public void onListCreated(TaskList taskList) {
        tracker.reportEvent(Tracking.Events.GTASK_NEW_LIST);
        GtasksList list = new GtasksList(taskList.getId());
        list.setName(taskList.getTitle());
        storeObjectDao.persist(list);
        setResult(RESULT_OK, new Intent().putExtra(TaskListActivity.OPEN_FILTER, new GtasksFilter(list)));
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
        storeObjectDao.persist(gtasksList);
        setResult(RESULT_OK, new Intent(ACTION_RENAMED).putExtra(TaskListActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
        finish();
    }

    @Override
    public void requestFailed() {
        Toast.makeText(this, R.string.gtasks_GLA_errorIOAuth, Toast.LENGTH_LONG).show();
    }
}
