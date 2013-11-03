/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.ListView;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.utility.AstridPreferences;

import org.tasks.R;

public class TagCommentsFragment extends CommentsFragment {

    private TagData tagData;

    @Autowired
    private TagDataService tagDataService;

    public TagCommentsFragment() {
        super();
    }

    public TagCommentsFragment(TagData tagData) {
        this();
        this.tagData = tagData;
    }

    @Override
    protected int getLayout() {
        return R.layout.tag_updates_fragment;
    }

    @Override
    protected void loadModelFromIntent(Intent intent) {
        if (tagData == null) {
            long id = intent.getLongExtra(TagViewFragment.EXTRA_TAG_DATA, 0);
            if (id > 0) {
                tagData = tagDataService.fetchById(id, TagData.PROPERTIES);
            }
        }
    }

    @Override
    protected boolean hasModel() {
        return tagData != null;
    }

    @Override
    protected String getModelName() {
        return tagData.getValue(TagData.NAME);
    }

    @Override
    protected Cursor getCursor() {
        return tagDataService.getActivityForTagData(tagData, null);
    }

    @Override
    protected void addHeaderToListView(ListView listView) {
        if (AstridPreferences.useTabletLayout(getActivity()) && tagData != null) {
            listHeader = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.tag_updates_header, listView, false);
            listView.addHeaderView(listHeader);
        }
    }

    @Override
    protected UserActivity createUpdate() {
        UserActivity userActivity = new UserActivity();
        userActivity.setValue(UserActivity.MESSAGE, addCommentField.getText().toString());
        userActivity.setValue(UserActivity.ACTION, UserActivity.ACTION_TAG_COMMENT);
        userActivity.setValue(UserActivity.USER_UUID, Task.USER_ID_SELF);
        userActivity.setValue(UserActivity.TARGET_ID, tagData.getUuid());
        userActivity.setValue(UserActivity.TARGET_NAME, tagData.getValue(TagData.NAME));
        userActivity.setValue(UserActivity.CREATED_AT, DateUtilities.now());
        return userActivity;
    }

    @Override
    protected void setLastViewed() {
        if(tagData != null && RemoteModel.isValidUuid(tagData.getValue(TagData.UUID))) {
            Preferences.setLong(UPDATES_LAST_VIEWED + tagData.getValue(TagData.UUID), DateUtilities.now());
            Activity activity = getActivity();
        }
    }
}
