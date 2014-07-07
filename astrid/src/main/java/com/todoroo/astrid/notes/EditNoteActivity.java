/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
import com.todoroo.astrid.actfm.ActFmCameraModule.ClearImageCallback;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.timers.TimerActionControlSet.TimerActionListener;

import org.json.JSONObject;
import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

public class EditNoteActivity extends LinearLayout implements TimerActionListener {

    private static final Property.StringProperty ACTIVITY_TYPE_PROPERTY = new Property.StringProperty(null, "'" + NameMaps.TABLE_ID_USER_ACTIVITY + "' as type");  //$NON-NLS-1$//$NON-NLS-2$

    private static final Property<?>[] USER_ACTIVITY_PROPERTIES = {
            UserActivity.CREATED_AT,
            UserActivity.ACTION,
            UserActivity.MESSAGE,
            UserActivity.TARGET_ID,
            UserActivity.PICTURE,
            UserActivity.ID,
            ACTIVITY_TYPE_PROPERTY,
    };

    private static final int TYPE_PROPERTY_INDEX = USER_ACTIVITY_PROPERTIES.length - 1;

    private Task task;

    private final Preferences preferences;
    private final MetadataService metadataService;
    private final UserActivityDao userActivityDao;
    private final TaskService taskService;
    private final ArrayList<NoteOrUpdate> items = new ArrayList<>();
    private EditText commentField;
    private final View commentsBar;
    private View timerView;
    private View commentButton;
    private int commentItems = 10;
    private ImageButton pictureButton;
    private Uri pendingCommentPicture = null;
    private final Fragment fragment;

    private final AstridActivity activity;

    private final int cameraButton;

    private final int color;

    private static boolean respondToPicture = false;

    private final List<UpdatesChangedListener> listeners = new LinkedList<>();

    public interface UpdatesChangedListener {
        public void updatesChanged();
        public void commentAdded();
    }

    public EditNoteActivity(
            Preferences preferences,
            MetadataService metadataService,
            UserActivityDao userActivityDao,
            TaskService taskService,
            Fragment fragment,
            View parent,
            long t) {
        super(fragment.getActivity());
        this.preferences = preferences;
        this.metadataService = metadataService;
        this.userActivityDao = userActivityDao;
        this.taskService = taskService;

        this.fragment = fragment;

        this.activity = (AstridActivity) fragment.getActivity();

        TypedValue tv = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.asTextColor, tv, false);
        color = tv.data;

        fragment.getActivity().getTheme().resolveAttribute(R.attr.asDueDateColor, tv, false);

        cameraButton = getDefaultCameraButton();

        setOrientation(VERTICAL);

        commentsBar = parent.findViewById(R.id.updatesFooter);

