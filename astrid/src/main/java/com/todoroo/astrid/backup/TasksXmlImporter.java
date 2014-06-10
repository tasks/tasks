/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.view.WindowManager.BadTokenException;

import com.google.ical.values.RRule;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.legacy.LegacyImportance;
import com.todoroo.astrid.legacy.LegacyRepeatInfo;
import com.todoroo.astrid.legacy.LegacyRepeatInfo.LegacyRepeatInterval;
import com.todoroo.astrid.legacy.LegacyTaskModel;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import javax.inject.Inject;

public class TasksXmlImporter {

    private static final Logger log = LoggerFactory.getLogger(TasksXmlImporter.class);

    private final TagDataService tagDataService;
    private final TagService tagService;
    private final MetadataService metadataService;
    private final TaskService taskService;

    private Context context;
    private Handler handler;
    private int taskCount;
    private int importCount = 0;
    private int skipCount = 0;
    private int errorCount = 0;
    private ProgressDialog progressDialog;
    private Runnable runAfterImport;
    private String input;

    private void setProgressMessage(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }

    @Inject
    public TasksXmlImporter(TagDataService tagDataService, TagService tagService, MetadataService metadataService, TaskService taskService) {
        this.tagDataService = tagDataService;
        this.tagService = tagService;
        this.metadataService = metadataService;
        this.taskService = taskService;
    }

