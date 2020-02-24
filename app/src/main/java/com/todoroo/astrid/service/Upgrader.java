package com.todoroo.astrid.service;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.caldav.CaldavUtils.getParent;
import static org.tasks.db.DbUtils.batch;

import android.content.Context;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import java.io.File;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.caldav.CaldavUtils;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.CaldavTaskContainer;
import org.tasks.data.Filter;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeColor;

public class Upgrader {

  private static final int V4_8_0 = 380;
  private static final int V4_9_5 = 434;
  private static final int V5_3_0 = 491;
  private static final int V6_0_beta_1 = 522;
  private static final int V6_0_beta_2 = 523;
  private static final int V6_4 = 546;
  private static final int V6_7 = 585;
  private static final int V6_8_1 = 607;
  private static final int V6_9 = 608;
  private static final int V7_0 = 617;
  private static final int V8_2 = 675;
  private final Context context;
  private final Preferences preferences;
  private final Tracker tracker;
  private final TagDataDao tagDataDao;
  private final TagDao tagDao;
  private final FilterDao filterDao;
  private final DefaultFilterProvider defaultFilterProvider;
  private final GoogleTaskListDao googleTaskListDao;
  private final UserActivityDao userActivityDao;
  private final TaskAttachmentDao taskAttachmentDao;
  private final CaldavDao caldavDao;
  private final TaskDao taskDao;

  @Inject
  public Upgrader(
      @ForApplication Context context,
      Preferences preferences,
      Tracker tracker,
      TagDataDao tagDataDao,
      TagDao tagDao,
      FilterDao filterDao,
      DefaultFilterProvider defaultFilterProvider,
      GoogleTaskListDao googleTaskListDao,
      UserActivityDao userActivityDao,
      TaskAttachmentDao taskAttachmentDao,
      CaldavDao caldavDao,
      TaskDao taskDao) {
    this.context = context;
    this.preferences = preferences;
    this.tracker = tracker;
    this.tagDataDao = tagDataDao;
    this.tagDao = tagDao;
    this.filterDao = filterDao;
    this.defaultFilterProvider = defaultFilterProvider;
    this.googleTaskListDao = googleTaskListDao;
    this.userActivityDao = userActivityDao;
    this.taskAttachmentDao = taskAttachmentDao;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
  }

  public void upgrade(int from, int to) {
    if (from > 0) {
      run(from, V4_8_0, this::performMarshmallowMigration);
      run(from, V4_9_5, this::removeDuplicateTags);
      run(from, V5_3_0, this::migrateFilters);
      run(from, V6_0_beta_1, this::migrateDefaultSyncList);
      run(from, V6_0_beta_2, this::migrateGoogleTaskAccount);
      run(from, V6_4, this::migrateUris);
      run(from, V6_7, this::migrateGoogleTaskFilters);
      run(from, V6_8_1, this::migrateCaldavFilters);
      run(from, V6_9, this::applyCaldavCategories);
      run(from, V7_0, this::applyCaldavSubtasks);
      run(from, V8_2, this::migrateColors);
    }
    preferences.setCurrentVersion(to);
  }

  private void run(int from, int version, Runnable runnable) {
    if (from < version) {
      runnable.run();
      preferences.setCurrentVersion(version);
    }
  }

  private void migrateColors() {
    preferences.setInt(
        R.string.p_theme_color, getAndroidColor(preferences.getInt(R.string.p_theme_color, 7)));
    for (CaldavCalendar calendar : caldavDao.getCalendars()) {
      calendar.setColor(getAndroidColor(calendar.getColor()));
      caldavDao.update(calendar);
    }
    for (GoogleTaskList list : googleTaskListDao.getAllLists()) {
      list.setColor(getAndroidColor(list.getColor()));
      googleTaskListDao.update(list);
    }
    for (TagData tagData : tagDataDao.getAll()) {
      tagData.setColor(getAndroidColor(tagData.getColor()));
      tagDataDao.update(tagData);
    }
    for (Filter filter : filterDao.getFilters()) {
      filter.setColor(getAndroidColor(filter.getColor()));
      filterDao.update(filter);
    }
  }

