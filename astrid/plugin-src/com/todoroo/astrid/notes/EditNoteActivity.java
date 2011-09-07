package com.todoroo.astrid.notes;

import greendroid.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.utility.Flags;

public class EditNoteActivity extends ListActivity {

    public static final String EXTRA_TASK_ID = "task"; //$NON-NLS-1$
    private static final int MENU_REFRESH_ID = Menu.FIRST;
    private static final String LAST_FETCH_KEY = "task-fetch-"; //$NON-NLS-1$

    private Task task;

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired MetadataService metadataService;
    @Autowired UpdateDao updateDao;

    private final ArrayList<NoteOrUpdate> items = new ArrayList<NoteOrUpdate>();
    private NoteAdapter adapter;
    private EditText commentField;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Dialog);
        setContentView(R.layout.edit_note_activity);

        findViewById(R.id.dismiss_comments).setOnClickListener(dismissCommentsListener);

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        task = PluginServices.getTaskService().fetchById(taskId, Task.NOTES, Task.ID, Task.REMOTE_ID, Task.TITLE);
        if(task == null) {
            finish();
            return;
        }

        setTitle(task.getValue(Task.TITLE));

        setUpInterface();
        setUpListAdapter();

        if(actFmPreferenceService.isLoggedIn()) {
            findViewById(R.id.add_comment).setVisibility(View.VISIBLE);

            if(task.getValue(Task.REMOTE_ID) == 0)
                refreshData(true);
            else {
                String fetchKey = LAST_FETCH_KEY + task.getId();
                long lastFetchDate = Preferences.getLong(fetchKey, 0);
                if(DateUtilities.now() > lastFetchDate + 300000L) {
                    refreshData(false);
                    Preferences.setLong(fetchKey, DateUtilities.now());
                } else {
                    loadingText.setText(R.string.ENA_no_comments);
                    if(items.size() == 0)
                        loadingText.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // --- UI preparation

    private void setUpInterface() {
        final View commentButton = findViewById(R.id.commentButton);
        commentField = (EditText) findViewById(R.id.commentField);
        commentField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_NULL && commentField.getText().length() > 0) {
                    addComment();
                    return true;
                }
                return false;
            }
        });
        commentField.addTextChangedListener(new TextWatcher() {
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
        commentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });

        if(!TextUtils.isEmpty(task.getValue(Task.NOTES))) {
            TextView notes = new TextView(this);
            notes.setLinkTextColor(Color.rgb(100, 160, 255));
            notes.setTextSize(18);
            getListView().addHeaderView(notes);
            notes.setText(task.getValue(Task.NOTES));
            notes.setPadding(5, 10, 5, 10);
            Linkify.addLinks(notes, Linkify.ALL);
        }
        loadingText = (TextView) findViewById(R.id.loading);
    }

    private void setUpListAdapter() {
        items.clear();
        TodorooCursor<Metadata> notes = metadataService.query(
                Query.select(Metadata.PROPERTIES).where(
                        MetadataCriteria.byTaskAndwithKey(task.getId(),
                                NoteMetadata.METADATA_KEY)));
        try {
            Metadata metadata = new Metadata();
            for(notes.moveToFirst(); !notes.isAfterLast(); notes.moveToNext()) {
                metadata.readFromCursor(notes);
                items.add(NoteOrUpdate.fromMetadata(metadata));
            }
        } finally {
            notes.close();
        }

        if(task.getValue(Task.REMOTE_ID) > 0) {
            TodorooCursor<Update> updates = updateDao.query(Query.select(Update.PROPERTIES).where(
                            Update.TASK.eq(task.getValue(Task.REMOTE_ID))));
            try {
                Update update = new Update();
                for(updates.moveToFirst(); !updates.isAfterLast(); updates.moveToNext()) {
                    update.readFromCursor(updates);
                    items.add(NoteOrUpdate.fromUpdate(update));
                }
            } finally {
                updates.close();
            }
        }

        Collections.sort(items, new Comparator<NoteOrUpdate>() {
            @Override
            public int compare(NoteOrUpdate a, NoteOrUpdate b) {
                if(a.createdAt > b.createdAt)
                    return 1;
                else if (a.createdAt == b.createdAt)
                    return 0;
                else
                    return -1;
            }
        });
        adapter = new NoteAdapter(this, R.id.name, items);
        setListAdapter(adapter);

        getListView().setSelection(items.size() - 1);
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

    // --- events

    private void refreshData(boolean manual) {
        final ProgressDialog progressDialog;
        if(manual)
            progressDialog = DialogUtilities.progressDialog(this, getString(R.string.DLG_please_wait));
        else
            progressDialog = null;

        if(task.getValue(Task.REMOTE_ID) == 0) {
            // push task if it hasn't been pushed
            new Thread(new Runnable() {
                @Override
                public void run() {
                    actFmSyncService.pushTask(task.getId());
                    refreshData(false);
                    DialogUtilities.dismissDialog(EditNoteActivity.this, progressDialog);
                }
            }).start();
            return;
        }

        actFmSyncService.fetchUpdatesForTask(task, manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setUpListAdapter();
                        loadingText.setText(R.string.ENA_no_comments);
                        loadingText.setVisibility(items.size() == 0 ? View.VISIBLE : View.GONE);
                        DialogUtilities.dismissDialog(EditNoteActivity.this, progressDialog);
                    }
                });
            }
        });
    }

    private void addComment() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, commentField.getText().toString());
        update.setValue(Update.ACTION_CODE, "task_comment"); //$NON-NLS-1$
        update.setValue(Update.USER_ID, 0L);
        update.setValue(Update.TASK, task.getValue(Task.REMOTE_ID));
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        Flags.checkAndClear(Flags.ACTFM_SUPPRESS_SYNC);
        updateDao.createNew(update);

        commentField.setText(""); //$NON-NLS-1$
        setUpListAdapter();

        StatisticsService.reportEvent(StatisticsConstants.ACTFM_TASK_COMMENT);
    }

    private final OnClickListener dismissCommentsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // handle my own menus
        switch (item.getItemId()) {

        case MENU_REFRESH_ID: {
            refreshData(true);
            return true;
        }

        default: return false;
        }
    }

    // --- adapter

    private static class NoteOrUpdate {
        private final String picture;
        private final String title;
        private final String body;
        private final long createdAt;

        public NoteOrUpdate(String picture, String title, String body,
                long createdAt) {
            super();
            this.picture = picture;
            this.title = title;
            this.body = body;
            this.createdAt = createdAt;
        }

        public static NoteOrUpdate fromMetadata(Metadata m) {
            if(!m.containsNonNullValue(NoteMetadata.THUMBNAIL))
                m.setValue(NoteMetadata.THUMBNAIL, ""); //$NON-NLS-1$

            return new NoteOrUpdate(m.getValue(NoteMetadata.THUMBNAIL),
                    m.getValue(NoteMetadata.TITLE),
                    m.getValue(NoteMetadata.BODY),
                    m.getValue(Metadata.CREATION_DATE));
        }

        @SuppressWarnings("nls")
        public static NoteOrUpdate fromUpdate(Update u) {
            JSONObject user = ActFmPreferenceService.userFromModel(u);

            String description = u.getValue(Update.ACTION);
            String message = u.getValue(Update.MESSAGE);
            if(u.getValue(Update.ACTION_CODE).equals("task_comment"))
                description = message;
            else if(!TextUtils.isEmpty(message))
                description += " " + message;

            return new NoteOrUpdate(user.optString("picture"),
                    user.optString("name", ""),
                    description,
                    u.getValue(Update.CREATION_DATE));
        }

        @Override
        public String toString() {
            return title + ": " + body; //$NON-NLS-1$
        }
    }

    private class NoteAdapter extends ArrayAdapter<NoteOrUpdate> {

        public NoteAdapter(Context context, int textViewResourceId, List<NoteOrUpdate> list) {
            super(context, textViewResourceId, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.update_adapter_row, parent, false);
            }
            bindView(convertView, items.get(position));
            return convertView;
        }

        /** Helper method to set the contents and visibility of each field */
        public synchronized void bindView(View view, NoteOrUpdate item) {
            // picture
            final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
                if(TextUtils.isEmpty(item.picture))
                    pictureView.setVisibility(View.GONE);
                else {
                    pictureView.setVisibility(View.VISIBLE);
                    pictureView.setUrl(item.picture);
                }
            }

            // name
            final TextView nameView = (TextView)view.findViewById(R.id.title); {
                nameView.setText(item.title);
            }

            // description
            final TextView descriptionView = (TextView)view.findViewById(R.id.description); {
                descriptionView.setText(item.body);
                Linkify.addLinks(descriptionView, Linkify.ALL);
            }

            // date
            final TextView date = (TextView)view.findViewById(R.id.date); {
                CharSequence dateString = DateUtils.getRelativeTimeSpanString(item.createdAt,
                        DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
                date.setText(dateString);
            }
        }
    }
}
