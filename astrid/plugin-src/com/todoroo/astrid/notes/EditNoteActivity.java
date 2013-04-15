/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.FetchHistory;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;
import com.todoroo.astrid.utility.ResourceDrawableCache;

import edu.mit.mobile.android.imagecache.ImageCache;

public class EditNoteActivity extends LinearLayout implements TimerActionListener {

    public static final String EXTRA_TASK_ID = "task"; //$NON-NLS-1$

    private Task task;

    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired MetadataService metadataService;
    @Autowired UserActivityDao userActivityDao;
    @Autowired TaskService taskService;
    @Autowired TaskDao taskDao;

    private final ArrayList<NoteOrUpdate> items = new ArrayList<NoteOrUpdate>();
    private EditText commentField;
    private TextView loadingText;
    private final View commentsBar;
    private View timerView;
    private View commentButton;
    private int commentItems = 10;
    private ImageButton pictureButton;
    private Bitmap pendingCommentPicture = null;
    private final Fragment fragment;

    private final AstridActivity activity;

    private final Resources resources;

    private final ImageCache imageCache;
    private final int cameraButton;
    private final String linkColor;
    private int historyCount = 0;

    private final int color;
    private final int grayColor;

    private final SyncMessageCallback callback = new SyncMessageCallback() {
        @Override
        public void runOnSuccess() {
            synchronized(this) {
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (task == null)
                                return;
                            fetchTask(task.getId());
                            if (task == null)
                                return;
                            setUpListAdapter();
                            loadingText.setText(R.string.ENA_no_comments);
                            loadingText.setVisibility(items.size() == 0 ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            }
        }
        @Override
        public void runOnErrors(List<JSONArray> errors) {/**/}
    };

    private static boolean respondToPicture = false;

    private final List<UpdatesChangedListener> listeners = new LinkedList<UpdatesChangedListener>();

    public interface UpdatesChangedListener {
        public void updatesChanged();
        public void commentAdded();
    }

    public EditNoteActivity(Fragment fragment, View parent, long t) {
        super(fragment.getActivity());
        DependencyInjectionService.getInstance().inject(this);

        imageCache = AsyncImageView.getImageCache();
        this.fragment = fragment;

        this.activity = (AstridActivity) fragment.getActivity();

        this.resources = fragment.getResources();
        TypedValue tv = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.asTextColor, tv, false);
        color = tv.data;

        fragment.getActivity().getTheme().resolveAttribute(R.attr.asDueDateColor, tv, false);
        grayColor = tv.data;


        linkColor = UpdateAdapter.getLinkColor(fragment);

        cameraButton = getDefaultCameraButton();

        setOrientation(VERTICAL);

        commentsBar = parent.findViewById(R.id.updatesFooter);

        loadViewForTaskID(t);
    }

    private int getDefaultCameraButton() {
        return R.drawable.camera_button;
    }

    private void fetchTask(long id) {
        task = PluginServices.getTaskService().fetchById(id, Task.NOTES, Task.ID, Task.UUID, Task.TITLE, Task.HISTORY_FETCH_DATE, Task.HISTORY_HAS_MORE, Task.USER_ACTIVITIES_PUSHED_AT, Task.ATTACHMENTS_PUSHED_AT);
    }

    public void loadViewForTaskID(long t){
        try {
            fetchTask(t);
        } catch (SQLiteException e) {
            StartupService.handleSQLiteError(ContextManager.getContext(), e);
        }
        if(task == null) {
            return;
        }
        setUpInterface();
        setUpListAdapter();

        if(actFmPreferenceService.isLoggedIn()) {
            long pushedAt = task.getValue(Task.USER_ACTIVITIES_PUSHED_AT);
            if(DateUtilities.now() - pushedAt > DateUtilities.ONE_HOUR / 2) {
                refreshData();
            } else {
                loadingText.setText(R.string.ENA_no_comments);
                if(items.size() == 0)
                    loadingText.setVisibility(View.VISIBLE);
            }
        }
    }



    // --- UI preparation

