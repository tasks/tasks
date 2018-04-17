/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.google.common.base.Joiner;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService;
import dagger.Lazy;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.injection.ContentProviderComponent;
import org.tasks.injection.InjectingContentProvider;
import org.tasks.ui.CheckBoxes;
import timber.log.Timber;

/**
 * This is the legacy Astrid task provider. While it will continue to be supported, note that it
 * does not expose all of the information in Astrid, nor does it support many editing operations.
 *
 * <p>See the individual methods for a description of what is returned.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class Astrid2TaskProvider extends InjectingContentProvider {

  private static final String AUTHORITY = "org.tasks.tasksprovider";

  private static final Uri CONTENT_URI = Uri.parse("content://org.tasks.tasksprovider");

  private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

  private static final String NAME = "name";
  private static final String IMPORTANCE_COLOR = "importance_color";
  private static final String IDENTIFIER = "identifier";
  private static final String PREFERRED_DUE_DATE = "preferredDueDate";
  private static final String DEFINITE_DUE_DATE = "definiteDueDate";
  private static final String IMPORTANCE = "importance";
  private static final String ID = "id";

  private static final String TAGS_ID = "tags_id";

  private static final String[] TASK_FIELD_LIST =
      new String[] {
        NAME,
        IMPORTANCE_COLOR,
        PREFERRED_DUE_DATE,
        DEFINITE_DUE_DATE,
        IMPORTANCE,
        IDENTIFIER,
        TAGS_ID
      };

  private static final String[] TAGS_FIELD_LIST = new String[] {ID, NAME};

  private static final int URI_TASKS = 0;
  private static final int URI_TAGS = 1;

  private static final String TAG_SEPARATOR = "|";

  static {
    URI_MATCHER.addURI(AUTHORITY, "tasks", URI_TASKS);
    URI_MATCHER.addURI(AUTHORITY, "tags", URI_TAGS);
  }

  @Inject Lazy<TagService> tagService;
  @Inject Lazy<CheckBoxes> checkBoxes;
  @Inject Lazy<TaskDao> taskDao;
  @Inject Lazy<TagDao> tagDao;

  public static void notifyDatabaseModification(Context context) {
    try {
      context.getContentResolver().notifyChange(CONTENT_URI, null);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public String getType(@NonNull Uri uri) {
    return null;
  }

  @Override
  public Uri insert(@NonNull Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public boolean onCreate() {
    super.onCreate();
    return false;
  }

  @Override
  protected void inject(ContentProviderComponent component) {
    component.inject(this);
  }

  /**
   * Note: tag id is no longer a real column, so we pass in a UID generated from the tag string.
   *
   * @return two-column cursor: tag id (string) and tag name
   */
  private Cursor getTags() {

    TagData[] tags = tagService.get().getGroupedTags();

    MatrixCursor ret = new MatrixCursor(TAGS_FIELD_LIST);

    for (TagData tag : tags) {
      Object[] values = new Object[2];
      values[0] = tagNameToLong(tag.getName());
      values[1] = tag.getName();

      ret.addRow(values);
    }

    return ret;
  }

  private long tagNameToLong(String tag) {
    MessageDigest m;
    try {
      m = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      Timber.e(e);
      return -1;
    }

    m.update(tag.getBytes(), 0, tag.length());
    return new BigInteger(1, m.digest()).longValue();
  }

  /**
   * Cursor with the following columns
   *
   * <ol>
   *   <li>task title, string
   *   <li>task importance color, int android RGB color
   *   <li>task due date (was: preferred due date), long millis since epoch
   *   <li>task due date (was: absolute due date), long millis since epoch
   *   <li>task importance, integer from 0 to 3 (0 => most important)
   *   <li>task id, long
   *   <li>task tags, string tags separated by |
   * </ol>
   *
   * @return cursor as described above
   */
  private Cursor getTasks() {
    MatrixCursor ret = new MatrixCursor(TASK_FIELD_LIST);
    List<Integer> importanceColors = checkBoxes.get().getPriorityColors();
    List<Task> tasks = taskDao.get().getAstrid2TaskProviderTasks();
    for (Task task : tasks) {
      String taskTags = getTagsAsString(task.getId(), TAG_SEPARATOR);

      Object[] values = new Object[7];
      values[0] = task.getTitle();
      values[1] = importanceColors.get(task.getPriority());
      values[2] = task.getDueDate();
      values[3] = task.getDueDate();
      values[4] = task.getPriority();
      values[5] = task.getId();
      values[6] = taskTags;

      ret.addRow(values);
    }
    return ret;
  }

  @Override
  public Cursor query(
      @NonNull Uri uri,
      String[] projection,
      String selection,
      String[] selectionArgs,
      String sortOrder) {
    switch (URI_MATCHER.match(uri)) {
      case URI_TASKS:
        return getTasks();
      case URI_TAGS:
        return getTags();
      default:
        throw new IllegalStateException("Unrecognized URI:" + uri);
    }
  }

  @Override
  public int update(
      @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException("not supported");
  }

  /**
   * Return tags as a list of strings separated by given separator
   *
   * @return empty string if no tags, otherwise string
   */
  private String getTagsAsString(long taskId, String separator) {
    return Joiner.on(separator).join(tagDao.get().getTagNames(taskId));
  }
}
