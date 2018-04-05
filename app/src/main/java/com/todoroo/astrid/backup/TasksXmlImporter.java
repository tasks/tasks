/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.io.FileReader;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.backup.XmlReader;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.dialogs.DialogBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import timber.log.Timber;

public class TasksXmlImporter {

  private static final String FORMAT2 = "2"; // $NON-NLS-1$
  private static final String FORMAT3 = "3"; // $NON-NLS-1$
  private final TagDataDao tagDataDao;
  private final UserActivityDao userActivityDao;
  private final DialogBuilder dialogBuilder;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final AlarmDao alarmDao;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final LocationDao locationDao;
  private Activity activity;
  private Handler handler;
  private int taskCount;
  private int importCount = 0;
  private int skipCount = 0;
  private int errorCount = 0;
  private ProgressDialog progressDialog;
  private String input;

  @Inject
  public TasksXmlImporter(
      TagDataDao tagDataDao,
      UserActivityDao userActivityDao,
      DialogBuilder dialogBuilder,
      TaskDao taskDao,
      LocationDao locationDao,
      LocalBroadcastManager localBroadcastManager,
      AlarmDao alarmDao,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao) {
    this.tagDataDao = tagDataDao;
    this.userActivityDao = userActivityDao;
    this.dialogBuilder = dialogBuilder;
    this.taskDao = taskDao;
    this.locationDao = locationDao;
    this.localBroadcastManager = localBroadcastManager;
    this.alarmDao = alarmDao;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
  }

  private void setProgressMessage(final String message) {
    handler.post(() -> progressDialog.setMessage(message));
  }

  public void importTasks(Activity activity, String input, ProgressDialog progressDialog) {
    this.activity = activity;
    this.input = input;
    this.progressDialog = progressDialog;

    handler = new Handler();

    new Thread(
            () -> {
              try {
                performImport();
              } catch (IOException | XmlPullParserException e) {
                Timber.e(e);
              }
            })
        .start();
  }

  // --- importers

  // =============================================================== FORMAT2

  private void performImport() throws IOException, XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    XmlPullParser xpp = factory.newPullParser();
    xpp.setInput(new FileReader(input));