    private void setUpInterface() {
        timerView = commentsBar.findViewById(R.id.timer_container);
        commentButton = commentsBar.findViewById(R.id.commentButton);
        commentField = (EditText) commentsBar.findViewById(R.id.commentField);

        final boolean showTimerShortcut = Preferences.getBoolean(R.string.p_show_timer_shortcut, false);

        if (showTimerShortcut) {
            commentField.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        timerView.setVisibility(View.GONE);
                        commentButton.setVisibility(View.VISIBLE);
                    }
                    else {
                        timerView.setVisibility(View.VISIBLE);
                        commentButton.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            timerView.setVisibility(View.GONE);
        }

        commentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                commentButton.setVisibility((s.length() > 0 || pendingCommentPicture != null) ? View.VISIBLE
                        : View.GONE);
                if (showTimerShortcut)
                    timerView.setVisibility((s.length() > 0 || pendingCommentPicture != null) ? View.GONE
                            : View.VISIBLE);
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
        commentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });

        final ClearImageCallback clearImage = new ClearImageCallback() {
            @Override
            public void clearImage() {
                pendingCommentPicture = null;
                pictureButton.setImageResource(cameraButton);
            }
        };
        pictureButton = (ImageButton) commentsBar.findViewById(R.id.picture);
        pictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingCommentPicture != null)
                    ActFmCameraModule.showPictureLauncher(fragment, clearImage);
                else
                    ActFmCameraModule.showPictureLauncher(fragment, null);
                respondToPicture = true;
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

        if (activity != null) {
            Bitmap bitmap = activity.getIntent().getParcelableExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS);
            if (bitmap != null) {
                pendingCommentPicture = bitmap;
                pictureButton.setImageBitmap(pendingCommentPicture);
            }
        }

        //TODO add loading text back in
        //        loadingText = (TextView) findViewById(R.id.loading);
        loadingText = new TextView(getContext());
    }

    private void setUpListAdapter() {
        items.clear();
        this.removeAllViews();
        historyCount = 0;
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

        User self = UpdateAdapter.getSelfUser();

        TodorooCursor<UserActivity> updates = taskService.getActivityAndHistoryForTask(task);
        try {
            UserActivity update = new UserActivity();
            History history = new History();
            User user = new User();
            for(updates.moveToFirst(); !updates.isAfterLast(); updates.moveToNext()) {
                update.clear();
                user.clear();

                String type = updates.getString(UpdateAdapter.TYPE_PROPERTY_INDEX);
                NoteOrUpdate noa;
                boolean isSelf;
                if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
                    UpdateAdapter.readUserActivityProperties(updates, update);
                    isSelf = Task.USER_ID_SELF.equals(update.getValue(UserActivity.USER_UUID));
                    UpdateAdapter.readUserProperties(updates, user, self, isSelf);
                    noa = NoteOrUpdate.fromUpdateOrHistory(activity, update, null, user, linkColor);
                } else {
                    UpdateAdapter.readHistoryProperties(updates, history);
                    isSelf = Task.USER_ID_SELF.equals(history.getValue(History.USER_UUID));
                    UpdateAdapter.readUserProperties(updates, user, self, isSelf);
                    noa = NoteOrUpdate.fromUpdateOrHistory(activity, null, history, user, linkColor);
                    historyCount++;
                }
                if(noa != null)
                    items.add(noa);
            }
        } finally {
            updates.close();
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

        if (items.size() > commentItems || task.getValue(Task.HISTORY_HAS_MORE) > 0) {
            Button loadMore = new Button(getContext());
            loadMore.setText(R.string.TEA_load_more);
            loadMore.setTextColor(activity.getResources().getColor(R.color.task_edit_deadline_gray));
            loadMore.setBackgroundColor(Color.alpha(0));
            loadMore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                    commentItems += 10;
                    setUpListAdapter();
                    if (task.getValue(Task.HISTORY_HAS_MORE) > 0)
                        new FetchHistory<Task>(taskDao, Task.HISTORY_FETCH_DATE, Task.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TASKS,
                                task.getUuid(), task.getValue(Task.TITLE), 0, historyCount, callback).execute();
                }
            });
            this.addView(loadMore);
        }
        else if (items.size() == 0) {
            TextView noUpdates = new TextView(getContext());
            noUpdates.setText(R.string.TEA_no_activity);
            noUpdates.setTextColor(activity.getResources().getColor(R.color.task_edit_deadline_gray));
            noUpdates.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            noUpdates.setPadding(10, 10, 10, 10);
            noUpdates.setGravity(Gravity.CENTER);
            noUpdates.setTextSize(16);
            this.addView(noUpdates);
        }

        for (UpdatesChangedListener l : listeners) {
            l.updatesChanged();
        }
    }



    public View getUpdateNotes(NoteOrUpdate note, ViewGroup parent) {
        View convertView = ((Activity)getContext()).getLayoutInflater().inflate(
                R.layout.update_adapter_row, parent, false);

        bindView(convertView, note);
        return convertView;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void bindView(View view, NoteOrUpdate item) {
        // picture
        final AsyncImageView pictureView = (AsyncImageView)view.findViewById(R.id.picture); {
            pictureView.setDefaultImageDrawable(ResourceDrawableCache.getImageDrawableFromId(resources, R.drawable.icn_default_person_image));
            pictureView.setUrl(item.picture);

        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(item.title);
            if (NameMaps.TABLE_ID_HISTORY.equals(item.type))
                nameView.setTextColor(grayColor);
            else
                nameView.setTextColor(color);
            Linkify.addLinks(nameView, Linkify.ALL);
        }

        // date
        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(item.createdAt,
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }

        // picture
        final AsyncImageView commentPictureView = (AsyncImageView)view.findViewById(R.id.comment_picture); {
            UpdateAdapter.setupImagePopupForCommentView(view, commentPictureView, item.pictureThumb, item.pictureFull, item.commentBitmap, item.title.toString(), fragment, imageCache);
        }
    }

    public void refreshData() {
        if(!task.containsNonNullValue(Task.UUID)) {
            return;
        }

        ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<UserActivity>(UserActivity.class, null, task.getValue(Task.USER_ACTIVITIES_PUSHED_AT), BriefMe.TASK_ID_KEY, task.getUuid()), callback);
        ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<TaskAttachment>(TaskAttachment.class, null, task.getValue(Task.ATTACHMENTS_PUSHED_AT), BriefMe.TASK_ID_KEY, task.getUuid()), new SyncMessageCallback() {
            @Override
            public void runOnSuccess() {
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TaskEditFragment tef = activity.getTaskEditFragment();
                            if (tef != null) {
                                tef.refreshFilesDisplay();
                            }
                        }
                    });
                }
            }

            @Override
            public void runOnErrors(List<JSONArray> errors) {/**/}
        });
        new FetchHistory<Task>(taskDao, Task.HISTORY_FETCH_DATE, Task.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TASKS,
                task.getUuid(), task.getValue(Task.TITLE), task.getValue(Task.HISTORY_FETCH_DATE), 0, callback).execute();
    }

    private void addComment() {
        addComment(commentField.getText().toString(), UserActivity.ACTION_TASK_COMMENT, task.getUuid(), task.getValue(Task.TITLE), true);
    }


    @SuppressWarnings("nls")
    private void addComment(String message, String actionCode, String uuid, String title, boolean usePicture) {
        // Allow for users to just add picture
        if (TextUtils.isEmpty(message) && usePicture) {
            message = " ";
        }
        UserActivity userActivity = new UserActivity();
        userActivity.setValue(UserActivity.MESSAGE, message);
        userActivity.setValue(UserActivity.ACTION, actionCode);
        userActivity.setValue(UserActivity.USER_UUID, Task.USER_ID_SELF);
        userActivity.setValue(UserActivity.TARGET_ID, uuid);
        userActivity.setValue(UserActivity.TARGET_NAME, title);
        userActivity.setValue(UserActivity.CREATED_AT, DateUtilities.now());
        if (usePicture && pendingCommentPicture != null) {
            JSONObject pictureJson = RemoteModel.PictureHelper.savePictureJson(activity, pendingCommentPicture);
            if (pictureJson != null)
                userActivity.setValue(UserActivity.PICTURE, pictureJson.toString());
        }

        userActivityDao.createNew(userActivity);
        if (commentField != null)
            commentField.setText(""); //$NON-NLS-1$

        pendingCommentPicture = usePicture ? null : pendingCommentPicture;
        if (usePicture) {
            if (activity != null)
                activity.getIntent().removeExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS);
        }
        if (pictureButton != null)
            pictureButton.setImageResource(cameraButton);
        StatisticsService.reportEvent(StatisticsConstants.ACTFM_TASK_COMMENT);

        setUpListAdapter();
        for (UpdatesChangedListener l : listeners) {
            l.commentAdded();
        }
    }

    public int numberOfComments() {
        return items.size();
    }

    private static class NoteOrUpdate {
        private final String type;
        private final String picture;
        private final Spanned title;
        private final String pictureThumb;
        private final String pictureFull;
        private final Bitmap commentBitmap;
        private final long createdAt;

        public NoteOrUpdate(String picture, Spanned title, String pictureThumb, String pictureFull, Bitmap commentBitmap, long createdAt, String type) {
            super();
            this.picture = picture;
            this.title = title;
            this.pictureThumb = pictureThumb;
            this.pictureFull = pictureFull;
            this.commentBitmap = commentBitmap;
            this.createdAt = createdAt;
            this.type = type;
        }

        public static NoteOrUpdate fromMetadata(Metadata m) {
            if(!m.containsNonNullValue(NoteMetadata.THUMBNAIL))
                m.setValue(NoteMetadata.THUMBNAIL, ""); //$NON-NLS-1$
            if(!m.containsNonNullValue(NoteMetadata.COMMENT_PICTURE))
                m.setValue(NoteMetadata.COMMENT_PICTURE, ""); //$NON-NLS-1$
            Spanned title = Html.fromHtml(String.format("%s\n%s", m.getValue(NoteMetadata.TITLE), m.getValue(NoteMetadata.BODY))); //$NON-NLS-1$
            return new NoteOrUpdate(m.getValue(NoteMetadata.THUMBNAIL),
                    title,
                    m.getValue(NoteMetadata.COMMENT_PICTURE),
                    m.getValue(NoteMetadata.COMMENT_PICTURE),
                    null,
                    m.getValue(Metadata.CREATION_DATE), null);
        }

        public static NoteOrUpdate fromUpdateOrHistory(AstridActivity context, UserActivity u, History history, User user, String linkColor) {
            String userImage = ""; //$NON-NLS-1$
            String pictureThumb = ""; //$NON-NLS-1$
            String pictureFull = ""; //$NON-NLS-1$
            Spanned title;
            Bitmap commentBitmap = null;
            long createdAt = 0;
            String type = null;

            if (u != null) {
                pictureThumb = u.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_MEDIUM);
                pictureFull = u.getPictureUrl(UserActivity.PICTURE, RemoteModel.PICTURE_LARGE);
                if (TextUtils.isEmpty(pictureThumb))
                    commentBitmap = u.getPictureBitmap(UserActivity.PICTURE);
                title = UpdateAdapter.getUpdateComment(context, u, user, linkColor, UpdateAdapter.FROM_TASK_VIEW);
                userImage = ""; //$NON-NLS-1$
                if (user.containsNonNullValue(UpdateAdapter.USER_PICTURE))
                    userImage = user.getPictureUrl(UpdateAdapter.USER_PICTURE, RemoteModel.PICTURE_THUMB);
                createdAt = u.getValue(UserActivity.CREATED_AT);
                type = NameMaps.TABLE_ID_USER_ACTIVITY;
            } else {
                if (user.containsNonNullValue(UpdateAdapter.USER_PICTURE))
                    userImage = user.getPictureUrl(UpdateAdapter.USER_PICTURE, RemoteModel.PICTURE_THUMB);
                title = new SpannableString(UpdateAdapter.getHistoryComment(context, history, user, linkColor, UpdateAdapter.FROM_TASK_VIEW));
                createdAt = history.getValue(History.CREATED_AT);
                type = NameMaps.TABLE_ID_HISTORY;
            }

            return new NoteOrUpdate(userImage,
                    title,
                    pictureThumb,
                    pictureFull,
                    commentBitmap,
                    createdAt,
                    type);
        }

    }

    public void addListener(UpdatesChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(UpdatesChangedListener listener) {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }

    @Override
    public void timerStarted(Task t) {
        addComment(String.format("%s %s",  //$NON-NLS-1$
                getContext().getString(R.string.TEA_timer_comment_started),
                DateUtilities.getTimeString(getContext(), new Date())),
                UserActivity.ACTION_TASK_COMMENT,
                t.getUuid(),
                t.getValue(Task.TITLE),
                false);
    }

    @Override
    public void timerStopped(Task t) {
        String elapsedTime = DateUtils.formatElapsedTime(t.getValue(Task.ELAPSED_SECONDS));
        addComment(String.format("%s %s\n%s %s", //$NON-NLS-1$
                getContext().getString(R.string.TEA_timer_comment_stopped),
                DateUtilities.getTimeString(getContext(), new Date()),
                getContext().getString(R.string.TEA_timer_comment_spent),
                elapsedTime), UserActivity.ACTION_TASK_COMMENT,
                t.getUuid(),
                t.getValue(Task.TITLE),
                false);
    }

    /*
     * Call back from edit task when picture is added
     */
    public boolean activityResult(int requestCode, int resultCode, Intent data) {

        if (respondToPicture) {
            respondToPicture = false;
            CameraResultCallback callback = new CameraResultCallback() {
                @Override
                public void handleCameraResult(Bitmap bitmap) {
                    if (activity != null) {
                        activity.getIntent().putExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS, bitmap);
                    }
                    pendingCommentPicture = bitmap;
                    pictureButton.setImageBitmap(pendingCommentPicture);
                    commentField.requestFocus();
                }
            };

            return (ActFmCameraModule.activityResult((Activity)getContext(),
                    requestCode, resultCode, data, callback));
        } else {
            return false;
        }
    }

}