        loadViewForTaskID(t);
    }

    private int getDefaultCameraButton() {
        TypedValue typedValue = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.ic_action_camera, typedValue, true);
        return typedValue.resourceId;
    }

    private void fetchTask(long id) {
        task = taskService.fetchById(id, Task.NOTES, Task.ID, Task.UUID, Task.TITLE, Task.USER_ACTIVITIES_PUSHED_AT, Task.ATTACHMENTS_PUSHED_AT);
    }

    public void loadViewForTaskID(long t){
        try {
            fetchTask(t);
        } catch (SQLiteException e) {
            StartupService.handleSQLiteError(fragment.getActivity(), e);
        }
        if(task == null) {
            return;
        }
        setUpInterface();
        setUpListAdapter();
    }

    // --- UI preparation

    private void setUpInterface() {
        timerView = commentsBar.findViewById(R.id.timer_container);
        commentButton = commentsBar.findViewById(R.id.commentButton);
        commentField = (EditText) commentsBar.findViewById(R.id.commentField);

        final boolean showTimerShortcut = preferences.getBoolean(R.string.p_show_timer_shortcut, false);

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
                if (showTimerShortcut) {
                    timerView.setVisibility((s.length() > 0 || pendingCommentPicture != null) ? View.GONE
                            : View.VISIBLE);
                }
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
                if (pendingCommentPicture != null) {
                    ActFmCameraModule.showPictureLauncher(fragment, clearImage);
                } else {
                    ActFmCameraModule.showPictureLauncher(fragment, null);
                }
                respondToPicture = true;
            }
        });
        if(!TextUtils.isEmpty(task.getNotes())) {
            TextView notes = new TextView(getContext());
            notes.setLinkTextColor(Color.rgb(100, 160, 255));
            notes.setTextSize(18);
            notes.setText(task.getNotes());
            notes.setPadding(5, 10, 5, 10);
            Linkify.addLinks(notes, Linkify.ALL);
        }

        if (activity != null) {
            String uri = activity.getIntent().getStringExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS);
            if (uri != null) {
                pendingCommentPicture = Uri.parse(uri);
                setPictureButtonToPendingPicture();
            }
        }
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

        TodorooCursor<UserActivity> updates = getActivityForTask(task);
        try {
            UserActivity update = new UserActivity();
            for(updates.moveToFirst(); !updates.isAfterLast(); updates.moveToNext()) {
                update.clear();

                String type = updates.getString(TYPE_PROPERTY_INDEX);
                NoteOrUpdate noa = null;
                if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
                    readUserActivityProperties(updates, update);
                    noa = NoteOrUpdate.fromUpdate(update);
                }
                if(noa != null) {
                    items.add(noa);
                }
            }
        } finally {
            updates.close();
        }

        Collections.sort(items, new Comparator<NoteOrUpdate>() {
            @Override
            public int compare(NoteOrUpdate a, NoteOrUpdate b) {
                if(a.createdAt < b.createdAt) {
                    return 1;
                } else if (a.createdAt == b.createdAt) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        for (int i = 0; i < Math.min(items.size(), commentItems); i++) {
            View notesView = this.getUpdateNotes(items.get(i), this);
            this.addView(notesView);
        }

        if (items.size() > commentItems) {
            Button loadMore = new Button(getContext());
            loadMore.setText(R.string.TEA_load_more);
            loadMore.setTextColor(activity.getResources().getColor(R.color.task_edit_deadline_gray));
            loadMore.setBackgroundColor(Color.alpha(0));
            loadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Perform action on click
                    commentItems += 10;
                    setUpListAdapter();
                }
            });
            this.addView(loadMore);
        }

        for (UpdatesChangedListener l : listeners) {
            l.updatesChanged();
        }
    }

    public TodorooCursor<UserActivity> getActivityForTask(Task task) {
        Query taskQuery = Query.select(AndroidUtilities.addToArray(Property.class, USER_ACTIVITY_PROPERTIES))
                .where(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT), UserActivity.TARGET_ID.eq(task.getUuid()), UserActivity.DELETED_AT.eq(0)));

        Query resultQuery = taskQuery.orderBy(Order.desc("1")); //$NON-NLS-1$

        return userActivityDao.query(resultQuery);
    }

    private static void readUserActivityProperties(TodorooCursor<UserActivity> unionCursor, UserActivity activity) {
        activity.setCreatedAt(unionCursor.getLong(0));
        activity.setAction(unionCursor.getString(1));
        activity.setMessage(unionCursor.getString(2));
        activity.setTargetId(unionCursor.getString(3));
        activity.setPicture(unionCursor.getString(4));
    }

    public View getUpdateNotes(NoteOrUpdate note, ViewGroup parent) {
        View convertView = ((Activity)getContext()).getLayoutInflater().inflate(
                    R.layout.update_adapter_row, parent, false);
        bindView(convertView, note);
        return convertView;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void bindView(View view, NoteOrUpdate item) {
        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(item.title);
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
        final ImageView commentPictureView = (ImageView)view.findViewById(R.id.comment_picture);
        setupImagePopupForCommentView(view, commentPictureView, item.commentBitmap, fragment);
    }

    private static void setupImagePopupForCommentView(View view, ImageView commentPictureView, final Uri updateBitmap,
                                                     final Fragment fragment) {
        if (updateBitmap != null) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            String path = getPathFromUri(fragment.getActivity(), updateBitmap);
            commentPictureView.setImageBitmap(sampleBitmap(path, commentPictureView.getLayoutParams().width, commentPictureView.getLayoutParams().height));

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.startActivity(new Intent(Intent.ACTION_VIEW) {{
                        setDataAndType(updateBitmap, "image/*");
                    }});
                }
            });
        } else {
            commentPictureView.setVisibility(View.GONE);
        }
    }

    private void addComment() {
        addComment(commentField.getText().toString(), UserActivity.ACTION_TASK_COMMENT, task.getUuid(), task.getTitle(), true);
    }

    private void addComment(String message, String actionCode, String uuid, String title, boolean usePicture) {
        // Allow for users to just add picture
        if (TextUtils.isEmpty(message) && usePicture) {
            message = " ";
        }
        UserActivity userActivity = new UserActivity();
        userActivity.setMessage(message);
        userActivity.setAction(actionCode);
        userActivity.setTargetId(uuid);
        userActivity.setCreatedAt(DateUtilities.now());
        if (usePicture && pendingCommentPicture != null) {
            JSONObject pictureJson = RemoteModel.PictureHelper.savePictureJson(pendingCommentPicture);
            if (pictureJson != null) {
                userActivity.setPicture(pictureJson.toString());
            }
        }

        userActivityDao.createNew(userActivity);
        if (commentField != null) {
            commentField.setText(""); //$NON-NLS-1$
        }

        pendingCommentPicture = usePicture ? null : pendingCommentPicture;
        if (usePicture) {
            if (activity != null) {
                activity.getIntent().removeExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS);
            }
        }
        if (pictureButton != null) {
            pictureButton.setImageResource(cameraButton);
        }

        setUpListAdapter();
        for (UpdatesChangedListener l : listeners) {
            l.commentAdded();
        }
    }

    private static class NoteOrUpdate {
        private final Spanned title;
        private final Uri commentBitmap;
        private final long createdAt;

        public NoteOrUpdate(Spanned title, Uri commentBitmap, long createdAt) {
            super();
            this.title = title;
            this.commentBitmap = commentBitmap;
            this.createdAt = createdAt;
        }

        public static NoteOrUpdate fromMetadata(Metadata m) {
            if(!m.containsNonNullValue(NoteMetadata.THUMBNAIL)) {
                m.setValue(NoteMetadata.THUMBNAIL, ""); //$NON-NLS-1$
            }
            if(!m.containsNonNullValue(NoteMetadata.COMMENT_PICTURE)) {
                m.setValue(NoteMetadata.COMMENT_PICTURE, ""); //$NON-NLS-1$
            }
            Spanned title = Html.fromHtml(String.format("%s\n%s", m.getValue(NoteMetadata.TITLE), m.getValue(NoteMetadata.BODY))); //$NON-NLS-1$
            return new NoteOrUpdate(title,
                    null,
                    m.getCreationDate());
        }

        public static NoteOrUpdate fromUpdate(UserActivity u) {
            if(u == null) {
                throw new RuntimeException("UserActivity should never be null");
            }

            Uri commentBitmap = u.getPictureUri();
            Spanned title = getUpdateComment(u);
            long createdAt = u.getCreatedAt();

            return new NoteOrUpdate(
                    title,
                    commentBitmap,
                    createdAt);
        }

        private static Spanned getUpdateComment(UserActivity activity) {
            String message = activity.getMessage();
            return Html.fromHtml(message);
        }
    }

    public void addListener(UpdatesChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void timerStarted(Task t) {
        addComment(String.format("%s %s",  //$NON-NLS-1$
                getContext().getString(R.string.TEA_timer_comment_started),
                DateUtilities.getTimeString(getContext(), newDate())),
                UserActivity.ACTION_TASK_COMMENT,
                t.getUuid(),
                t.getTitle(),
                false);
    }

    @Override
    public void timerStopped(Task t) {
        String elapsedTime = DateUtils.formatElapsedTime(t.getElapsedSeconds());
        addComment(String.format("%s %s\n%s %s", //$NON-NLS-1$
                getContext().getString(R.string.TEA_timer_comment_stopped),
                DateUtilities.getTimeString(getContext(), newDate()),
                getContext().getString(R.string.TEA_timer_comment_spent),
                elapsedTime), UserActivity.ACTION_TASK_COMMENT,
                t.getUuid(),
                t.getTitle(),
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
                public void handleCameraResult(Uri uri) {
                    if (activity != null) {
                        activity.getIntent().putExtra(TaskEditFragment.TOKEN_PICTURE_IN_PROGRESS, uri.toString());
                    }
                    pendingCommentPicture = uri;
                    setPictureButtonToPendingPicture();
                    commentField.requestFocus();
                }
            };

            return (ActFmCameraModule.activityResult((Activity)getContext(),
                    requestCode, resultCode, data, callback));
        } else {
            return false;
        }
    }

    private void setPictureButtonToPendingPicture() {
        String path = getPathFromUri(activity, pendingCommentPicture);
        pictureButton.setImageBitmap(sampleBitmap(path, pictureButton.getWidth(), pictureButton.getHeight()));
    }
}
