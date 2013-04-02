/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.FetchHistory;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
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

    @Autowired
    private TagDataDao tagDataDao;

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
            if (id > 0)
                tagData = tagDataService.fetchById(id, TagData.PROPERTIES);
        }
    }

    @Override
    protected boolean hasModel() {
        return tagData != null;
    }

    @Override
    protected void refetchModel() {
        if (tagData != null) {
            tagData = tagDataService.fetchById(tagData.getId(), TagData.PROPERTIES);
        }
    }

    @Override
    protected String getModelName() {
        return tagData.getValue(TagData.NAME);
    }

    @Override
    protected Cursor getCursor() {
        return tagDataService.getActivityAndHistoryForTagData(tagData, null, UpdateAdapter.USER_TABLE_ALIAS, UpdateAdapter.USER_PROPERTIES);
    }

    @Override
    protected String getSourceIdentifier() {
        return (tagData == null) ? UpdateAdapter.FROM_RECENT_ACTIVITY_VIEW : UpdateAdapter.FROM_TAG_VIEW;
    }

    @Override
    protected boolean canLoadMoreHistory() {
        return hasModel() && tagData.getValue(TagData.HISTORY_HAS_MORE) > 0;
    }

    @Override
    protected void loadMoreHistory(int offset, SyncMessageCallback callback) {
        new FetchHistory<TagData>(tagDataDao, TagData.HISTORY_FETCH_DATE, TagData.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TAGS,
                tagData.getUuid(), null, 0, offset, callback).execute();
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
        imageView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(getResources(), TagService.getDefaultImageIDForTag(tagData.getUuid())));
        String imageUrl = tagData.getPictureUrl(TagData.PICTURE, RemoteModel.PICTURE_MEDIUM);
        Bitmap imageBitmap = null;
        if (TextUtils.isEmpty(imageUrl))
            imageBitmap = tagData.getPictureBitmap(TagData.PICTURE);
        if (imageBitmap != null)
            imageView.setImageBitmap(imageBitmap);
        else
            imageView.setUrl(imageUrl);
    }

    @Override
    protected void performFetch(boolean manual, SyncMessageCallback done) {
        if (tagData != null) {
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<UserActivity>(UserActivity.class, null, tagData.getValue(TagData.USER_ACTIVITIES_PUSHED_AT), BriefMe.TAG_ID_KEY, tagData.getUuid()), done);
            new FetchHistory<TagData>(tagDataDao, TagData.HISTORY_FETCH_DATE, TagData.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TAGS,
                    tagData.getUuid(), null, tagData.getValue(TagData.HISTORY_FETCH_DATE), 0, done).execute();
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
    protected String commentAddStatistic() {
        return StatisticsConstants.ACTFM_TAG_COMMENT;
    }

    @Override
    protected void setLastViewed() {
        if(tagData != null && RemoteModel.isValidUuid(tagData.getValue(TagData.UUID))) {
            Preferences.setLong(UPDATES_LAST_VIEWED + tagData.getValue(TagData.UUID), DateUtilities.now());
            Activity activity = getActivity();
            if (activity instanceof TaskListActivity)
                ((TaskListActivity) activity).setCommentsCount(0);
        }
    }


}
