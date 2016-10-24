package org.tasks.tasklist;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.R;
import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

public class GtasksListFragment extends TaskListFragment {

    public static TaskListFragment newGtasksListFragment(GtasksFilter filter, GtasksList list) {
        GtasksListFragment fragment = new GtasksListFragment();
        fragment.filter = filter;
        fragment.list = list;
        return fragment;
    }

    private static final String EXTRA_STORE_OBJECT = "extra_store_object";

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
            case R.id.menu_clear_completed:
                clearCompletedTasks();
                return true;
            default:
                return super.onMenuItemClick(item);
        }
    }

    private void clearCompletedTasks() {
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
