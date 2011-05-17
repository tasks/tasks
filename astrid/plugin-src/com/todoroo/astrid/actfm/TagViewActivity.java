package com.todoroo.astrid.actfm;

import greendroid.widget.AsyncImageView;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.ui.PeopleContainer;
import com.todoroo.astrid.utility.Flags;

public class TagViewActivity extends TaskListActivity implements OnTabChangeListener {

    private static final String LAST_FETCH_KEY = "tag-fetch-"; //$NON-NLS-1$

    public static final String BROADCAST_TAG_ACTIVITY = AstridApiConstants.PACKAGE + ".TAG_ACTIVITY"; //$NON-NLS-1$

    public static final String EXTRA_TAG_NAME = "tag"; //$NON-NLS-1$
    public static final String EXTRA_TAG_REMOTE_ID = "remoteId"; //$NON-NLS-1$
    public static final String EXTRA_START_TAB = "tab"; //$NON-NLS-1$

    protected static final int MENU_REFRESH_ID = MENU_SYNC_ID;

    protected static final int REQUEST_CODE_CAMERA = 1;
    protected static final int REQUEST_CODE_PICTURE = 2;


    private TagData tagData;

    @Autowired TagDataService tagDataService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired UpdateDao updateDao;

    private TabHost tabHost;
    private UpdateAdapter updateAdapter;
    private PeopleContainer tagMembers;
    private EditText addCommentField;
    private AsyncImageView picture;
    private EditText tagName;
    private boolean dataLoaded = false;

    // --- UI initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getListView().setOnKeyListener(null);