    try {
      while (xpp.next() != XmlPullParser.END_DOCUMENT) {
        String tag = xpp.getName();
        if (xpp.getEventType() == XmlPullParser.END_TAG) {
          // Ignore end tags
          continue;
        }
        if (tag != null) {
          // Process <astrid ... >
          if (tag.equals(BackupConstants.ASTRID_TAG)) {
            String format = xpp.getAttributeValue(null, BackupConstants.ASTRID_ATTR_FORMAT);
            if (TextUtils.equals(format, FORMAT2)) {
              new Format2TaskImporter(xpp);
            } else if (TextUtils.equals(format, FORMAT3)) {
              new Format3TaskImporter(xpp);
            } else {
              throw new UnsupportedOperationException(
                  "Did not know how to import tasks with xml format '" + format + "'");
            }
          }
        }
      }
    } finally {
      localBroadcastManager.broadcastRefresh();
      handler.post(
          () -> {
            if (progressDialog.isShowing()) {
              DialogUtilities.dismissDialog(activity, progressDialog);
              showSummary();
            }
          });
    }
  }

  private void showSummary() {
    Resources r = activity.getResources();
    dialogBuilder
        .newDialog()
        .setTitle(R.string.import_summary_title)
        .setMessage(
            activity.getString(
                R.string.import_summary_message,
                input,
                r.getQuantityString(R.plurals.Ntasks, taskCount, taskCount),
                r.getQuantityString(R.plurals.Ntasks, importCount, importCount),
                r.getQuantityString(R.plurals.Ntasks, skipCount, skipCount),
                r.getQuantityString(R.plurals.Ntasks, errorCount, errorCount)))
        .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss())
        .show();
  }

  // =============================================================== FORMAT3

  private class Format2TaskImporter {

    XmlPullParser xpp;
    Task currentTask;

    Format2TaskImporter() {}

    Format2TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
      this.xpp = xpp;

      while (xpp.next() != XmlPullParser.END_DOCUMENT) {
        String tag = xpp.getName();
        if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG) {
          continue;
        }

        try {
          if (tag.equals(BackupConstants.TASK_TAG)) {
            // Parse <task ... >
            parseTask();
          } else if (tag.equals(BackupConstants.COMMENT_TAG)) {
            // Process <comment ... >
            parseComment();
          } else if (tag.equals(BackupConstants.METADATA_TAG)) {
            // Process <metadata ... >
            parseMetadata(2);
          }
        } catch (Exception e) {
          errorCount++;
          Timber.e(e);
        }
      }
    }

    void parseTask() {
      taskCount++;
      setProgressMessage(activity.getString(R.string.import_progress_read, taskCount));

      currentTask = new Task(new XmlReader(xpp));

      Task existingTask = taskDao.fetch(currentTask.getUuid());

      if (existingTask == null) {
        taskDao.createNew(currentTask);
        importCount++;
      } else {
        skipCount++;
      }
    }

    /** Imports a comment from the XML we're reading. taken from EditNoteActivity.addComment() */
    void parseComment() {
      if (!currentTask.isSaved()) {
        return;
      }

      UserActivity userActivity = new UserActivity(new XmlReader(xpp));
      userActivityDao.createNew(userActivity);
    }

    void parseMetadata(int format) {
      if (!currentTask.isSaved()) {
        return;
      }
      XmlReader xml = new XmlReader(xpp);
      String key = xml.readString("key");
      if ("alarm".equals(key)) {
        Alarm alarm = new Alarm();
        alarm.setTask(currentTask.getId());
        alarm.setTime(xml.readLong("value"));
        alarmDao.insert(alarm);
      } else if ("geofence".equals(key)) {
        Location location = new Location();
        location.setTask(currentTask.getId());
        location.setName(xml.readString("value"));
        location.setLatitude(xml.readDouble("value2"));
        location.setLongitude(xml.readDouble("value3"));
        location.setRadius(xml.readInteger("value4"));
        locationDao.insert(location);
      } else if ("tags-tag".equals(key)) {
        String name = xml.readString("value");
        String tagUid = xml.readString("value2");
        if (tagDao.getTagByTaskAndTagUid(currentTask.getId(), tagUid) == null) {
          tagDao.insert(new Tag(currentTask.getId(), currentTask.getUuid(), name, tagUid));
        }
        // Construct the TagData from Metadata
        // Fix for failed backup, Version before 4.6.10
        if (format == 2) {
          TagData tagData = tagDataDao.getByUuid(tagUid);
          if (tagData == null) {
            tagData = new TagData();
            tagData.setRemoteId(tagUid);
            tagData.setName(name);
            tagDataDao.createNew(tagData);
          }
        }
      } else if ("gtasks".equals(key)) {
        GoogleTask googleTask = new GoogleTask();
        googleTask.setTask(currentTask.getId());
        googleTask.setRemoteId(xml.readString("value"));
        googleTask.setListId(xml.readString("value2"));
        googleTask.setParent(xml.readLong("value3"));
        googleTask.setIndent(xml.readInteger("value4"));
        googleTask.setOrder(xml.readLong("value5"));
        googleTask.setRemoteOrder(xml.readLong("value6"));
        googleTask.setLastSync(xml.readLong("value7"));
        googleTask.setDeleted(xml.readLong("deleted"));
        googleTaskDao.insert(googleTask);
      }
    }
  }

  private class Format3TaskImporter extends Format2TaskImporter {

    Format3TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
      this.xpp = xpp;
      while (xpp.next() != XmlPullParser.END_DOCUMENT) {
        String tag = xpp.getName();
        if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG) {
          continue;
        }

        try {
          switch (tag) {
            case BackupConstants.TASK_TAG:
              parseTask();
              break;
            case BackupConstants.METADATA_TAG:
              parseMetadata(3);
              break;
            case BackupConstants.COMMENT_TAG:
              parseComment();
              break;
            case BackupConstants.TAGDATA_TAG:
              parseTagdata();
              break;
          }
        } catch (Exception e) {
          errorCount++;
          Timber.e(e);
        }
      }
    }

    private void parseTagdata() {
      TagData tagData = new TagData(new XmlReader(xpp));
      if (tagDataDao.getByUuid(tagData.getRemoteId()) == null) {
        tagDataDao.createNew(tagData);
      }
    }
  }
}
