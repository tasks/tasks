package org.tasks.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static org.tasks.activities.SupportGoogleTaskListPicker.newSupportGoogleTaskListPicker;

public class GoogleTaskListFragment extends TaskEditControlFragment {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";
    private static final String EXTRA_ORIGINAL_LIST = "extra_original_list";
    private static final String EXTRA_SELECTED_LIST = "extra_selected_list";

    public static final int TAG = R.string.TEA_ctrl_google_task_list;

    @BindView(R.id.google_task_list) TextView textView;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GoogleTaskDao googleTaskDao;
    @Inject Tracker tracker;

    @Nullable private GoogleTaskList originalList;
    @Nullable private GoogleTaskList selectedList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            originalList = savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST);
            selectedList = savedInstanceState.getParcelable(EXTRA_SELECTED_LIST);
        } else {
            if (task.isNew()) {
                originalList = gtasksListService.getList(task.getTransitory(GoogleTask.KEY));
            } else {
                GoogleTask googleTask = googleTaskDao.getByTaskId(task.getId());
                if (googleTask != null) {
                    originalList = gtasksListService.getList(googleTask.getListId());
                }
            }
            if (originalList == null) {
                originalList = gtasksListService.getList(gtasksPreferenceService.getDefaultList());
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
        return R.layout.control_set_google_task_list;
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
        newSupportGoogleTaskListPicker(selectedList)
                .show(getChildFragmentManager(), FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
    }

    @Override
    public void apply(Task task) {
        if (selectedList == null) {
            return;
        }

        GoogleTask googleTask = googleTaskDao.getByTaskId(task.getId());
        if (googleTask == null) {
            googleTaskDao.insert(new GoogleTask(task.getId(), selectedList.getRemoteId()));
        } else if (!googleTask.getListId().equals(selectedList.getRemoteId())) {
            tracker.reportEvent(Tracking.Events.GTASK_MOVE);
            task.putTransitory(SyncFlags.FORCE_SYNC, true);
            googleTask.setDeleted(now());
            googleTaskDao.update(googleTask);
            googleTaskDao.insert(new GoogleTask(task.getId(), selectedList.getRemoteId()));
        }
    }

    @Override
    public boolean hasChanges(Task original) {
        return selectedList != null && !selectedList.equals(originalList);
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    public void setList(GoogleTaskList list) {
        this.selectedList = list;
        refreshView();
    }

    private void refreshView() {
        if (selectedList != null) {
            textView.setText(selectedList.getTitle());
        }
    }
}
