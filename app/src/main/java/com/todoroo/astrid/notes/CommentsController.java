/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.utility.Constants;

import org.tasks.R;
import org.tasks.files.FileHelper;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import static android.support.v4.content.ContextCompat.getColor;
import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

public class CommentsController {

    private final MetadataDao metadataDao;
    private final UserActivityDao userActivityDao;
    private final ArrayList<NoteOrUpdate> items = new ArrayList<>();
    private final Activity activity;
    private final Preferences preferences;

    private int commentItems = 10;
    private Task task;
    private ViewGroup commentsContainer;

    @Inject
    public CommentsController(MetadataDao metadataDao, UserActivityDao userActivityDao,
                              Activity activity, Preferences preferences) {
        this.metadataDao = metadataDao;
        this.userActivityDao = userActivityDao;
        this.activity = activity;
        this.preferences = preferences;
    }

    public void initialize(Task task, ViewGroup commentsContainer) {
        this.task = task;
        this.commentsContainer = commentsContainer;
    }

    public void reloadView() {
        if (!preferences.getBoolean(R.string.p_show_task_edit_comments, true)) {
            return;
        }

        items.clear();
        commentsContainer.removeAllViews();
        metadataDao.byTaskAndKey(task.getId(), NoteMetadata.METADATA_KEY, metadata -> items.add(NoteOrUpdate.fromMetadata(metadata)));

        userActivityDao.getCommentsForTask(task.getUuid(), update -> items.add(NoteOrUpdate.fromUpdate(update)));

        Collections.sort(items, (a, b) -> {
            if (a.createdAt < b.createdAt) {
                return 1;
            } else if (a.createdAt == b.createdAt) {
                return 0;
            } else {
                return -1;
            }
        });

        for (int i = 0; i < Math.min(items.size(), commentItems); i++) {
            View notesView = this.getUpdateNotes(items.get(i), commentsContainer);
            commentsContainer.addView(notesView);
        }

        if (items.size() > commentItems) {
            Button loadMore = new Button(activity);
            loadMore.setText(R.string.TEA_load_more);
            loadMore.setTextColor(getColor(activity, R.color.text_secondary));
            loadMore.setBackgroundColor(Color.alpha(0));
            loadMore.setOnClickListener(v -> {
                // Perform action on click
                commentItems += 10;
                reloadView();
            });
            commentsContainer.addView(loadMore);
        }
    }

    private View getUpdateNotes(NoteOrUpdate note, ViewGroup parent) {
        View convertView = activity.getLayoutInflater().inflate(R.layout.comment_adapter_row, parent, false);
        bindView(convertView, note);
        return convertView;
    }

    /** Helper method to set the contents and visibility of each field */
    private void bindView(View view, NoteOrUpdate item) {
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
        setupImagePopupForCommentView(view, commentPictureView, item.commentBitmap, activity);
    }

    private static void setupImagePopupForCommentView(View view, ImageView commentPictureView, final Uri updateBitmap,
                                                     final Activity activity) {
        if (updateBitmap != null) {
            commentPictureView.setVisibility(View.VISIBLE);
            String path = getPathFromUri(activity, updateBitmap);
            commentPictureView.setImageBitmap(sampleBitmap(path, commentPictureView.getLayoutParams().width, commentPictureView.getLayoutParams().height));

            view.setOnClickListener(v -> {
                File file = new File(updateBitmap.getPath());
                Uri uri = FileProvider.getUriForFile(activity, Constants.FILE_PROVIDER_AUTHORITY, file.getAbsoluteFile());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "image/*");
                FileHelper.grantReadPermissions(activity, intent, uri);
                activity.startActivity(intent);
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
