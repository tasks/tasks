/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class TagViewFragment extends TaskListFragment {

    public static final String BROADCAST_TAG_ACTIVITY = AstridApiConstants.API_PACKAGE + ".TAG_ACTIVITY"; //$NON-NLS-1$

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$

    @Deprecated
    private static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$

    private static final int REQUEST_CODE_SETTINGS = 0;

    public static final String TOKEN_START_ACTIVITY = "startActivity"; //$NON-NLS-1$

    protected TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired TagDataDao tagDataDao;

    protected View taskListView;

    private boolean dataLoaded = false;

    protected AtomicBoolean isBeingFiltered = new AtomicBoolean(false);

    protected boolean justDeleted = false;

    // --- UI initialization

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnKeyListener(null);

        // allow for text field entry, needed for android bug #2516
        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };

        ((EditText) getView().findViewById(R.id.quickAddText)).setOnTouchListener(onTouch);
    }

    /* (non-Javadoc)
     * @see com.todoroo.astrid.activity.TaskListActivity#getListBody(android.view.ViewGroup)
     */
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getActivity().getLayoutInflater().inflate(getTaskListBodyLayout(), root, false);

        taskListView = super.getListBody(parent);
        parent.addView(taskListView);

        return parent;
    }

    protected int getTaskListBodyLayout() {
        return R.layout.task_list_body_tag;
    }

    // --- data loading

    @Override
    protected void initializeData() {
        synchronized(this) {
            if(dataLoaded) {
                return;
            }
            dataLoaded = true;
        }

        TaskListActivity activity = (TaskListActivity) getActivity();
        String tag = extras.getString(EXTRA_TAG_NAME);
        String uuid = RemoteModel.NO_UUID;
        if (extras.containsKey(EXTRA_TAG_UUID)) {
            uuid = extras.getString(EXTRA_TAG_UUID);
        } else if (extras.containsKey(EXTRA_TAG_REMOTE_ID)) // For legacy support with shortcuts, widgets, etc.
        {
            uuid = Long.toString(extras.getLong(EXTRA_TAG_REMOTE_ID));
        }


        if(tag == null && RemoteModel.NO_UUID.equals(uuid)) {
            return;
        }

        TodorooCursor<TagData> cursor;
        if (!RemoteModel.isUuidEmpty(uuid)) {
            cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(TagData.UUID.eq(uuid)));
        } else {
            cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(TagData.NAME.eqCaseInsensitive(tag)));
        }

        try {
            tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, tag);
                tagData.setValue(TagData.UUID, uuid);
                tagDataService.save(tagData);
            } else {
                cursor.moveToFirst();
                tagData.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        super.initializeData();

        if (extras.getBoolean(TOKEN_START_ACTIVITY, false)) {
            extras.remove(TOKEN_START_ACTIVITY);
        }
    }

    @Override
    public TagData getActiveTagData() {
        return tagData;
    }

    @Override
    public void loadTaskListContent(boolean requery) {
        super.loadTaskListContent(requery);
        if(taskAdapter == null || taskAdapter.getCursor() == null) {
            return;
        }

        int count = taskAdapter.getCursor().getCount();

        if(tagData != null && sortFlags <= SortHelper.FLAG_REVERSE_SORT &&
                count != tagData.getValue(TagData.TASK_COUNT)) {
            tagData.setValue(TagData.TASK_COUNT, count);
            tagDataService.save(tagData);
        }
    }

    // --------------------------------------------------------- refresh data


    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment()) {
            return;
        }
        if (tagData != null) {
            long lastAutosync = tagData.getValue(TagData.LAST_AUTOSYNC);
            if(DateUtilities.now() - lastAutosync > AUTOSYNC_INTERVAL) {
                tagData.setValue(TagData.LAST_AUTOSYNC, DateUtilities.now());
                tagDataDao.saveExisting(tagData);
            }
        }
    }

    // --- receivers

    private final BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!intent.hasExtra("tag_id")) {
                return;
            }
            if(tagData == null || !tagData.getValue(TagData.UUID).toString().equals(intent.getStringExtra("tag_id"))) {
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //refreshUpdatesList();
                }
            });

            NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
            try {
                nm.cancel(Integer.parseInt(tagData.getValue(TagData.UUID)));
            } catch (NumberFormatException e) {
                // Eh
            }
        }
    };

    @Override
    public void onResume() {
        if (justDeleted) {
            parentOnResume();
            // tag was deleted locally in settings
            // go back to active tasks
            AstridActivity activity = ((AstridActivity) getActivity());
            FilterListFragment fl = activity.getFilterListFragment();
            if (fl != null) {
                fl.clear(); // Should auto refresh
                activity.switchToActiveTasks();
            }
            return;
        }
        super.onResume();


        IntentFilter intentFilter = new IntentFilter(BROADCAST_TAG_ACTIVITY);
        getActivity().registerReceiver(notifyReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        AndroidUtilities.tryUnregisterReceiver(getActivity(), notifyReceiver);
    }

    protected void reloadTagData(boolean onActivityResult) {
        tagData = tagDataService.fetchById(tagData.getId(), TagData.PROPERTIES); // refetch
        if (tagData == null) {
            // This can happen if a tag has been deleted as part of a sync
            taskListMetadata = null;
            return;
        } else if (tagData.isDeleted()) {
            justDeleted = true;
            return;
        }
        initializeTaskListMetadata();
        filter = TagFilterExposer.filterFromTagData(getActivity(), tagData);
        getActivity().getIntent().putExtra(TOKEN_FILTER, filter);
        extras.putParcelable(TOKEN_FILTER, filter);
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity) {
            ((TaskListActivity) activity).setListsTitle(filter.title);
            FilterListFragment flf = ((TaskListActivity) activity).getFilterListFragment();
            if (flf != null) {
                if (!onActivityResult) {
                    flf.refresh();
                } else {
                    flf.clear();
                }
            }
        }
        taskAdapter = null;
        Flags.set(Flags.REFRESH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
            reloadTagData(true);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected boolean hasDraggableOption() {
        return tagData != null;
    }

    @Override
    protected void toggleDragDrop(boolean newState) {
        Class<?> customComponent;

        if(newState) {
            customComponent = SubtasksTagListFragment.class;
        } else {
            filter.setFilterQueryOverride(null);
            customComponent = TagViewFragment.class;
        }

        ((FilterWithCustomIntent) filter).customTaskList = new ComponentName(getActivity(), customComponent);

        extras.putParcelable(TOKEN_FILTER, filter);
        ((AstridActivity)getActivity()).setupTasklistFragmentWithFilterAndCustomTaskList(filter,
                extras, customComponent);
    }

    @Override
    protected void refresh() {
        loadTaskListContent(true);
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
    }

}
