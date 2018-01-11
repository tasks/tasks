/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;

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
import org.tasks.dialogs.DialogBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

public class TasksXmlImporter {

    private final TagDataDao tagDataDao;
    private final UserActivityDao userActivityDao;
    private final DialogBuilder dialogBuilder;
    private final TaskDao taskDao;
    private final LocalBroadcastManager localBroadcastManager;
    private final AlarmDao alarmDao;
    private final TagDao tagDao;
    private GoogleTaskDao googleTaskDao;
    private final LocationDao locationDao;

    private Activity activity;
    private Handler handler;
    private int taskCount;
    private int importCount = 0;
    private int skipCount = 0;
    private int errorCount = 0;
    private ProgressDialog progressDialog;
    private String input;

    private void setProgressMessage(final String message) {
        handler.post(() -> progressDialog.setMessage(message));
    }

    @Inject
    public TasksXmlImporter(TagDataDao tagDataDao, UserActivityDao userActivityDao,
                            DialogBuilder dialogBuilder, TaskDao taskDao, LocationDao locationDao,
                            LocalBroadcastManager localBroadcastManager, AlarmDao alarmDao,
                            TagDao tagDao, GoogleTaskDao googleTaskDao) {
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

    public void importTasks(Activity activity, String input, ProgressDialog progressDialog) {
        this.activity = activity;
        this.input = input;
        this.progressDialog = progressDialog;

        handler = new Handler();

        new Thread(() -> {
            try {
                performImport();
            } catch (IOException | XmlPullParserException e) {
                Timber.e(e, e.getMessage());
            }
        }).start();
    }

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
                        if(TextUtils.equals(format, FORMAT2)) {
                            new Format2TaskImporter(xpp);
                        } else if(TextUtils.equals(format, FORMAT3) || TextUtils.equals(format, FORMAT4)) {
                            new Format3TaskImporter(xpp);
                        } else {
                            throw new UnsupportedOperationException(
                                    "Did not know how to import tasks with xml format '" +
                                            format + "'");
                        }
                    }
                }
            }
        } finally {
            localBroadcastManager.broadcastRefresh();
            handler.post(() -> {
                if(progressDialog.isShowing()) {
                    DialogUtilities.dismissDialog(activity, progressDialog);
                    showSummary();
                }
            });
        }
    }

    private void showSummary() {
        Resources r = activity.getResources();
        dialogBuilder.newDialog()
                .setTitle(R.string.import_summary_title)
                .setMessage(activity.getString(R.string.import_summary_message,
                        input,
                        r.getQuantityString(R.plurals.Ntasks, taskCount, taskCount),
                        r.getQuantityString(R.plurals.Ntasks, importCount, importCount),
                        r.getQuantityString(R.plurals.Ntasks, skipCount, skipCount),
                        r.getQuantityString(R.plurals.Ntasks, errorCount, errorCount)))
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss())
                .show();
    }

    // --- importers

    // =============================================================== FORMAT2

    private static final String FORMAT2 = "2"; //$NON-NLS-1$
    private class Format2TaskImporter {

        XmlPullParser xpp;
        final Task currentTask = new Task();

        public Format2TaskImporter() { }
        public Format2TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
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
                    Timber.e(e, e.getMessage());
                }
            }
        }

        void parseTask() {
            taskCount++;
            setProgressMessage(activity.getString(R.string.import_progress_read, taskCount));
            currentTask.clear();

            String title = xpp.getAttributeValue(null, Task.TITLE.name);
            String created = xpp.getAttributeValue(null, Task.CREATION_DATE.name);

            // if we don't have task name or creation date, skip
            if (created == null || title == null) {
                skipCount++;
                return;
            }

            // if the task's name and creation date match an existing task, skip
            Query query = Query.select(Task.ID, Task.COMPLETION_DATE, Task.DELETION_DATE)
                    .where(Criterion.and(Task.TITLE.eq(title), Task.CREATION_DATE.eq(created)));
            if (taskDao.count(query) > 0) {
                skipCount++;
            } else {
                deserializeModel(currentTask, Task.PROPERTIES);

                currentTask.setId(Task.NO_ID);

                // Save the task to the database.
                taskDao.save(currentTask);
                importCount++;
            }
        }

        /**
         * Imports a comment from the XML we're reading.
         * taken from EditNoteActivity.addComment()
         */
        void parseComment() {
            if (!currentTask.isSaved()) {
                return;
            }

            UserActivity userActivity = new UserActivity(new XmlReader(xpp));
            userActivityDao.createNew(userActivity);
        }

        void parseAlarm() {
            if (!currentTask.isSaved()) {
                return;
            }

            Alarm alarm = new Alarm(new XmlReader(xpp));
            alarm.setTask(currentTask.getId());
            alarmDao.insert(alarm);
        }

        void parseLocation() {
            if (!currentTask.isSaved()) {
                return;
            }

            Location location = new Location(new XmlReader(xpp));
            location.setTask(currentTask.getId());
            locationDao.insert(location);
        }

        void parseTag() {
            if (!currentTask.isSaved()) {
                return;
            }

            Tag tag = new Tag(new XmlReader(xpp));
            tag.setTask(currentTask.getId());
            tagDao.insert(tag);
        }

        void parseGoogleTask() {
            if (!currentTask.isSaved()) {
                return;
            }

            GoogleTask googleTask = new GoogleTask(new XmlReader(xpp));
            googleTask.setTask(currentTask.getId());
            googleTaskDao.insert(googleTask);
        }

        void parseMetadata(int format) {
            if(!currentTask.isSaved()) {
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
                        tagDataDao.insert(tagData);
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

        /**
         * Turn a model into xml attributes
         */
        void deserializeModel(AbstractModel model, Property<?>[] properties) {
            for(Property<?> property : properties) {
                try {
                    property.accept(xmlReadingVisitor, model);
                } catch (Exception e) {
                    Timber.e(e, e.getMessage());
                }
            }
        }

        private final XmlReadingPropertyVisitor xmlReadingVisitor = new XmlReadingPropertyVisitor();

        private class XmlReadingPropertyVisitor implements PropertyVisitor<Void, AbstractModel> {

            @Override
            public Void visitInteger(Property<Integer> property,
                    AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null) {
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Integer.parseInt(value));
                }
                return null;
            }

            @Override
            public Void visitLong(Property<Long> property, AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null) {
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Long.parseLong(value));
                }
                return null;
            }

            @Override
            public Void visitDouble(Property<Double> property, AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if (value != null) {
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Double.parseDouble(value));
                }
                return null;
            }

            @Override
            public Void visitString(Property<String> property,
                    AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null) {
                    data.setValue(property, value);
                }
                return null;
            }

        }
    }

    // =============================================================== FORMAT3

    private static final String FORMAT3 = "3"; //$NON-NLS-1$
    private static final String FORMAT4 = "4"; //$NON-NLS-1$
    private class Format3TaskImporter extends Format2TaskImporter {

        public Format3TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
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
                        case BackupConstants.ALARM_TAG:
                            parseAlarm();
                            break;
                        case BackupConstants.LOCATION_TAG:
                            parseLocation();
                            break;
                        case BackupConstants.TAG_TAG:
                            parseTag();
                            break;
                        case BackupConstants.GOOGLE_TASKS_TAG:
                            parseGoogleTask();
                            break;
                    }
                } catch (Exception e) {
                    errorCount++;
                    Timber.e(e, e.getMessage());
                }
            }
        }

        private void parseTagdata() {
            TagData tagData = new TagData(new XmlReader(xpp));
            if (tagDataDao.getByUuid(tagData.getRemoteId()) == null) {
                tagDataDao.insert(tagData);
            }
        }
    }
}
