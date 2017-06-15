package org.tasks.tasklist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.R;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.analytics.Tracking;
import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class GtasksListFragment extends TaskListFragment {

    public static TaskListFragment newGtasksListFragment(GtasksFilter filter, GtasksList list) {
        GtasksListFragment fragment = new GtasksListFragment();
        fragment.filter = filter;
        fragment.list = list;
        return fragment;
    }

    private static final String EXTRA_STORE_OBJECT = "extra_store_object";
    private static final int REQUEST_LIST_SETTINGS = 10101;

    @Inject SyncV2Service syncService;

    protected GtasksList list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            StoreObject storeObject = savedInstanceState.getParcelable(EXTRA_STORE_OBJECT);
            list = new GtasksList(storeObject);
        }
    }

    @Override
    protected void inflateMenu(Toolbar toolbar) {
        super.inflateMenu(toolbar);
        toolbar.inflateMenu(R.menu.menu_gtasks_list_fragment);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_gtasks_list_settings:
                Intent intent = new Intent(getActivity(), GoogleTaskListSettingsActivity.class);
                intent.putExtra(GoogleTaskListSettingsActivity.EXTRA_STORE_DATA, list.getStoreObject());
                startActivityForResult(intent, REQUEST_LIST_SETTINGS);
                return true;
            default:
                return super.onMenuItemClick(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LIST_SETTINGS) {
            if (resultCode == RESULT_OK) {
                TaskListActivity activity = (TaskListActivity) getActivity();
                String action = data.getAction();
                if (GoogleTaskListSettingsActivity.ACTION_DELETED.equals(action)) {
                    activity.onFilterItemClicked(null);
                } else if (GoogleTaskListSettingsActivity.ACTION_RELOAD.equals(action)) {
                    activity.getIntent().putExtra(TaskListActivity.OPEN_FILTER,
                            (Filter) data.getParcelableExtra(TaskListActivity.OPEN_FILTER));
                    activity.recreate();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void clearCompleted() {
        tracker.reportEvent(Tracking.Events.GTASK_CLEAR_COMPLETED);
        syncService.clearCompleted(list, new SyncResultCallback() {
            @Override
            public void started() {
                setSyncOngoing(true);
            }

            @Override
            public void finished() {
                setSyncOngoing(false);

                onRefresh();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_STORE_OBJECT, list.getStoreObject());
    }

    @Override
    protected boolean hasDraggableOption() {
        return list != null;
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
