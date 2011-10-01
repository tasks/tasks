package com.todoroo.astrid.actfm;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.utility.Flags;

public class TagUpdatesActivity extends ListActivity {

    private TagData tagData;
    private UpdateAdapter updateAdapter;
    private EditText addCommentField;

    private ImageButton pictureButton;

    private Bitmap picture = null;

    private static final int MENU_REFRESH_ID = Menu.FIRST;

    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired TagDataService tagDataService;
    @Autowired UpdateDao updateDao;
    @Autowired ActFmSyncService actFmSyncService;

    public TagUpdatesActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        ThemeService.applyTheme(this);
        setContentView(R.layout.tag_updates_activity);
        tagData = getIntent().getParcelableExtra(TagViewActivity.EXTRA_TAG_DATA);

        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };

        addCommentField = (EditText) findViewById(R.id.commentField);
        addCommentField.setOnTouchListener(onTouch);

        setUpUpdateList();
    }

    protected void setUpUpdateList() {
        ((TextView) findViewById(R.id.listLabel)).setText(this.getString(R.string.tag_updates_title, tagData.getValue(TagData.NAME)));
        final ImageButton commentButton = (ImageButton) findViewById(R.id.commentButton);
        addCommentField = (EditText) findViewById(R.id.commentField);
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
                pictureButton.setImageResource(R.drawable.icn_camera);
            }
        };
        pictureButton = (ImageButton) findViewById(R.id.picture);
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picture != null)
                    ActFmCameraModule.showPictureLauncher(TagUpdatesActivity.this, clearImage);
                else
                    ActFmCameraModule.showPictureLauncher(TagUpdatesActivity.this, null);
            }
        });

        refreshUpdatesList();
    }

    private void refreshUpdatesList() {

        if(!actFmPreferenceService.isLoggedIn() || tagData.getValue(Task.REMOTE_ID) <= 0)
            return;

        if(updateAdapter == null) {
            TodorooCursor<Update> currentCursor = tagDataService.getUpdates(tagData);
            startManagingCursor(currentCursor);

            updateAdapter = new UpdateAdapter(this, R.layout.update_adapter_row,
                    currentCursor, false, null);
            ((ListView)findViewById(android.R.id.list)).setAdapter(updateAdapter);
        } else {
            Cursor cursor = updateAdapter.getCursor();
            cursor.requery();
            startManagingCursor(cursor);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            return true;

        MenuItem item;
        if(actFmPreferenceService.isLoggedIn()) {
            item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                    R.string.ENA_refresh_comments);
            item.setIcon(R.drawable.ic_menu_refresh);
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {

        case MENU_REFRESH_ID: {

            final ProgressDialog progressDialog = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
            actFmSyncService.fetchUpdatesForTag(tagData, true, new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshUpdatesList();
                            DialogUtilities.dismissDialog(TagUpdatesActivity.this, progressDialog);
                        }
                    });
                }
            });
            return true;
        }

        default: return false;
        }
    }

    @SuppressWarnings("nls")
    private void addComment() {
        if(tagData.getValue(TagData.REMOTE_ID) == 0L)
            return;

        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "tag_comment");
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TAGS, "," + tagData.getValue(TagData.REMOTE_ID) + ",");
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        if (picture != null) {
            update.setValue(Update.PICTURE, Update.PICTURE_LOADING);
        }
        Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
        updateDao.createNew(update);

        final long updateId = update.getId();
        new Thread() {
            @Override
            public void run() {
                actFmSyncService.pushUpdate(updateId, picture);
            }
        }.start();
        addCommentField.setText(""); //$NON-NLS-1$
        pictureButton.setImageResource(R.drawable.icn_camera);
        refreshUpdatesList();

        StatisticsService.reportEvent(StatisticsConstants.ACTFM_TAG_COMMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        CameraResultCallback callback = new CameraResultCallback() {
            @Override
            public void handleCameraResult(Bitmap bitmap) {
                picture = bitmap;
                pictureButton.setImageBitmap(picture);
            }
        };

        if (ActFmCameraModule.activityResult(this, requestCode, resultCode, data, callback)) {
            //Handled
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStop() {
        StatisticsService.sessionStop(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
    }

}
