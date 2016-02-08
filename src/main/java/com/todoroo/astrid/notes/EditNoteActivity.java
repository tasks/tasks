/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.Fragment;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import timber.log.Timber;

import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

public class EditNoteActivity extends LinearLayout {

    private Task task;

    private final MetadataDao metadataDao;
    private final UserActivityDao userActivityDao;
    private final TaskService taskService;
    private final ArrayList<NoteOrUpdate> items = new ArrayList<>();
    private int commentItems = 10;
    private final Fragment fragment;

    private final TaskListActivity activity;

    public EditNoteActivity(
            MetadataDao metadataDao,
            UserActivityDao userActivityDao,
            TaskService taskService,
            Fragment fragment,
            long t) {
        super(fragment.getActivity());
        this.metadataDao = metadataDao;
        this.userActivityDao = userActivityDao;
        this.taskService = taskService;

        this.fragment = fragment;

        this.activity = (TaskListActivity) fragment.getActivity();

        setOrientation(VERTICAL);

        loadViewForTaskID(t);
    }

    private void fetchTask(long id) {
        task = taskService.fetchById(id, Task.NOTES, Task.ID, Task.UUID, Task.TITLE);
    }

    public void loadViewForTaskID(long t){
        try {
            fetchTask(t);
        } catch (SQLiteException e) {
            Timber.e(e, e.getMessage());
        }
        if(task == null) {
            return;
        }
        setUpInterface();
        reloadView();
    }

    // --- UI preparation

    private void setUpInterface() {
        if(!TextUtils.isEmpty(task.getNotes())) {
            TextView notes = new TextView(activity);
            notes.setLinkTextColor(Color.rgb(100, 160, 255));
            notes.setTextSize(18);
            notes.setText(task.getNotes());
            notes.setPadding(5, 10, 5, 10);
            Linkify.addLinks(notes, Linkify.ALL);
        }
    }

    public void reloadView() {
        items.clear();
        this.removeAllViews();
        metadataDao.byTaskAndKey(task.getId(), NoteMetadata.METADATA_KEY, new Callback<Metadata>() {
            @Override
            public void apply(Metadata metadata) {
                items.add(NoteOrUpdate.fromMetadata(metadata));
            }
        });

        userActivityDao.getCommentsForTask(task.getUuid(), new Callback<UserActivity>() {
            @Override
            public void apply(UserActivity update) {
                items.add(NoteOrUpdate.fromUpdate(update));
            }
        });

        Collections.sort(items, new Comparator<NoteOrUpdate>() {
            @Override
            public int compare(NoteOrUpdate a, NoteOrUpdate b) {
                if (a.createdAt < b.createdAt) {
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
            Button loadMore = new Button(activity);
            loadMore.setText(R.string.TEA_load_more);
            loadMore.setTextColor(activity.getResources().getColor(R.color.task_edit_deadline_gray));
            loadMore.setBackgroundColor(Color.alpha(0));
            loadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Perform action on click
                    commentItems += 10;
                    reloadView();
                }
            });
            this.addView(loadMore);
        }
    }

    public View getUpdateNotes(NoteOrUpdate note, ViewGroup parent) {
        View convertView = activity.getLayoutInflater().inflate(
                    R.layout.comment_adapter_row, parent, false);
        bindView(convertView, note);
        return convertView;
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void bindView(View view, NoteOrUpdate item) {
        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(item.title);
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
}
