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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.AstridPreferences;

public class TagUpdatesFragment extends CommentsFragment {

    private TagData tagData;

    @Autowired
    private TagDataService tagDataService;

    @Autowired
    private ActFmSyncService actFmSyncService;

    public TagUpdatesFragment(TagData tagData) {
        super();
        this.tagData = tagData;
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
        imageView.setDefaultImageResource(TagService.getDefaultImageIDForTag(tagName));
        imageView.setUrl(tagData.getValue(TagData.PICTURE));
    }

    @Override
    protected void refreshActivity(boolean manual) {
        if (actFmPreferenceService.isLoggedIn()) {
            final ProgressBarSyncResultCallback callback = new ProgressBarSyncResultCallback(
                    getActivity(), this, R.id.comments_progressBar, new Runnable() {
                        @Override
                        public void run() {
                            refreshUpdatesList();
                        }
                    });

            callback.started();
            callback.incrementMax(100);
            Runnable doneRunnable = new Runnable() {
                @Override
                public void run() {
                    callback.incrementProgress(50);
                    callback.finished();
                }
            };
            if (tagData != null) {
                actFmSyncService.fetchUpdatesForTag(tagData, manual, doneRunnable);
            } else {
                actFmSyncService.fetchPersonalUpdates(manual, doneRunnable);
            }
            callback.incrementProgress(50);
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected void addComment() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "tag_comment");
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TAGS, "," + tagData.getValue(TagData.REMOTE_ID) + ",");
        update.setValue(Update.TAGS_LOCAL, "," + tagData.getId() + ",");
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        update.setValue(Update.TARGET_NAME, tagData.getValue(TagData.NAME));
        if (picture != null) {
            update.setValue(Update.PICTURE, Update.PICTURE_LOADING);
            try {
                String updateString = getPictureHashForUpdate(update);
                imageCache.put(updateString, picture);
                update.setValue(Update.PICTURE, updateString);
            }
            catch (Exception e) {
                Log.e("TagUpdatesFragment", "Failed to put image to disk...");
            }
        }
        update.putTransitory(SyncFlags.ACTFM_SUPPRESS_SYNC, true);
        updateDao.createNew(update);

        final long updateId = update.getId();
        final Bitmap tempPicture = picture;
        new Thread() {
            @Override
            public void run() {
                actFmSyncService.pushUpdate(updateId, tempPicture);
            }
        }.start();
        addCommentField.setText(""); //$NON-NLS-1$
        picture = null;

        resetPictureButton();
        refreshUpdatesList();

        StatisticsService.reportEvent(StatisticsConstants.ACTFM_TAG_COMMENT);
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
