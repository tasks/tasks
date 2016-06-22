package org.tasks.ui;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.activities.GoogleTaskListSelectionDialog;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import static com.todoroo.andlib.utility.DateUtilities.now;

public class GoogleTaskListFragment extends TaskEditControlFragment {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";
    private static final String EXTRA_TASK_ID = "extra_task_id";
    private static final String EXTRA_ORIGINAL_LIST = "extra_original_list";
    private static final String EXTRA_SELECTED_LIST = "extra_selected_list";

    public static final int TAG = R.string.TEA_ctrl_google_task_list;

    @BindView(R.id.google_task_list) TextView textView;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject MetadataDao metadataDao;
    @Inject Tracker tracker;

    private long taskId;
    private GtasksList originalList;
    private GtasksList selectedList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            taskId = savedInstanceState.getLong(EXTRA_TASK_ID);
            originalList = new GtasksList((StoreObject) savedInstanceState.getParcelable(EXTRA_ORIGINAL_LIST));
            selectedList = new GtasksList((StoreObject) savedInstanceState.getParcelable(EXTRA_SELECTED_LIST));
        } else {
            Metadata metadata = gtasksMetadataService.getActiveTaskMetadata(taskId);
            if (metadata != null) {
                originalList = gtasksListService.getList(metadata.getValue(GtasksMetadata.LIST_ID));
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

        outState.putLong(EXTRA_TASK_ID, taskId);
        outState.putParcelable(EXTRA_ORIGINAL_LIST, originalList.getStoreObject());
        outState.putParcelable(EXTRA_SELECTED_LIST, selectedList.getStoreObject());
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_google_task_list;
    }

    @Override
    protected int getIcon() {
        return R.drawable.ic_cloud_queue_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @OnClick(R.id.google_task_list)
    void clickGoogleTaskList(View view) {
        FragmentManager fragmentManager = getFragmentManager();
        GoogleTaskListSelectionDialog dialog = (GoogleTaskListSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
        if (dialog == null) {
            dialog = new GoogleTaskListSelectionDialog();
            dialog.show(fragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
        }
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        taskId = task.getId();
    }

    @Override
    public void apply(Task task) {
        if (selectedList == null) {
            return;
        }

        Metadata taskMetadata = gtasksMetadataService.getActiveTaskMetadata(task.getId());
        if (taskMetadata == null) {
            taskMetadata = GtasksMetadata.createEmptyMetadataWithoutList(task.getId());
        } else if (!taskMetadata.getValue(GtasksMetadata.LIST_ID).equals(selectedList.getRemoteId())) {
            tracker.reportEvent(Tracking.Events.GTASK_MOVE);
            task.putTransitory(SyncFlags.FORCE_SYNC, true);
            taskMetadata.setDeletionDate(now());
            metadataDao.persist(taskMetadata);
            taskMetadata = GtasksMetadata.createEmptyMetadataWithoutList(task.getId());
        }
        taskMetadata.setValue(GtasksMetadata.LIST_ID, selectedList.getRemoteId());
        metadataDao.persist(taskMetadata);
    }

    @Override
    public boolean hasChanges(Task original) {
        return !selectedList.equals(originalList);
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    public void setList(GtasksList list) {
        this.selectedList = list;
        refreshView();
    }

    private void refreshView() {
        if (selectedList != null) {
            textView.setText(selectedList.getName());
        }
    }
}