  private int getAndroidColor(int index) {
    return index >= 0 && index < ThemeColor.COLORS.length
        ? ContextCompat.getColor(context, ThemeColor.COLORS[index])
        : 0;
  }

  private void applyCaldavSubtasks() {
    List<CaldavTask> updated = newArrayList();

    for (CaldavTask task : transform(caldavDao.getTasks(), CaldavTaskContainer::getCaldavTask)) {
      at.bitfire.ical4android.Task remoteTask = CaldavUtils.fromVtodo(task.getVtodo());
      if (remoteTask == null) {
        continue;
      }
      task.setRemoteParent(getParent(remoteTask));
      if (!Strings.isNullOrEmpty(task.getRemoteParent())) {
        updated.add(task);
      }
    }

    caldavDao.update(updated);
    caldavDao.updateParents();
  }

  private void applyCaldavCategories() {
    List<Long> tasksWithTags = caldavDao.getTasksWithTags();
    for (CaldavTaskContainer container : caldavDao.getTasks()) {
      at.bitfire.ical4android.Task remoteTask =
          CaldavUtils.fromVtodo(container.caldavTask.getVtodo());
      if (remoteTask != null) {
        tagDao.insert(container.task, CaldavUtils.getTags(tagDataDao, remoteTask.getCategories()));
      }
    }
    batch(tasksWithTags, taskDao::touch);
  }

