package org.tasks.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.DefaultFilterProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static org.tasks.activities.RemoteListSupportPicker.newRemoteListSupportPicker;

public class RemoteListFragment extends TaskEditControlFragment {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";
    private static final String EXTRA_ORIGINAL_LIST = "extra_original_list";
    private static final String EXTRA_SELECTED_LIST = "extra_selected_list";

    public static final int TAG = R.string.TEA_ctrl_google_task_list;

    @BindView(R.id.google_task_list) TextView textView;

    @Inject GtasksListService gtasksListService;
    @Inject GoogleTaskDao googleTaskDao;
    @Inject Tracker tracker;
    @Inject DefaultFilterProvider defaultFilterProvider;

    @Nullable private Filter originalList;
    @Nullable private Filter selectedList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            originalList = savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST);
            selectedList = savedInstanceState.getParcelable(EXTRA_SELECTED_LIST);
        } else {
            if (task.isNew()) {
                originalList = task.hasTransitory(GoogleTask.KEY)
                        ? new GtasksFilter(gtasksListService.getList(task.getTransitory(GoogleTask.KEY)))
                        : defaultFilterProvider.getDefaultRemoteList();
            } else {
                GoogleTask googleTask = googleTaskDao.getByTaskId(task.getId());
                if (googleTask != null) {
                    originalList = new GtasksFilter(gtasksListService.getList(googleTask.getListId()));
                }
            }
            selectedList = originalList;
        }

        refreshView();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (originalList != null) {
            outState.putParcelable(EXTRA_ORIGINAL_LIST, originalList);
        }
        if (selectedList != null) {
            outState.putParcelable(EXTRA_SELECTED_LIST, selectedList);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_remote_list;
    }

    @Override
    protected int getIcon() {
        return R.drawable.ic_cloud_black_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @OnClick(R.id.google_task_list)
    void clickGoogleTaskList(View view) {
        newRemoteListSupportPicker(selectedList)
                .show(getChildFragmentManager(), FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
    }

    @Override
    public void apply(Task task) {
        GoogleTask googleTask = googleTaskDao.getByTaskId(task.getId());
        if (googleTask != null && selectedList != null && googleTask.getListId().equals(((GtasksFilter) selectedList).getRemoteId())) {
            return;
        }

        if (googleTask != null) {
            tracker.reportEvent(Tracking.Events.GTASK_MOVE);
            task.putTransitory(SyncFlags.FORCE_SYNC, true);
            googleTask.setDeleted(now());
            googleTaskDao.update(googleTask);
        }

        if (selectedList != null) {
            googleTaskDao.insert(new GoogleTask(task.getId(), ((GtasksFilter) selectedList).getRemoteId()));
        }
    }

    @Override
    public boolean hasChanges(Task original) {
        return selectedList == null ? originalList != null : !selectedList.equals(originalList);
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    public void setList(Filter list) {
        if (list == null) {
            this.selectedList = null;
        } else if (list instanceof GtasksFilter) {
            this.selectedList = list;
        } else {
            throw new RuntimeException("Unhandled filter type");
        }
        refreshView();
    }

    private void refreshView() {
        textView.setText(selectedList == null ? null : selectedList.listingTitle);
    }
}
