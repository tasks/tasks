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
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.ImageDiskCache;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.StatisticsService;

public abstract class CommentsFragment extends ListFragment {

//    private TagData tagData;
    protected UpdateAdapter updateAdapter;
    protected EditText addCommentField;
    protected ViewGroup listHeader;

    protected ImageButton pictureButton;

    protected Bitmap picture = null;

    public static final String TAG_UPDATES_FRAGMENT = "tagupdates_fragment"; //$NON-NLS-1$

    //Append tag data remote id to this preference
    public static final String UPDATES_LAST_VIEWED = "updates_last_viewed_"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = Menu.FIRST;

    protected final ImageDiskCache imageCache;


    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired UpdateDao updateDao;

    public CommentsFragment() {
        DependencyInjectionService.getInstance().inject(this);
        imageCache = ImageDiskCache.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(getLayout(), container, false);

        return v;
    }

    protected abstract int getLayout();

    protected abstract void loadModelFromIntent(Intent intent);

    protected abstract boolean hasModel();

    protected abstract String getModelName();

    protected abstract Cursor getCursor();

    protected abstract String getSourceIdentifier();

    protected abstract void addHeaderToListView(ListView listView);

    protected abstract void populateListHeader(ViewGroup header);

    protected abstract Update createUpdate();

    protected abstract String commentAddStatistic();

    protected abstract void performFetch(boolean manual, Runnable done);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        loadModelFromIntent(getActivity().getIntent());

        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };

        if (!hasModel()) {
            getView().findViewById(R.id.updatesFooter).setVisibility(View.GONE);
        }

        addCommentField = (EditText) getView().findViewById(R.id.commentField);
        addCommentField.setOnTouchListener(onTouch);

        setUpUpdateList();
    }

    protected void setUpUpdateList() {
        if (getActivity() instanceof TagUpdatesActivity) {
            ActionBar ab = ((AstridActivity) getActivity()).getSupportActionBar();
            String title = hasModel() ? getString(R.string.tag_updates_title, getModelName())
                    : getString(R.string.TLA_all_activity);
            ((TextView) ab.getCustomView().findViewById(R.id.title)).setText(title);
        }

        final ImageButton commentButton = (ImageButton) getView().findViewById(R.id.commentButton);
        addCommentField = (EditText) getView().findViewById(R.id.commentField);
        addCommentField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL && addCommentField.getText().length() > 0) {
                    addComment();
                    return true;
                }
                return false;
            }
        });
        addCommentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                commentButton.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //
            }
        });
        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });

        final ClearImageCallback clearImage = new ClearImageCallback() {
            @Override
            public void clearImage() {
                picture = null;
                resetPictureButton();
            }
        };
        pictureButton = (ImageButton) getView().findViewById(R.id.picture);
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picture != null)
                    ActFmCameraModule.showPictureLauncher(CommentsFragment.this, clearImage);
                else
                    ActFmCameraModule.showPictureLauncher(CommentsFragment.this, null);
            }
        });

        refreshUpdatesList();
        refreshActivity(false); // start a pull in the background
    }

    protected void resetPictureButton() {
        pictureButton.setImageResource(R.drawable.camera_button);
    }

    protected void refreshUpdatesList() {
        Cursor cursor = null;
        ListView listView = ((ListView) getView().findViewById(android.R.id.list));
        if(updateAdapter == null) {
            cursor = getCursor();
            getActivity().startManagingCursor(cursor);
            String source = getSourceIdentifier();

            updateAdapter = new UpdateAdapter(this, R.layout.update_adapter_row,
                    cursor, false, source);
            addHeaderToListView(listView);
            listView.setAdapter(updateAdapter);
        } else {
            cursor = updateAdapter.getCursor();
            cursor.requery();
            getActivity().startManagingCursor(cursor);
            populateListHeader(listHeader);
        }

        View activityContainer = getView().findViewById(R.id.no_activity_container);
        if (cursor.getCount() == 0) {
            activityContainer.setVisibility(View.VISIBLE);
            TextView textView = (TextView)activityContainer.findViewById(R.id.no_activity_message);
            if(actFmPreferenceService.isLoggedIn()) {
                textView.setText(getActivity().getString(R.string.ENA_no_comments));
            }
            else {
                textView.setText(getActivity().getString(R.string.UpS_no_activity_log_in));
                activityContainer.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        startActivityForResult(new Intent(getActivity(), ActFmLoginActivity.class),
                                TagSettingsActivity.REQUEST_ACTFM_LOGIN);
                    }
                });
            }
            listView.setVisibility(View.GONE);
        }
        else {
            activityContainer.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        if (getActivity() instanceof TagUpdatesActivity)
            setLastViewed();
    }

    protected void setLastViewed() {
        //
    }

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
            if (hasModel()) {
                performFetch(manual, doneRunnable);
            } else {
                actFmSyncService.fetchPersonalUpdates(manual, doneRunnable);
            }
            callback.incrementProgress(50);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() > 0)
            return;

        MenuItem item;
        boolean showCommentsRefresh = actFmPreferenceService.isLoggedIn();
        if (showCommentsRefresh) {
        Activity activity = getActivity();
            if (activity instanceof TaskListActivity) {
                TaskListActivity tla = (TaskListActivity) activity;
                showCommentsRefresh = tla.getTaskEditFragment() == null;
            }
        }
        if(showCommentsRefresh) {
            item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                    R.string.ENA_refresh_comments);
            item.setIcon(R.drawable.icn_menu_refresh_dark);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {

        case MENU_REFRESH_ID: {

            refreshActivity(true);
            return true;
        }

        default: return false;
        }
    }

    protected void addComment() {
        Update update = createUpdate();

        if (picture != null) {
            update.setValue(Update.PICTURE, Update.PICTURE_LOADING);
            try {
                String updateString = getPictureHashForUpdate(update);
                imageCache.put(updateString, picture);
                update.setValue(Update.PICTURE, updateString);
            }
            catch (Exception e) {
                Log.e("CommentFragment", "Failed to put image to disk...", e);  //$NON-NLS-1$//$NON-NLS-2$
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

        StatisticsService.reportEvent(commentAddStatistic());
    }


    protected String getPictureHashForUpdate(Update u) {
        String s = u.getValue(Update.TASK).toString() + u.getValue(Update.CREATION_DATE);
        return s;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        CameraResultCallback callback = new CameraResultCallback() {
            @Override
            public void handleCameraResult(Bitmap bitmap) {
                picture = bitmap;
                pictureButton.setImageBitmap(picture);
            }
        };

        if (ActFmCameraModule.activityResult(getActivity(), requestCode, resultCode, data, callback)) {
            //Handled
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
