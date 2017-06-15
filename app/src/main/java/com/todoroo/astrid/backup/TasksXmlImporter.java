/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;

import com.google.common.collect.Iterables;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class TasksXmlImporter {

    private final TagDataDao tagDataDao;
    private final MetadataDao metadataDao;
    private final UserActivityDao userActivityDao;
    private final DialogBuilder dialogBuilder;
    private final TaskDao taskDao;

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
    public TasksXmlImporter(TagDataDao tagDataDao, MetadataDao metadataDao, UserActivityDao userActivityDao,
                            DialogBuilder dialogBuilder, TaskDao taskDao) {
        this.tagDataDao = tagDataDao;
        this.metadataDao = metadataDao;
        this.userActivityDao = userActivityDao;
        this.dialogBuilder = dialogBuilder;
        this.taskDao = taskDao;
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
                        } else if(TextUtils.equals(format, FORMAT3)) {
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
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            activity.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
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
        final UserActivity userActivity = new UserActivity();
        final Metadata metadata = new Metadata();
        final TagData tagdata = new TagData();

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
            userActivity.clear();

            // We only want to import these fields.
            // These are the fields that are sent in EditNoteActivity.addComment() when a user creates a comment.
            final Property<?>[] NEW_PROPERTIES = new Property<?>[4];
            NEW_PROPERTIES[0] = UserActivity.MESSAGE;
            NEW_PROPERTIES[1] = UserActivity.ACTION;
            NEW_PROPERTIES[2] = UserActivity.TARGET_ID;
            NEW_PROPERTIES[3] = UserActivity.CREATED_AT;
            deserializeModel(userActivity, NEW_PROPERTIES);

            userActivity.setId(UserActivity.NO_ID);

            userActivityDao.createNew(userActivity);
        }

        void parseMetadata(int format) {
            if(!currentTask.isSaved()) {
                return;
            }
            metadata.clear();
            deserializeModel(metadata, Metadata.PROPERTIES);
            if (metadata.getKey().equals(TaskToTagMetadata.KEY)) {
                String uuid = metadata.getValue(TaskToTagMetadata.TAG_UUID);
                List<Metadata> metadatas = metadataDao.byTaskAndKey(currentTask.getId(), TaskToTagMetadata.KEY);
                if (Iterables.any(metadatas, existing -> uuid.equals(existing.getValue(TaskToTagMetadata.TAG_UUID)))) {
                    return;
                }
            }
            metadata.setId(Metadata.NO_ID);
            metadata.setTask(currentTask.getId());
            metadataDao.persist(metadata);

            // Construct the TagData from Metadata
            // Fix for failed backup, Version before 4.6.10
            if (format == 2) {
                String key = metadata.getKey();
                String name = metadata.getValue(Metadata.VALUE1);
                String uuid = metadata.getValue(Metadata.VALUE2);
                long deletionDate = metadata.getDeletionDate();
                // UUID is uniquely for every TagData, so we don't need to test the name
                TagData tagData = tagDataDao.getByUuid(uuid, TagData.ID);
                //If you sync with Google tasks it adds some Google task metadata.
                //For this metadata we don't create a list!
                if(key.equals(TaskToTagMetadata.KEY) && tagData == null && deletionDate == 0) {
                    tagdata.clear();
                    tagdata.setId(TagData.NO_ID);
                    tagdata.setUuid(uuid);
                    tagdata.setName(name);
                    tagDataDao.persist(tagdata);
                }
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
                    }
                } catch (Exception e) {
                    errorCount++;
                    Timber.e(e, e.getMessage());
                }
            }
        }

        private void parseTagdata() {
            tagdata.clear();
            deserializeModel(tagdata, TagData.PROPERTIES);
            if (tagDataDao.getByUuid(tagdata.getUuid()) == null) {
                tagDataDao.persist(tagdata);
            }
        }
    }
}
