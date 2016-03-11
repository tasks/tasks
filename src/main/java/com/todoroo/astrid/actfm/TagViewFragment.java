/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.os.Bundle;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;

import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

public class TagViewFragment extends TaskListFragment {

    private static final String EXTRA_TAG_DATA = "extra_tag_data";

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$
    public static final String EXTRA_TAG_UUID = "uuid"; //$NON-NLS-1$

    protected TagData tagData;

    @Inject TagDataDao tagDataDao;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            tagData = savedInstanceState.getParcelable(EXTRA_TAG_DATA);
        } else {
            String tag = extras.getString(EXTRA_TAG_NAME);
            String uuid = RemoteModel.NO_UUID;
            if (extras.containsKey(EXTRA_TAG_UUID)) {
                uuid = extras.getString(EXTRA_TAG_UUID);
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
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnKeyListener(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_TAG_DATA, tagData);
    }

    @Override
    public TagData getActiveTagData() {
        return tagData;
    }

    // --------------------------------------------------------- refresh data

    @Override
    protected void initiateAutomaticSyncImpl() {
        if (tagData != null) {
            long lastAutosync = tagData.getLastAutosync();
            if(DateUtilities.now() - lastAutosync > AUTOSYNC_INTERVAL) {
                tagData.setLastAutosync(DateUtilities.now());
                tagDataDao.saveExisting(tagData);
            }
        }
    }

    @Override
    protected boolean hasDraggableOption() {
        return tagData != null;
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