  private void performMarshmallowMigration() {
    try {
      // preserve pre-marshmallow default backup location
      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        if (!preferences.isStringValueSet(R.string.p_backup_dir)) {
          String directory = String.format("%s/astrid", Environment.getExternalStorageDirectory());
          File file = new File(directory);
          if (file.exists() && file.isDirectory()) {
            preferences.setString(R.string.p_backup_dir, directory);
          }
        }
      }
    } catch (Exception e) {
      tracker.reportException(e);
    }
  }

  private void removeDuplicateTags() {
    ListMultimap<String, TagData> tagsByUuid =
        Multimaps.index(tagDataDao.tagDataOrderedByName(), TagData::getRemoteId);
    for (String uuid : tagsByUuid.keySet()) {
      removeDuplicateTagData(tagsByUuid.get(uuid));
      removeDuplicateTagMetadata(uuid);
    }
  }

  private void migrateGoogleTaskFilters() {
    for (Filter filter : filterDao.getAll()) {
      filter.setSql(migrateGoogleTaskFilters(filter.getSql()));
      filter.setCriterion(migrateGoogleTaskFilters(filter.getCriterion()));
      filterDao.update(filter);
    }
  }

  private void migrateCaldavFilters() {
    for (Filter filter : filterDao.getAll()) {
      filter.setSql(migrateCaldavFilters(filter.getSql()));
      filter.setCriterion(migrateCaldavFilters(filter.getCriterion()));
      filterDao.update(filter);
    }
  }

  private void migrateFilters() {
    for (Filter filter : filterDao.getFilters()) {
      filter.setSql(migrateMetadata(filter.getSql()));
      filter.setCriterion(migrateMetadata(filter.getCriterion()));
      filterDao.update(filter);
    }
  }

  private void migrateDefaultSyncList() {
    String account = preferences.getStringValue("gtasks_user");
    if (isNullOrEmpty(account)) {
      return;
    }

    String defaultGoogleTaskList = preferences.getStringValue("gtasks_defaultlist");
    if (isNullOrEmpty(defaultGoogleTaskList)) {
      // TODO: look up default list
    } else {
      GoogleTaskList googleTaskList = googleTaskListDao.getByRemoteId(defaultGoogleTaskList);
      if (googleTaskList != null) {
        defaultFilterProvider.setDefaultRemoteList(new GtasksFilter(googleTaskList));
      }
    }
  }

  private void migrateGoogleTaskAccount() {
    String account = preferences.getStringValue("gtasks_user");
    if (!isNullOrEmpty(account)) {
      GoogleTaskAccount googleTaskAccount = new GoogleTaskAccount();
      googleTaskAccount.setAccount(account);
      googleTaskListDao.insert(googleTaskAccount);
      for (GoogleTaskList list : googleTaskListDao.getAllLists()) {
        list.setAccount(account);
        googleTaskListDao.insertOrReplace(list);
      }
    }
  }

  private void migrateUris() {
    migrateUriPreference(R.string.p_backup_dir);
    migrateUriPreference(R.string.p_attachment_dir);
    for (UserActivity userActivity : userActivityDao.getComments()) {
      userActivity.convertPictureUri();
      userActivityDao.update(userActivity);
    }
    for (TaskAttachment attachment : taskAttachmentDao.getAttachments()) {
      attachment.convertPathUri();
      taskAttachmentDao.update(attachment);
    }
  }

  private void migrateUriPreference(int pref) {
    String path = preferences.getStringValue(pref);
    if (Strings.isNullOrEmpty(path)) {
      return;
    }
    File file = new File(path);
    try {
      if (file.canWrite()) {
        preferences.setUri(pref, file.toURI());
      } else {
        preferences.remove(pref);
      }
    } catch (SecurityException ignored) {
      preferences.remove(pref);
    }
  }

  private String migrateGoogleTaskFilters(String input) {
    return input
        .replace("SELECT task FROM google_tasks", "SELECT gt_task as task FROM google_tasks")
        .replace("(list_id", "(gt_list_id")
        .replace("google_tasks.list_id", "google_tasks.gt_list_id")
        .replace("google_tasks.task", "google_tasks.gt_task");
  }

  private String migrateCaldavFilters(String input) {
    return input
        .replace("SELECT task FROM caldav_tasks", "SELECT cd_task as task FROM caldav_tasks")
        .replace("(calendar", "(cd_calendar");
  }

  private String migrateMetadata(String input) {
    return input
        .replaceAll(
            "SELECT metadata\\.task AS task FROM metadata INNER JOIN tasks ON \\(\\(metadata\\.task=tasks\\._id\\)\\) WHERE \\(\\(\\(tasks\\.completed=0\\) AND \\(tasks\\.deleted=0\\) AND \\(tasks\\.hideUntil<\\(strftime\\(\\'%s\\',\\'now\\'\\)\\*1000\\)\\)\\) AND \\(metadata\\.key=\\'tags-tag\\'\\) AND \\(metadata\\.value",
            "SELECT tags.task AS task FROM tags INNER JOIN tasks ON ((tags.task=tasks._id)) WHERE (((tasks.completed=0) AND (tasks.deleted=0) AND (tasks.hideUntil<(strftime('%s','now')*1000))) AND (tags.name")
        .replaceAll(
            "SELECT metadata\\.task AS task FROM metadata INNER JOIN tasks ON \\(\\(metadata\\.task=tasks\\._id\\)\\) WHERE \\(\\(\\(tasks\\.completed=0\\) AND \\(tasks\\.deleted=0\\) AND \\(tasks\\.hideUntil<\\(strftime\\(\\'%s\\',\\'now\\'\\)\\*1000\\)\\)\\) AND \\(metadata\\.key=\\'gtasks\\'\\) AND \\(metadata\\.value2",
            "SELECT google_tasks.task AS task FROM google_tasks INNER JOIN tasks ON ((google_tasks.task=tasks._id)) WHERE (((tasks.completed=0) AND (tasks.deleted=0) AND (tasks.hideUntil<(strftime('%s','now')*1000))) AND (google_tasks.list_id")
        .replaceAll("AND \\(metadata\\.deleted=0\\)", "");
  }

  private void removeDuplicateTagData(List<TagData> tagData) {
    if (tagData.size() > 1) {
      tagDataDao.delete(tagData.subList(1, tagData.size()));
    }
  }

  private void removeDuplicateTagMetadata(String uuid) {
    List<Tag> metadatas = tagDao.getByTagUid(uuid);
    ImmutableListMultimap<Long, Tag> metadataByTask = Multimaps.index(metadatas, Tag::getTask);
    for (Long key : metadataByTask.keySet()) {
      ImmutableList<Tag> tags = metadataByTask.get(key);
      if (tags.size() > 1) {
        tagDao.delete(tags.subList(1, tags.size()));
      }
    }
  }
}