        // allow for text field entry, needed for android bug #2516
        OnTouchListener onTouch = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.requestFocusFromTouch();
                return false;
            }
        };
        ((EditText) findViewById(R.id.quickAddText)).setOnTouchListener(onTouch);
        ((EditText) findViewById(R.id.commentField)).setOnTouchListener(onTouch);

        if(getIntent().hasExtra(EXTRA_START_TAB))
            tabHost.setCurrentTab(getIntent().getIntExtra(EXTRA_START_TAB, 0));
    }

    @SuppressWarnings("nls")
    @Override
    protected View getListBody(ViewGroup root) {
        ViewGroup parent = (ViewGroup) getLayoutInflater().inflate(R.layout.task_list_body_tag, root, false);
        ViewGroup tabContent = (ViewGroup) parent.findViewById(android.R.id.tabcontent);

        String[] tabLabels = getResources().getStringArray(R.array.TVA_tabs);
        tabHost = (TabHost) parent.findViewById(android.R.id.tabhost);
        TabWidget tabWidget = (TabWidget) parent.findViewById(android.R.id.tabs);
        tabHost.setup();

        // set up tabs
        View taskList = super.getListBody(parent);
        tabContent.addView(taskList);
        addTab(tabWidget, taskList.getId(), "tasks", tabLabels[0]);
        addTab(tabWidget, R.id.tab_updates, "updates", tabLabels[1]);
        addTab(tabWidget, R.id.tab_settings, "members", tabLabels[2]);

        tabHost.setOnTabChangedListener(this);

        return parent;
    }

    private void addTab(TabWidget tabWidget, int contentId, String id, String label) {
        TabHost.TabSpec spec = tabHost.newTabSpec(id);
        spec.setContent(contentId);
        TextView textIndicator = (TextView) getLayoutInflater().inflate(R.layout.gd_tab_indicator, tabWidget, false);
        textIndicator.setText(label);
        spec.setIndicator(textIndicator);
        tabHost.addTab(spec);
    }

    @SuppressWarnings("nls")
    @Override
    public void onTabChanged(String tabId) {
        if(tabId.equals("tasks"))
            findViewById(R.id.taskListFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.taskListFooter).setVisibility(View.GONE);

        if(tabId.equals("updates"))
            findViewById(R.id.updatesFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.updatesFooter).setVisibility(View.GONE);

        if(tabId.equals("members"))
            findViewById(R.id.membersFooter).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.membersFooter).setVisibility(View.GONE);
    }


    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            menu.clear();

        MenuItem item;

        item = menu.add(Menu.NONE, MENU_ADDONS_ID, Menu.NONE,
                R.string.TLA_menu_addons);
        item.setIcon(android.R.drawable.ic_menu_set_as);

        item = menu.add(Menu.NONE, MENU_SETTINGS_ID, Menu.NONE,
                R.string.TLA_menu_settings);
        item.setIcon(android.R.drawable.ic_menu_preferences);

        item = menu.add(Menu.NONE, MENU_SORT_ID, Menu.NONE,
                R.string.TLA_menu_sort);
        item.setIcon(android.R.drawable.ic_menu_sort_by_size);

        item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                R.string.actfm_TVA_menu_refresh);
        item.setIcon(R.drawable.ic_menu_refresh);

        item = menu.add(Menu.NONE, MENU_HELP_ID, Menu.NONE,
                R.string.TLA_menu_help);
        item.setIcon(android.R.drawable.ic_menu_help);

        return true;
    }

    protected void setUpMemberPage() {
        tagMembers = (PeopleContainer) findViewById(R.id.members_container);
        tagName = (EditText) findViewById(R.id.tag_name);
        picture = (AsyncImageView) findViewById(R.id.picture);

        picture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(TagViewActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, new String[] {
                            getString(R.string.actfm_picture_camera),
                            getString(R.string.actfm_picture_gallery),
                });

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @SuppressWarnings("nls")
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if(which == 0) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQUEST_CODE_CAMERA);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(Intent.createChooser(intent,
                                    getString(R.string.actfm_TVA_tag_picture)), REQUEST_CODE_PICTURE);
                        }
                    }
                };

                // show a menu of available options
                new AlertDialog.Builder(TagViewActivity.this)
                .setAdapter(adapter, listener)
                .show().setOwnerActivity(TagViewActivity.this);
            }
        });

        findViewById(R.id.saveMembers).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                saveSettings();
            }
        });

        refreshMembersPage();
    }

    protected void setUpUpdateList() {
        TodorooCursor<Update> currentCursor = tagDataService.getUpdates(tagData);
        startManagingCursor(currentCursor);

        updateAdapter = new UpdateAdapter(this, R.layout.update_adapter_row,
                currentCursor, false, null);
        ((ListView)findViewById(R.id.tab_updates)).setAdapter(updateAdapter);

        final ImageButton quickAddButton = (ImageButton) findViewById(R.id.commentButton);
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
                quickAddButton.setVisibility((s.length() > 0) ? View.VISIBLE : View.GONE);
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
        quickAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });
    }

    // --- data loading

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        synchronized(this) {
            if(dataLoaded)
                return;
            dataLoaded = true;
        }

        String tag = getIntent().getStringExtra(EXTRA_TAG_NAME);
        long remoteId = getIntent().getLongExtra(EXTRA_TAG_REMOTE_ID, 0);

        if(tag == null && remoteId == 0)
            return;

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.or(TagData.NAME.eq(tag),
                Criterion.and(TagData.REMOTE_ID.gt(0), TagData.REMOTE_ID.eq(remoteId)))));
        try {
            tagData = new TagData();
            if(cursor.getCount() == 0) {
                tagData.setValue(TagData.NAME, tag);
                tagData.setValue(TagData.REMOTE_ID, remoteId);
                tagDataService.save(tagData);
            } else {
                cursor.moveToFirst();
                tagData.readFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        String fetchKey = LAST_FETCH_KEY + tagData.getId();
        long lastFetchDate = Preferences.getLong(fetchKey, 0);
        if(DateUtilities.now() > lastFetchDate + 300000L) {
            refreshData(false, false);
            Preferences.setLong(fetchKey, DateUtilities.now());
        }

        setUpUpdateList();
        setUpMemberPage();
    }

    private void refreshUpdatesList() {
        Cursor cursor = updateAdapter.getCursor();
        cursor.requery();
        startManagingCursor(cursor);
    }

    @Override
    public void loadTaskListContent(boolean requery) {
        super.loadTaskListContent(requery);
        int count = taskAdapter.getCursor().getCount();

        if(tagData != null && sortFlags <= SortHelper.FLAG_REVERSE_SORT &&
                count != tagData.getValue(TagData.TASK_COUNT)) {
            tagData.setValue(TagData.TASK_COUNT, count);
            tagDataService.save(tagData);
        }
    }

    @SuppressWarnings("nls")
    private void refreshMembersPage() {
        tagName.setText(tagData.getValue(TagData.NAME));
        picture.setUrl(tagData.getValue(TagData.PICTURE));

        TextView ownerLabel = (TextView) findViewById(R.id.tag_owner);
        try {
            if(tagData.getFlag(TagData.FLAGS, TagData.FLAG_EMERGENT)) {
                ownerLabel.setText(String.format("<%s>", getString(R.string.actfm_TVA_tag_owner_none)));
            } else if(tagData.getValue(TagData.USER_ID) == 0) {
                ownerLabel.setText(Preferences.getStringValue(ActFmPreferenceService.PREF_NAME));
            } else {
                JSONObject owner = new JSONObject(tagData.getValue(TagData.USER));
                ownerLabel.setText(owner.getString("name"));
            }
        } catch (JSONException e) {
            Log.e("tag-view-activity", "json error refresh owner", e);
            ownerLabel.setText("<error>");
            System.err.println(tagData.getValue(TagData.USER));
        }

        tagMembers.removeAllViews();
        String peopleJson = tagData.getValue(TagData.MEMBERS);
        if(!TextUtils.isEmpty(peopleJson)) {
            try {
                JSONArray people = new JSONArray(peopleJson);
                for(int i = 0; i < people.length(); i++) {
                    JSONObject person = people.getJSONObject(i);
                    TextView textView = null;

                    if(person.has("id") && person.getLong("id") == ActFmPreferenceService.userId())
                        textView = tagMembers.addPerson(Preferences.getStringValue(ActFmPreferenceService.PREF_NAME));
                    else if(!TextUtils.isEmpty(person.optString("name")))
                        textView = tagMembers.addPerson(person.getString("name"));
                    else if(!TextUtils.isEmpty(person.optString("email")))
                        textView = tagMembers.addPerson(person.getString("email"));

                    if(textView != null) {
                        textView.setTag(person);
                        textView.setEnabled(false);
                    }
                }
            } catch (JSONException e) {
                System.err.println(peopleJson);
                Log.e("tag-view-activity", "json error refresh members", e);
            }
        }

        tagMembers.addPerson(""); //$NON-NLS-1$
    }

    /** refresh the list with latest data from the web */
    private void refreshData(final boolean manual, boolean bypassTagShow) {
        final boolean noRemoteId = tagData.getValue(TagData.REMOTE_ID) == 0;

        final ProgressDialog progressDialog;
        if(manual && !noRemoteId)
            progressDialog = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        else
            progressDialog = null;

        Thread tagShowThread = new Thread(new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                try {
                    String oldName = tagData.getValue(TagData.NAME);
                    actFmSyncService.fetchTag(tagData);
                    if(noRemoteId && tagData.getValue(TagData.REMOTE_ID) > 0) {
                        refreshData(manual, true);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshUpdatesList();
                                refreshMembersPage();
                            }
                        });
                    }

                    if(!oldName.equals(tagData.getValue(TagData.NAME))) {
                        TagService.getInstance().rename(oldName,
                                tagData.getValue(TagData.NAME));
                    }

                } catch (IOException e) {
                    Log.e("tag-view-activity", "error-fetching-task-io", e);
                } catch (JSONException e) {
                    Log.e("tag-view-activity", "error-fetching-task", e);
                }
            }
        });
        if(!bypassTagShow)
            tagShowThread.start();

        if(noRemoteId)
            return;

        actFmSyncService.fetchTasksForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadTaskListContent(true);
                        DialogUtilities.dismissDialog(TagViewActivity.this, progressDialog);
                    }
                });
            }
        });

        actFmSyncService.fetchUpdatesForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshUpdatesList();
                        DialogUtilities.dismissDialog(TagViewActivity.this, progressDialog);
                    }
                });
            }
        });
    }

    // --- receivers

    private final BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
        @SuppressWarnings("nls")
        @Override
        public void onReceive(Context context, Intent intent) {
            System.err.println("TVA thug hustlin - ");

            if(!intent.hasExtra("tag_id"))
                return;
            System.err.println(Long.toString(tagData.getValue(TagData.REMOTE_ID)) +
                    " VS " + intent.getStringExtra("tag_id"));
            if(!Long.toString(tagData.getValue(TagData.REMOTE_ID)).equals(intent.getStringExtra("tag_id")))
                return;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.err.println("REFRESH updates list pa-pa-pa");
                    refreshUpdatesList();
                }
            });
            refreshData(false, true);

            NotificationManager nm = new AndroidNotificationManager(ContextManager.getContext());
            nm.cancel(tagData.getValue(TagData.REMOTE_ID).intValue());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BROADCAST_TAG_ACTIVITY);
        registerReceiver(notifyReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(notifyReceiver);
    }

    // --- events

    private void saveSettings() {
        String oldName = tagData.getValue(TagData.NAME);
        String newName = tagName.getText().toString();

        if(!oldName.equals(newName)) {
            tagData.setValue(TagData.NAME, newName);
            TagService.getInstance().rename(oldName, newName);
            tagData.setFlag(TagData.FLAGS, TagData.FLAG_EMERGENT, false);
        }

        JSONArray members = tagMembers.toJSONArray();
        tagData.setValue(TagData.MEMBERS, members.toString());
        tagData.setValue(TagData.MEMBER_COUNT, members.length());
        Flags.set(Flags.TOAST_ON_SAVE);
        tagDataService.save(tagData);

        refreshMembersPage();
    }

    @SuppressWarnings("nls")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            Bitmap bitmap = data.getParcelableExtra("data");
            if(bitmap != null) {
                picture.setImageBitmap(bitmap);
                uploadTagPicture(bitmap);
            }
        } else if(requestCode == REQUEST_CODE_PICTURE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            String path;

            if(cursor != null) {
                try {
                    int column_index = cursor
                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    path = cursor.getString(column_index);
                } finally {
                    cursor.close();
                }
            } else {
                path = uri.getPath();
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if(bitmap != null) {
                picture.setImageBitmap(bitmap);
                uploadTagPicture(bitmap);
            }
        }
    }

    private void uploadTagPicture(final Bitmap bitmap) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = actFmSyncService.setTagPicture(tagData.getValue(TagData.REMOTE_ID), bitmap);
                    tagData.setValue(TagData.PICTURE, url);
                    Flags.set(Flags.SUPPRESS_SYNC);
                    tagDataService.save(tagData);
                } catch (IOException e) {
                    DialogUtilities.okDialog(TagViewActivity.this, e.toString(), null);
                }
            }
        }).start();
    }

    private void addComment() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "tag_comment"); //$NON-NLS-1$
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TAG, tagData.getId());
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        updateDao.createNew(update);

        addCommentField.setText(""); //$NON-NLS-1$
        refreshUpdatesList();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {
        case MENU_REFRESH_ID:
            refreshData(true, false);
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

}
