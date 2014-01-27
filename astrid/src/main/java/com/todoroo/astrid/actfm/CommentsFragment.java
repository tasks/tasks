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
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.UserActivity;

import org.json.JSONObject;
import org.tasks.R;

public abstract class CommentsFragment extends ListFragment {

    protected UpdateAdapter updateAdapter;
    protected EditText addCommentField;
    protected ViewGroup listHeader;
    protected Button footerView = null;

    protected ImageButton pictureButton;

    protected Bitmap picture = null;

    public static final String TAG_UPDATES_FRAGMENT = "tagupdates_fragment"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = Menu.FIRST;

    @Autowired UserActivityDao userActivityDao;

    public CommentsFragment() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(getLayout(), container, false);
    }

    protected abstract int getLayout();

    protected abstract void loadModelFromIntent(Intent intent);

    protected abstract boolean hasModel();

    protected abstract Cursor getCursor();

    protected abstract void addHeaderToListView(ListView listView);

    protected abstract UserActivity createUpdate();

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
        commentButton.setVisibility(TextUtils.isEmpty(addCommentField.getText()) ? View.GONE : View.VISIBLE);
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
                if (picture != null) {
                    ActFmCameraModule.showPictureLauncher(CommentsFragment.this, clearImage);
                } else {
                    ActFmCameraModule.showPictureLauncher(CommentsFragment.this, null);
                }
            }
        });

        refreshUpdatesList();
    }

    protected void resetPictureButton() {
        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.ic_action_camera, typedValue, true);
        pictureButton.setImageResource(typedValue.resourceId);
    }

    protected void refreshUpdatesList() {
        Activity activity = getActivity();
        View view = getView();
        if (activity == null || view == null) {
            return;
        }

        Cursor cursor;
        ListView listView = ((ListView) view.findViewById(android.R.id.list));
        if(updateAdapter == null) {
            cursor = getCursor();
            activity.startManagingCursor(cursor);

            updateAdapter = new UpdateAdapter(this, R.layout.update_adapter_row, cursor);
            addHeaderToListView(listView);
            addFooterToListView(listView);
            listView.setAdapter(updateAdapter);
        } else {
            cursor = updateAdapter.getCursor();
            cursor.requery();
            activity.startManagingCursor(cursor);
            if (footerView != null) {
                listView.removeFooterView(footerView);
                footerView = null;
            }
        }

        listView.setVisibility(View.VISIBLE);
    }

    private void addFooterToListView(ListView listView) {
        if (footerView != null) {
            listView.removeFooterView(footerView);
        }
        footerView = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(menu.size() > 0) {
            return;
        }

        MenuItem item;
        boolean showCommentsRefresh = false;
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
            return true;
        }

        default: return false;
        }
    }

    protected void addComment() {
        UserActivity update = createUpdate();
        if (picture != null) {
            JSONObject pictureJson = RemoteModel.PictureHelper.savePictureJson(getActivity(), picture);
            if (pictureJson != null) {
                update.setPicture(pictureJson.toString());
            }

        }

        userActivityDao.createNew(update);
        addCommentField.setText(""); //$NON-NLS-1$
        picture = null;

        resetPictureButton();
        refreshUpdatesList();
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

        if (!ActFmCameraModule.activityResult(getActivity(), requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