    /**
     * Import tasks.
     * @param runAfterImport optional runnable after import
     */
    public void importTasks(Context context, String input, Runnable runAfterImport) {
        this.context = context;
        this.input = input;
        this.runAfterImport = runAfterImport;

        handler = new Handler();
        progressDialog = new ProgressDialog(context);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(R.string.import_progress_title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        try {
            progressDialog.show();
            if(context instanceof Activity) {
                progressDialog.setOwnerActivity((Activity) context);
            }
        } catch (BadTokenException e) {
            // Running from a unit test or some such thing
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    performImport();
                } catch (IOException | XmlPullParserException e) {
                    log.error(e.getMessage(), e);
                }
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
                        if(TextUtils.equals(format, FORMAT1)) {
                            new Format1TaskImporter(xpp);
                        } else if(TextUtils.equals(format, FORMAT2)) {
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
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(progressDialog.isShowing() && context instanceof Activity) {
                        DialogUtilities.dismissDialog((Activity) context, progressDialog);
                    }
                    showSummary();
                }
            });
        }
    }

    private void showSummary() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.import_summary_title);
        Resources r = context.getResources();
        String message = context.getString(R.string.import_summary_message,
                input,
                r.getQuantityString(R.plurals.Ntasks, taskCount, taskCount),
                r.getQuantityString(R.plurals.Ntasks, importCount, importCount),
                r.getQuantityString(R.plurals.Ntasks, skipCount, skipCount),
                r.getQuantityString(R.plurals.Ntasks, errorCount, errorCount));
        builder.setMessage(message);
        builder.setPositiveButton(context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (runAfterImport != null) {
                            handler.post(runAfterImport);
                        }
                    }
        });

        builder.show();
    }

    // --- importers

    // =============================================================== FORMAT2

    private static final String FORMAT2 = "2"; //$NON-NLS-1$
    private class Format2TaskImporter {

        protected XmlPullParser xpp;
        protected Task currentTask = new Task();
        protected Metadata metadata = new Metadata();
        protected TagData tagdata = new TagData();

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
                    } else if (tag.equals(BackupConstants.METADATA_TAG)) {
                        // Process <metadata ... >
                        parseMetadata(2);
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error(e.getMessage(), e);
                }
            }
        }

        protected void parseTask() {
            taskCount++;
            setProgressMessage(context.getString(R.string.import_progress_read,
                    taskCount));
            currentTask.clear();

            String title = xpp.getAttributeValue(null, Task.TITLE.name);
            String created = xpp.getAttributeValue(null, Task.CREATION_DATE.name);

            // if we don't have task name or creation date, skip
            if (created == null || title == null) {
                skipCount++;
                return;
            }

            // if the task's name and creation date match an existing task, skip
            long existingTask = 0;
            TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID,
                        Task.COMPLETION_DATE, Task.DELETION_DATE).
                    where(Criterion.and(Task.TITLE.eq(title), Task.CREATION_DATE.eq(created))));
            try {
                if(cursor.getCount() > 0) {
                    cursor.moveToNext();

                    if(existingTask == 0) {
                        skipCount++;
                        return;
                    }
                }
            } finally {
                cursor.close();
            }

            // else, make a new task model and add away.
            deserializeModel(currentTask, Task.PROPERTIES);

            if(existingTask > 0) {
                currentTask.setId(existingTask);
            } else {
                currentTask.setId(Task.NO_ID);
            }

            // Save the task to the database.
            taskService.save(currentTask);
            importCount++;
        }

        protected void parseMetadata(int format) {
            if(!currentTask.isSaved()) {
                return;
            }
            metadata.clear();
            deserializeModel(metadata, Metadata.PROPERTIES);
            metadata.setId(Metadata.NO_ID);
            metadata.setTask(currentTask.getId());
            metadataService.save(metadata);

            // Construct the TagData from Metadata
            // Fix for failed backup, Version before 4.6.10
            if (format == 2) {
                String key = metadata.getKey();
                String name = metadata.getValue(Metadata.VALUE1);
                String uuid = metadata.getValue(Metadata.VALUE2);
                long deletionDate = metadata.getDeletionDate();
                // UUID is uniquely for every TagData, so we don't need to test the name
                TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.ID).
                        where(TagData.UUID.eq(uuid)));
                try {
                    //If you sync with Google tasks it adds some Google task metadata.
                    //For this metadata we don't create a list!
                    if(key.equals(TaskToTagMetadata.KEY) && cursor.getCount() == 0 && deletionDate == 0) {
                        tagdata.clear();
                        tagdata.setId(TagData.NO_ID);
                        tagdata.setUuid(uuid);
                        tagdata.setName(name);
                        tagDataService.save(tagdata);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        /**
         * Turn a model into xml attributes
         */
        protected void deserializeModel(AbstractModel model, Property<?>[] properties) {
            for(Property<?> property : properties) {
                try {
                    property.accept(xmlReadingVisitor, model);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
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
                        case BackupConstants.TAGDATA_TAG:
                            parseTagdata();
                            break;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error(e.getMessage(), e);
                }
            }
        }

        private void parseTagdata() {
            tagdata.clear();
            deserializeModel(tagdata, TagData.PROPERTIES);
            tagDataService.save(tagdata);
        }
    }

    // =============================================================== FORMAT1

    private static final String FORMAT1 = null;
    private class Format1TaskImporter {

        private final XmlPullParser xpp;
        private Task currentTask = null;
        private String upgradeNotes = null;
        private boolean syncOnComplete = false;

        private final LinkedHashSet<String> tags = new LinkedHashSet<>();

        public Format1TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            this.xpp = xpp;

            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                String tag = xpp.getName();

                try {
                    if(BackupConstants.TASK_TAG.equals(tag) && xpp.getEventType() == XmlPullParser.END_TAG) {
                        saveTags();
                    } else if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG) {
                    } else if (tag.equals(BackupConstants.TASK_TAG)) {
                        // Parse <task ... >
                        currentTask = parseTask();
                    } else if (currentTask != null) {
                        // These tags all require that we have a task to associate
                        // them with.
                        if (tag.equals(BackupConstants.TAG_TAG)) {
                            // Process <tag ... >
                            parseTag();
                        } else if (tag.equals(BackupConstants.SYNC_TAG)) {
                            // Process <sync ... >
                            parseSync();
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error(e.getMessage(), e);
                }
            }
        }

        private void parseSync() {
            String service = xpp.getAttributeValue(null, BackupConstants.SYNC_ATTR_SERVICE);
            String remoteId = xpp.getAttributeValue(null, BackupConstants.SYNC_ATTR_REMOTE_ID);
            if (service != null && remoteId != null) {
                StringTokenizer strtok = new StringTokenizer(remoteId, "|"); //$NON-NLS-1$
                String taskId = strtok.nextToken();
                String taskSeriesId = strtok.nextToken();
                String listId = strtok.nextToken();

                Metadata metadata = new Metadata();
                metadata.setTask(currentTask.getId());
                metadata.setValue1(listId);
                metadata.setValue2(taskSeriesId);
                metadata.setValue3(taskId);
                metadata.setValue4(syncOnComplete ? "1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$
                metadataService.save(metadata);
            }
        }

        private void parseTag() {
            String tagName = xpp.getAttributeValue(null, BackupConstants.TAG_ATTR_NAME);
            tags.add(tagName);
        }

        private void saveTags() {
            if(currentTask != null && tags.size() > 0) {
                tagService.synchronizeTags(currentTask.getId(), currentTask.getUUID(), tags);
            }
            tags.clear();
        }

        private Task parseTask() {
            taskCount++;
            setProgressMessage(context.getString(R.string.import_progress_read,
                    taskCount));

            String taskName = xpp.getAttributeValue(null, LegacyTaskModel.NAME);
            Date creationDate = null;
            String createdString = xpp.getAttributeValue(null,
                    LegacyTaskModel.CREATION_DATE);
            if (createdString != null) {
                creationDate = BackupDateUtilities.getDateFromIso8601String(createdString);
            }

            // if we don't have task name or creation date, skip
            if (creationDate == null || taskName == null) {
                skipCount++;
                return null;
            }

            // if the task's name and creation date match an existing task, skip
            TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID).
                    where(Criterion.and(Task.TITLE.eq(taskName),
                            Task.CREATION_DATE.like(creationDate.getTime()/1000L + "%"))));
            try {
                if(cursor.getCount() > 0) {
                    skipCount++;
                    return null;
                }
            } finally {
                cursor.close();
            }

            // else, make a new task model and add away.
            Task task = new Task();
            int numAttributes = xpp.getAttributeCount();
            for (int i = 0; i < numAttributes; i++) {
                String fieldName = xpp.getAttributeName(i);
                String fieldValue = xpp.getAttributeValue(i);
                if(!setTaskField(task, fieldName, fieldValue)) {
                    log.info("Task: {}: Unknown field '{}' with value '{}' disregarded.", taskName, fieldName, fieldValue);
                }
            }

            if(upgradeNotes != null) {
                if(task.containsValue(Task.NOTES) && task.getNotes().length() > 0) {
                    task.setNotes(task.getNotes() + "\n" + upgradeNotes);
                } else {
                    task.setNotes(upgradeNotes);
                }
                upgradeNotes = null;
            }

            // Save the task to the database.
            taskService.save(task);
            importCount++;
            return task;
        }

        /** helper method to set field on a task */
        private boolean setTaskField(Task task, String field, String value) {
            switch (field) {
                case LegacyTaskModel.ID:
                    // ignore
                    break;
                case LegacyTaskModel.NAME:
                    task.setTitle(value);
                    break;
                case LegacyTaskModel.NOTES:
                    task.setNotes(value);
                    break;
                case LegacyTaskModel.PROGRESS_PERCENTAGE:
                    // ignore
                    break;
                case LegacyTaskModel.IMPORTANCE:
                    task.setImportance(LegacyImportance.valueOf(value).ordinal());
                    break;
                case LegacyTaskModel.ESTIMATED_SECONDS:
                    task.setEstimatedSeconds(Integer.parseInt(value));
                    break;
                case LegacyTaskModel.ELAPSED_SECONDS:
                    task.setELAPSED_SECONDS(Integer.parseInt(value));
                    break;
                case LegacyTaskModel.TIMER_START:
                    task.setTimerStart(
                            BackupDateUtilities.getDateFromIso8601String(value).getTime());
                    break;
                case LegacyTaskModel.DEFINITE_DUE_DATE:
                    String preferred = xpp.getAttributeValue(null, LegacyTaskModel.PREFERRED_DUE_DATE);
                    if (preferred != null) {
                        Date preferredDate = BackupDateUtilities.getDateFromIso8601String(value);
                        upgradeNotes = "Project Deadline: " +
                                DateUtilities.getDateString(preferredDate);
                    }
                    task.setDueDate(
                            BackupDateUtilities.getTaskDueDateFromIso8601String(value).getTime());
                    break;
                case LegacyTaskModel.PREFERRED_DUE_DATE:
                    String definite = xpp.getAttributeValue(null, LegacyTaskModel.DEFINITE_DUE_DATE);
                    if (definite != null) {
                        // handled above
                    } else {
                        task.setDueDate(
                                BackupDateUtilities.getTaskDueDateFromIso8601String(value).getTime());
                    }
                    break;
                case LegacyTaskModel.HIDDEN_UNTIL:
                    task.setHideUntil(
                            BackupDateUtilities.getDateFromIso8601String(value).getTime());
                    break;
                case LegacyTaskModel.BLOCKING_ON:
                    // ignore
                    break;
                case LegacyTaskModel.POSTPONE_COUNT:
                    task.setPostponeCount(Integer.parseInt(value));
                    break;
                case LegacyTaskModel.NOTIFICATIONS:
                    task.setReminderPeriod(Integer.parseInt(value) * 1000L);
                    break;
                case LegacyTaskModel.CREATION_DATE:
                    task.setCreationDate(
                            BackupDateUtilities.getDateFromIso8601String(value).getTime());
                    break;
                case LegacyTaskModel.COMPLETION_DATE:
                    String completion = xpp.getAttributeValue(null, LegacyTaskModel.PROGRESS_PERCENTAGE);
                    if ("100".equals(completion)) {
                        task.setCompletionDate(
                                BackupDateUtilities.getDateFromIso8601String(value).getTime());
                    }
                    break;
                case LegacyTaskModel.NOTIFICATION_FLAGS:
                    task.setReminderFlags(Integer.parseInt(value));
                    break;
                case LegacyTaskModel.LAST_NOTIFIED:
                    task.setReminderLast(
                            BackupDateUtilities.getDateFromIso8601String(value).getTime());
                    break;
                case "repeat_interval":
                    // handled below
                    break;
                case "repeat_value":
                    int repeatValue = Integer.parseInt(value);
                    String repeatInterval = xpp.getAttributeValue(null, "repeat_interval");
                    if (repeatValue > 0 && repeatInterval != null) {
                        LegacyRepeatInterval interval = LegacyRepeatInterval.valueOf(repeatInterval);
                        LegacyRepeatInfo repeatInfo = new LegacyRepeatInfo(interval, repeatValue);
                        RRule rrule = repeatInfo.toRRule();
                        task.setRecurrence(rrule.toIcal());
                    }
                    break;
                case LegacyTaskModel.FLAGS:
                    if (Integer.parseInt(value) == LegacyTaskModel.FLAG_SYNC_ON_COMPLETE) {
                        syncOnComplete = true;
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

}
