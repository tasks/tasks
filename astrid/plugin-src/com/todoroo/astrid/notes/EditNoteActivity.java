package com.todoroo.astrid.notes;

import greendroid.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.SyncV2Service.SyncResultCallback;
import com.todoroo.astrid.utility.Flags;

public class EditNoteActivity extends LinearLayout {



    public static final String EXTRA_TASK_ID = "task"; //$NON-NLS-1$
    private static final String LAST_FETCH_KEY = "task-fetch-"; //$NON-NLS-1$

    private Task task;

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired MetadataService metadataService;
    @Autowired UpdateDao updateDao;

    private final ArrayList<NoteOrUpdate> items = new ArrayList<NoteOrUpdate>();
    private EditText commentField;
    private TextView loadingText;
    private final View commentsBar;
    private final View parentView;
    private int commentItems = 10;
    private final List<UpdatesChangedListener> listeners = new LinkedList<UpdatesChangedListener>();

    public interface UpdatesChangedListener {
        public void updatesChanged();
        public void commentAdded();
    }

    public EditNoteActivity(Context context, View parent, long t) {
        super(context);

        DependencyInjectionService.getInstance().inject(this);
        setOrientation(VERTICAL);

        commentsBar = parent.findViewById(R.id.updatesFooter);
        parentView = parent;


        loadViewForTaskID(t);
    }

    public void loadViewForTaskID(long t){

        task = PluginServices.getTaskService().fetchById(t, Task.NOTES, Task.ID, Task.REMOTE_ID, Task.TITLE);
        if(task == null) {
            return;
        }
        setUpInterface();
        setUpListAdapter();

        if(actFmPreferenceService.isLoggedIn()) {
            if(task.getValue(Task.REMOTE_ID) == 0)
                refreshData(true, null);
            else {
                String fetchKey = LAST_FETCH_KEY + task.getId();
                long lastFetchDate = Preferences.getLong(fetchKey, 0);
                if(DateUtilities.now() > lastFetchDate + 300000L) {
                    refreshData(false, null);
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
        final View commentButton = commentsBar.findViewById(R.id.commentButton);
        commentField = (EditText) commentsBar.findViewById(R.id.commentField);
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
            TextView notes = new TextView(getContext());
            notes.setLinkTextColor(Color.rgb(100, 160, 255));
            notes.setTextSize(18);
            notes.setText(task.getValue(Task.NOTES));
            notes.setPadding(5, 10, 5, 10);
            Linkify.addLinks(notes, Linkify.ALL);
        }
        //TODO add loading text back in
        //        loadingText = (TextView) findViewById(R.id.loading);
        loadingText = new TextView(getContext());
    }

    private void setUpListAdapter() {
        items.clear();
        this.removeAllViews();
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
                if(a.createdAt < b.createdAt)
                    return 1;
                else if (a.createdAt == b.createdAt)
                    return 0;
                else
                    return -1;
            }
        });

        for (int i = 0; i < Math.min(items.size(), commentItems); i++) {
            View notesView = this.getUpdateNotes(items.get(i), this);
            this.addView(notesView);
        }


        if ( items.size() > commentItems) {
            Button loadMore = new Button(getContext());
            loadMore.setText(R.string.TEA_load_more);
            loadMore.setBackgroundColor(Color.alpha(0));
            loadMore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    commentItems += 10;
                    setUpListAdapter();
                }
            });
            this.addView(loadMore);
        }
        else if (items.size() == 0) {
            TextView noUpdates = new TextView(getContext());
            noUpdates.setText(R.string.TEA_no_activity);
            noUpdates.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            noUpdates.setPadding(10, 10, 10, 10);
            noUpdates.setGravity(Gravity.CENTER);
            noUpdates.setTextSize(20);
            this.addView(noUpdates);
        }


        for (UpdatesChangedListener l : listeners) {
            l.updatesChanged();
        }
    }



    public View getUpdateNotes(NoteOrUpdate note, ViewGroup parent) {
        View convertView = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.update_adapter_row, parent, false);

        bindView(convertView, note);
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
    /*
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
    }*/

    // --- events

    public void refreshData(boolean manual, SyncResultCallback existingCallback) {
        final SyncResultCallback callback;
        if(existingCallback != null)
            callback = existingCallback;
        else {
            callback = new ProgressBarSyncResultCallback(
                    ((Activity)getContext()), (ProgressBar)parentView.findViewById(R.id.progressBar), R.id.progressBar, new Runnable() {
                        @Override
                        public void run() {
                            setUpListAdapter();
                            loadingText.setText(R.string.ENA_no_comments);
                            loadingText.setVisibility(items.size() == 0 ? View.VISIBLE : View.GONE);
                        }
                    });

            callback.started();
            callback.incrementMax(100);
        }

        // push task if it hasn't been pushed
        if(task.getValue(Task.REMOTE_ID) == 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    actFmSyncService.pushTask(task.getId());
                    refreshData(false, callback);
                }
            }).start();
            return;
        }

        actFmSyncService.fetchUpdatesForTask(task, manual, new Runnable() {
            @Override
            public void run() {
                callback.incrementProgress(50);
                callback.finished();
            }
        });
        callback.incrementProgress(50);
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

        for (UpdatesChangedListener l : listeners) {
            l.commentAdded();
        }
    }

    public int numberOfComments() {
        return items.size();
    }
    //TODO figure out what to do with menu
    /*
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
            refreshData(true, null);
            return true;
        }

        default: return false;
        }
    }*/

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

    public void addListener(UpdatesChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(UpdatesChangedListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }
}
