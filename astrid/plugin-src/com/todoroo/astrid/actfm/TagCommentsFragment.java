/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.ResourceDrawableCache;

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
        if (tagData == null)
            tagData = intent.getParcelableExtra(TagViewFragment.EXTRA_TAG_DATA);
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
        return tagDataService.getUpdates(tagData);
    }

    @Override
    protected String getSourceIdentifier() {
        return (tagData == null) ? UpdateAdapter.FROM_RECENT_ACTIVITY_VIEW : UpdateAdapter.FROM_TAG_VIEW;
    }

    @Override
    protected void addHeaderToListView(ListView listView) {
        if (AstridPreferences.useTabletLayout(getActivity()) && tagData != null) {
            listHeader = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.tag_updates_header, listView, false);
            populateListHeader(listHeader);
            listView.addHeaderView(listHeader);
        }
    }

    @Override
    protected void populateListHeader(ViewGroup header) {
        if (header == null) return;
        TextView tagTitle = (TextView) header.findViewById(R.id.tag_title);
        String tagName = tagData.getValue(TagData.NAME);
        tagTitle.setText(tagName);
        TextView descriptionTitle = (TextView) header.findViewById(R.id.tag_description);
        String description = tagData.getValue(TagData.TAG_DESCRIPTION);
        if (!TextUtils.isEmpty(description)) {
            descriptionTitle.setText(description);
            descriptionTitle.setVisibility(View.VISIBLE);
        }
        else {
            descriptionTitle.setVisibility(View.GONE);
        }


        AsyncImageView imageView = (AsyncImageView) header.findViewById(R.id.tag_picture);
        imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(getResources(), TagService.getDefaultImageIDForTag(tagName)));
        imageView.setUrl(tagData.getValue(TagData.PICTURE));
    }

    @Override
    protected void performFetch(boolean manual, Runnable done) {
        actFmSyncService.fetchUpdatesForTag(tagData, manual, done);
    }

    @Override
    @SuppressWarnings("nls")
    protected Update createUpdate() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "tag_comment");
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TAGS, "," + tagData.getValue(TagData.REMOTE_ID) + ",");
        update.setValue(Update.TAGS_LOCAL, "," + tagData.getId() + ",");
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        update.setValue(Update.TARGET_NAME, tagData.getValue(TagData.NAME));
        return update;
    }

    @Override
    protected String commentAddStatistic() {
        return StatisticsConstants.ACTFM_TAG_COMMENT;
    }

    @Override
    protected void setLastViewed() {
        if(tagData != null && tagData.getValue(TagData.REMOTE_ID) > 0) {
            Preferences.setLong(UPDATES_LAST_VIEWED + tagData.getValue(TagData.REMOTE_ID), DateUtilities.now());
            Activity activity = getActivity();
            if (activity instanceof TaskListActivity)
                ((TaskListActivity) activity).setCommentsCount(0);
        }
    }


}
