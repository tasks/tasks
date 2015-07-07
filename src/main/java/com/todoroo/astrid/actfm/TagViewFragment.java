/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.utility.Flags;

import org.tasks.ui.NavigationDrawerFragment;

import javax.inject.Inject;

public class TagViewFragment extends TaskListFragment {

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$

    @Deprecated
    private static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$

    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    public static final String EXTRA_TAG_DATA = "tagData"; //$NON-NLS-1$

    private static final int REQUEST_CODE_SETTINGS = 0;

    public static final String TOKEN_START_ACTIVITY = "startActivity"; //$NON-NLS-1$

    protected TagData tagData;

    @Inject TagDataDao tagDataDao;

    private boolean dataLoaded = false;

    // --- UI initialization

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnKeyListener(null);
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

        tagData = RemoteModel.isUuidEmpty(uuid)
                ? tagDataDao.getTagByName(tag, TagData.PROPERTIES)
                : tagDataDao.getByUuid(uuid, TagData.PROPERTIES);

        if (tagData == null) {
            tagData = new TagData();
            tagData.setName(tag);
            tagData.setUUID(uuid);
            tagDataDao.persist(tagData);
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

    // --------------------------------------------------------- refresh data

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (!isCurrentTaskListFragment()) {
            return;
        }
        if (tagData != null) {
            long lastAutosync = tagData.getLastAutosync();
            if(DateUtilities.now() - lastAutosync > AUTOSYNC_INTERVAL) {
                tagData.setLastAutosync(DateUtilities.now());
                tagDataDao.saveExisting(tagData);
            }
        }
    }

    protected void reloadTagData() {
        tagData = tagDataDao.fetch(tagData.getId(), TagData.PROPERTIES); // refetch
        if (tagData == null) {
            // This can happen if a tag has been deleted as part of a sync
            taskListMetadata = null;
            return;
        }
        initializeTaskListMetadata();
        filter = TagFilterExposer.filterFromTagData(getActivity(), tagData);
        getActivity().getIntent().putExtra(TOKEN_FILTER, filter);
        extras.putParcelable(TOKEN_FILTER, filter);
        Activity activity = getActivity();
        if (activity instanceof TaskListActivity) {
            ((TaskListActivity) activity).setListsTitle(filter.listingTitle);
            NavigationDrawerFragment navigationDrawer = ((TaskListActivity) activity).getNavigationDrawerFragment();
            if (navigationDrawer != null) {
                navigationDrawer.clear();
            }
        }
        taskAdapter = null;
        Flags.set(Flags.REFRESH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == Activity.RESULT_OK) {
            reloadTagData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected boolean hasDraggableOption() {
        return tagData != null;
    }
}
