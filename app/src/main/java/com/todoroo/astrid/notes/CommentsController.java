/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import static android.support.v4.content.ContextCompat.getColor;
import static org.tasks.files.FileHelper.getPathFromUri;
import static org.tasks.files.ImageHelper.sampleBitmap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Constants;
import java.io.File;
import java.util.ArrayList;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.files.FileHelper;
import org.tasks.preferences.Preferences;

public class CommentsController {

  private final UserActivityDao userActivityDao;
  private final ArrayList<UserActivity> items = new ArrayList<>();
  private final Activity activity;
  private final Preferences preferences;

  private int commentItems = 10;
  private Task task;
  private ViewGroup commentsContainer;

  @Inject
  public CommentsController(
      UserActivityDao userActivityDao, Activity activity, Preferences preferences) {
    this.userActivityDao = userActivityDao;
    this.activity = activity;
    this.preferences = preferences;
  }

  private static void setupImagePopupForCommentView(
      View view, ImageView commentPictureView, final Uri updateBitmap, final Activity activity) {
    if (updateBitmap != null) {
      commentPictureView.setVisibility(View.VISIBLE);
      String path = getPathFromUri(activity, updateBitmap);
      commentPictureView.setImageBitmap(
          sampleBitmap(
              path,
              commentPictureView.getLayoutParams().width,
              commentPictureView.getLayoutParams().height));

      view.setOnClickListener(
          v -> {
            File file = new File(updateBitmap.getPath());
            Uri uri =
                FileProvider.getUriForFile(
                    activity, Constants.FILE_PROVIDER_AUTHORITY, file.getAbsoluteFile());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            FileHelper.grantReadPermissions(activity, intent, uri);
            activity.startActivity(intent);
          });
    } else {
      commentPictureView.setVisibility(View.GONE);
    }
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

    items.addAll(userActivityDao.getCommentsForTask(task.getUuid()));

    for (int i = 0; i < Math.min(items.size(), commentItems); i++) {
      View notesView = this.getUpdateNotes(items.get(i), commentsContainer);
      commentsContainer.addView(notesView);
    }

    if (items.size() > commentItems) {
      Button loadMore = new Button(activity);
      loadMore.setText(R.string.TEA_load_more);
      loadMore.setTextColor(getColor(activity, R.color.text_secondary));
      loadMore.setBackgroundColor(Color.alpha(0));
      loadMore.setOnClickListener(
          v -> {
            // Perform action on click
            commentItems += 10;
            reloadView();
          });
      commentsContainer.addView(loadMore);
    }
  }

  private View getUpdateNotes(UserActivity userActivity, ViewGroup parent) {
    View convertView =
        activity.getLayoutInflater().inflate(R.layout.comment_adapter_row, parent, false);
    bindView(convertView, userActivity);
    return convertView;
  }

  /** Helper method to set the contents and visibility of each field */
  private void bindView(View view, UserActivity item) {
    // name
    final TextView nameView = view.findViewById(R.id.title);
    nameView.setText(Html.fromHtml(item.getMessage()));
    Linkify.addLinks(nameView, Linkify.ALL);

    // date
    final TextView date = view.findViewById(R.id.date);
    date.setText(DateUtilities.getLongDateStringWithTime(activity, item.getCreated()));

    // picture
    final ImageView commentPictureView = view.findViewById(R.id.comment_picture);
    setupImagePopupForCommentView(view, commentPictureView, item.getPictureUri(), activity);
  }
}
