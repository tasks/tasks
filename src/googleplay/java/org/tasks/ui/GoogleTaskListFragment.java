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
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.R;
import org.tasks.activities.GoogleTaskListSelectionDialog;
import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class GoogleTaskListFragment extends TaskEditControlFragment {

    private static final String FRAG_TAG_GOOGLE_TASK_LIST_SELECTION = "frag_tag_google_task_list_selection";
    private static final String EXTRA_IS_NEW_TASK = "extra_is_new_task";
    private static final String EXTRA_TASK_ID = "extra_task_id";
    private static final String EXTRA_LIST = "extra_list";

    public static final int TAG = R.string.TEA_ctrl_google_task_list;

    @BindView(R.id.google_task_list) TextView textView;

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject MetadataDao metadataDao;

    private long taskId;
    private GtasksList list;
    private boolean isNewTask;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            isNewTask = savedInstanceState.getBoolean(EXTRA_IS_NEW_TASK);
            taskId = savedInstanceState.getLong(EXTRA_TASK_ID);
            list = new GtasksList((StoreObject) savedInstanceState.getParcelable(EXTRA_LIST));
        }
        Metadata metadata = gtasksMetadataService.getTaskMetadata(taskId);
        if (metadata != null) {
            list = gtasksListService.getList(metadata.getValue(GtasksMetadata.LIST_ID));
        }
        if (list == null) {
            list = gtasksListService.getList(gtasksPreferenceService.getDefaultList());
        }

        refreshView();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_IS_NEW_TASK, isNewTask);
        outState.putLong(EXTRA_TASK_ID, taskId);
        outState.putParcelable(EXTRA_LIST, list.getStoreObject());
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
        if (!isNewTask) {
            return;
        }

        FragmentManager fragmentManager = getFragmentManager();
        GoogleTaskListSelectionDialog dialog = (GoogleTaskListSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
        if (dialog == null) {
            dialog = new GoogleTaskListSelectionDialog();
            dialog.show(fragmentManager, FRAG_TAG_GOOGLE_TASK_LIST_SELECTION);
        }
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        this.isNewTask = isNewTask;
        taskId = task.getId();
    }

    @Override
    public void apply(Task task) {
        if (!isNewTask) {
            return;
        }
        Metadata taskMetadata = gtasksMetadataService.getTaskMetadata(task.getId());
        if (taskMetadata == null) {
            taskMetadata = GtasksMetadata.createEmptyMetadataWithoutList(task.getId());
        }
        taskMetadata.setValue(GtasksMetadata.LIST_ID, list.getRemoteId());
        metadataDao.persist(taskMetadata);
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    public void setList(GtasksList list) {
        this.list = list;
        refreshView();
    }

    private void refreshView() {
        if (list != null) {
            textView.setText(list.getName());
        }
    }
}
