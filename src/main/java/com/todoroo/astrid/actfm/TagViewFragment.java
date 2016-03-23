/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.os.Bundle;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;

import org.tasks.injection.FragmentComponent;

import javax.inject.Inject;

public class TagViewFragment extends TaskListFragment {

    public static TaskListFragment newTagViewFragment(TagFilter filter, TagData tagData) {
        TagViewFragment fragment = new TagViewFragment();
        fragment.filter = filter;
        fragment.tagData = tagData;
        return fragment;
    }

    private static final String EXTRA_TAG_DATA = "extra_tag_data";

    @Inject TagDataDao tagDataDao;

    protected TagData tagData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            tagData = savedInstanceState.getParcelable(EXTRA_TAG_DATA);
        }
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

    public TagData getTagData() {
        return tagData;
    }

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
