/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.legacy.LegacyImportance;
import com.todoroo.astrid.legacy.LegacyRepeatInfo;
import com.todoroo.astrid.legacy.LegacyRepeatInfo.LegacyRepeatInterval;
import com.todoroo.astrid.legacy.LegacyTaskModel;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.tags.TagService;

public class TasksXmlImporter {

    // --- public interface

    /**
     * Import tasks from the given file
     *
     * @param input
     * @param runAfterImport
     */
    public static void importTasks(Context context, String input, Runnable runAfterImport) {
        new TasksXmlImporter(context, input, runAfterImport);
    }

    // --- implementation

    private final Handler handler;
    private int taskCount;
    private int importCount = 0;
    private int skipCount = 0;
    private int errorCount = 0;
    private final String input;

    private final Context context;
    private final TaskService taskService = PluginServices.getTaskService();
    private final MetadataService metadataService = PluginServices.getMetadataService();
    private final ExceptionService exceptionService = PluginServices.getExceptionService();
    private final ProgressDialog progressDialog;
    private final Runnable runAfterImport;

    private void setProgressMessage(final String message) {
        handler.post(new Runnable() {
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }

    /**
     * Import tasks.
     * @param runAfterImport optional runnable after import
     */
    private TasksXmlImporter(final Context context, String input, Runnable runAfterImport) {
        this.input = input;
        this.context = context;
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
            if(context instanceof Activity)
                progressDialog.setOwnerActivity((Activity)context);
        } catch (BadTokenException e) {
            // Running from a unit test or some such thing
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    performImport();
                } catch (IOException e) {
                    exceptionService.displayAndReportError(context,
                            context.getString(R.string.backup_TXI_error), e);
                } catch (XmlPullParserException e) {
                    exceptionService.displayAndReportError(context,
                            context.getString(R.string.backup_TXI_error), e);
                }
            }
        }).start();
    }

    @SuppressWarnings("nls")
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
                        if(TextUtils.equals(format, FORMAT1))
                            new Format1TaskImporter(xpp);
                        else if(TextUtils.equals(format, FORMAT2))
                            new Format2TaskImporter(xpp);
                        else
                            throw new UnsupportedOperationException(
                                    "Did not know how to import tasks with xml format '" +
                                    format + "'");
                    }
                }
            }
        } finally {
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(progressDialog.isShowing() && context instanceof Activity)
                       DialogUtilities.dismissDialog((Activity) context, progressDialog);
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

        private int version;
        private final XmlPullParser xpp;
        private final Task currentTask = new Task();
        private final Metadata metadata = new Metadata();

        public Format2TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            this.xpp = xpp;

            try {
                this.version = Integer.parseInt(xpp.getAttributeValue(null, BackupConstants.ASTRID_ATTR_VERSION));
            } catch (NumberFormatException e) {
                // can't read version, assume max version
                this.version = Integer.MAX_VALUE;
            }

            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                String tag = xpp.getName();
                if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG)
                    continue;

                try {
                    if (tag.equals(BackupConstants.TASK_TAG)) {
                        // Parse <task ... >
                        parseTask();
                    } else if (tag.equals(BackupConstants.METADATA_TAG)) {
                        // Process <metadata ... >
                        parseMetadata();
                    }
                } catch (Exception e) {
                    errorCount++;
                    Log.e("astrid-importer", //$NON-NLS-1$
                            "Caught exception while reading from " + //$NON-NLS-1$
                            xpp.getText(), e);
                }
            }
        }

        @SuppressWarnings("nls")
        private void parseTask() {
            taskCount++;
            setProgressMessage(context.getString(R.string.import_progress_read,
                    taskCount));
            currentTask.clear();

            String title = xpp.getAttributeValue(null, Task.TITLE.name);
            String created = xpp.getAttributeValue(null, Task.CREATION_DATE.name);
            String deletionDate = xpp.getAttributeValue(null, Task.DELETION_DATE.name);
            String completionDate = xpp.getAttributeValue(null, Task.COMPLETION_DATE.name);

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

                    // fix for failed migration in 4.0.6
                    if(version < UpgradeService.V4_0_6) {
                        if(!completionDate.equals("0") &&
                                !completionDate.equals(Long.toString(cursor.get(Task.COMPLETION_DATE))))
                            existingTask = cursor.get(Task.ID);

                        if(!deletionDate.equals("0") &&
                                !deletionDate.equals(Long.toString(cursor.get(Task.DELETION_DATE))))
                            existingTask = cursor.get(Task.ID);
                    }

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
            if(version < UpgradeService.V4_0_6)
                adjustDueDateScheme(currentTask);

            if(existingTask > 0)
                currentTask.setId(existingTask);
            else
                currentTask.setId(Task.NO_ID);

            // Save the task to the database.
            taskService.save(currentTask);
            importCount++;
        }

        private void adjustDueDateScheme(Task model) {
            long dueDate = model.getValue(Task.DUE_DATE);

            if (dueDate > 0) {
                Date date = new Date(dueDate);
                if (date.getHours() == 23 && date.getMinutes() == 59 && date.getSeconds() == 59) {
                    date.setHours(12);
                    date.setMinutes(0);
                    date.setSeconds(0);
                } else {
                    date.setSeconds(1);
                }
                model.setValue(Task.DUE_DATE, date.getTime());
            }
        }

        private void parseMetadata() {
            if(!currentTask.isSaved())
                return;
            metadata.clear();
            deserializeModel(metadata, Metadata.PROPERTIES);
            metadata.setId(Metadata.NO_ID);
            metadata.setValue(Metadata.TASK, currentTask.getId());
            metadataService.save(metadata);
        }

        /**
         * Turn a model into xml attributes
         * @param model
         */
        private void deserializeModel(AbstractModel model, Property<?>[] properties) {
            for(Property<?> property : properties) {
                try {
                    property.accept(xmlReadingVisitor, model);
                } catch (Exception e) {
                    Log.e("astrid-importer", //$NON-NLS-1$
                            "Caught exception while writing " + property.name + //$NON-NLS-1$
                            " from " + xpp.getText(), e); //$NON-NLS-1$
                }
            }
        }

        private final XmlReadingPropertyVisitor xmlReadingVisitor = new XmlReadingPropertyVisitor();

        private class XmlReadingPropertyVisitor implements PropertyVisitor<Void, AbstractModel> {

            @Override
            public Void visitInteger(Property<Integer> property,
                    AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null)
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Integer.parseInt(value));
                return null;
            }

            @Override
            public Void visitLong(Property<Long> property, AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null)
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Long.parseLong(value));
                return null;
            }

            @Override
            public Void visitDouble(Property<Double> property,
                    AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null)
                    data.setValue(property, TasksXmlExporter.XML_NULL.equals(value) ?
                            null : Double.parseDouble(value));
                return null;
            }

            @Override
            public Void visitString(Property<String> property,
                    AbstractModel data) {
                String value = xpp.getAttributeValue(null, property.name);
                if(value != null)
                    data.setValue(property, value);
                return null;
            }

        }
    }

    // =============================================================== FORMAT1

    private static final String FORMAT1 = null;
    private class Format1TaskImporter {

        private final XmlPullParser xpp;
        private Task currentTask = null;
        private String upgradeNotes = null;
        private boolean syncOnComplete = false;

        private final LinkedHashSet<String> tags = new LinkedHashSet<String>();

        public Format1TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            this.xpp = xpp;

            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                String tag = xpp.getName();

                try {
                    if(BackupConstants.TASK_TAG.equals(tag) && xpp.getEventType() == XmlPullParser.END_TAG)
                        saveTags();
                    else if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG)
                        continue;
                    else if (tag.equals(BackupConstants.TASK_TAG)) {
                        // Parse <task ... >
                        currentTask = parseTask();
                    } else if (currentTask != null) {
                        // These tags all require that we have a task to associate
                        // them with.
                        if (tag.equals(BackupConstants.TAG_TAG)) {
                            // Process <tag ... >
                            parseTag();
                        } else if (tag.equals(BackupConstants.ALERT_TAG)) {
                            // Process <alert ... >
                            parseAlert();
                        } else if (tag.equals(BackupConstants.SYNC_TAG)) {
                            // Process <sync ... >
                            parseSync();
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    Log.e("astrid-importer", //$NON-NLS-1$
                            "Caught exception while reading from " + //$NON-NLS-1$
                            xpp.getText(), e);
                }
            }
        }

        private boolean parseSync() {
            String service = xpp.getAttributeValue(null, BackupConstants.SYNC_ATTR_SERVICE);
            String remoteId = xpp.getAttributeValue(null, BackupConstants.SYNC_ATTR_REMOTE_ID);
            if (service != null && remoteId != null) {
                StringTokenizer strtok = new StringTokenizer(remoteId, "|"); //$NON-NLS-1$
                String taskId = strtok.nextToken();
                String taskSeriesId = strtok.nextToken();
                String listId = strtok.nextToken();

                Metadata metadata = new Metadata();
                metadata.setValue(Metadata.TASK, currentTask.getId());
                metadata.setValue(Metadata.VALUE1, (listId));
                metadata.setValue(Metadata.VALUE2, (taskSeriesId));
                metadata.setValue(Metadata.VALUE3, (taskId));
                metadata.setValue(Metadata.VALUE4, syncOnComplete ? "1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$
                metadataService.save(metadata);
                return true;
            }
            return false;
        }

        private boolean parseAlert() {
            // drop it
            return false;
        }

        private boolean parseTag() {
            String tagName = xpp.getAttributeValue(null, BackupConstants.TAG_ATTR_NAME);
            tags.add(tagName);
            return true;
        }

        private void saveTags() {
            if(currentTask != null && tags.size() > 0) {
                TagService.getInstance().synchronizeTags(currentTask.getId(), currentTask.getValue(Task.UUID), tags);
            }
            tags.clear();
        }

        @SuppressWarnings("nls")
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
                    Log.i("astrid-xml-import", "Task: " + taskName + ": Unknown field '" +
                            fieldName + "' with value '" + fieldValue + "' disregarded.");
                }
            }

            if(upgradeNotes != null) {
                if(task.containsValue(Task.NOTES) && task.getValue(Task.NOTES).length() > 0)
                    task.setValue(Task.NOTES, task.getValue(Task.NOTES) + "\n" + upgradeNotes);
                else
                    task.setValue(Task.NOTES, upgradeNotes);
                upgradeNotes = null;
            }

            // Save the task to the database.
            taskService.save(task);
            importCount++;
            return task;
        }

        /** helper method to set field on a task */
        @SuppressWarnings("nls")
        private final boolean setTaskField(Task task, String field, String value) {
            if(field.equals(LegacyTaskModel.ID)) {
                // ignore
            }
            else if(field.equals(LegacyTaskModel.NAME)) {
                task.setValue(Task.TITLE, value);
            }
            else if(field.equals(LegacyTaskModel.NOTES)) {
                task.setValue(Task.NOTES, value);
            }
            else if(field.equals(LegacyTaskModel.PROGRESS_PERCENTAGE)) {
                // ignore
            }
            else if(field.equals(LegacyTaskModel.IMPORTANCE)) {
                task.setValue(Task.IMPORTANCE, LegacyImportance.valueOf(value).ordinal());
            }
            else if(field.equals(LegacyTaskModel.ESTIMATED_SECONDS)) {
                task.setValue(Task.ESTIMATED_SECONDS, Integer.parseInt(value));
            }
            else if(field.equals(LegacyTaskModel.ELAPSED_SECONDS)) {
                task.setValue(Task.ELAPSED_SECONDS, Integer.parseInt(value));
            }
            else if(field.equals(LegacyTaskModel.TIMER_START)) {
                task.setValue(Task.TIMER_START,
                        BackupDateUtilities.getDateFromIso8601String(value).getTime());
            }
            else if(field.equals(LegacyTaskModel.DEFINITE_DUE_DATE)) {
                String preferred = xpp.getAttributeValue(null, LegacyTaskModel.PREFERRED_DUE_DATE);
                if(preferred != null) {
                    Date preferredDate = BackupDateUtilities.getDateFromIso8601String(value);
                    upgradeNotes = "Project Deadline: " +
                            DateUtilities.getDateString(ContextManager.getContext(),
                                    preferredDate);
                }
                task.setValue(Task.DUE_DATE,
                        BackupDateUtilities.getTaskDueDateFromIso8601String(value).getTime());
            }
            else if(field.equals(LegacyTaskModel.PREFERRED_DUE_DATE)) {
                String definite = xpp.getAttributeValue(null, LegacyTaskModel.DEFINITE_DUE_DATE);
                if(definite != null)
                    ; // handled above
                else
                    task.setValue(Task.DUE_DATE,
                            BackupDateUtilities.getTaskDueDateFromIso8601String(value).getTime());
            }
            else if(field.equals(LegacyTaskModel.HIDDEN_UNTIL)) {
                task.setValue(Task.HIDE_UNTIL,
                        BackupDateUtilities.getDateFromIso8601String(value).getTime());
            }
            else if(field.equals(LegacyTaskModel.BLOCKING_ON)) {
                // ignore
            }
            else if(field.equals(LegacyTaskModel.POSTPONE_COUNT)) {
                task.setValue(Task.POSTPONE_COUNT, Integer.parseInt(value));
            }
            else if(field.equals(LegacyTaskModel.NOTIFICATIONS)) {
                task.setValue(Task.REMINDER_PERIOD, Integer.parseInt(value) * 1000L);
            }
            else if(field.equals(LegacyTaskModel.CREATION_DATE)) {
                task.setValue(Task.CREATION_DATE,
                        BackupDateUtilities.getDateFromIso8601String(value).getTime());
            }
            else if(field.equals(LegacyTaskModel.COMPLETION_DATE)) {
                String completion = xpp.getAttributeValue(null, LegacyTaskModel.PROGRESS_PERCENTAGE);
                if("100".equals(completion)) {
                    task.setValue(Task.COMPLETION_DATE,
                        BackupDateUtilities.getDateFromIso8601String(value).getTime());
                }
            }
            else if(field.equals(LegacyTaskModel.NOTIFICATION_FLAGS)) {
                task.setValue(Task.REMINDER_FLAGS, Integer.parseInt(value));
            }
            else if(field.equals(LegacyTaskModel.LAST_NOTIFIED)) {
                task.setValue(Task.REMINDER_LAST,
                        BackupDateUtilities.getDateFromIso8601String(value).getTime());
            }
            else if(field.equals("repeat_interval")) {
                // handled below
            }
            else if(field.equals("repeat_value")) {
                int repeatValue = Integer.parseInt(value);
                String repeatInterval = xpp.getAttributeValue(null, "repeat_interval");
                if(repeatValue > 0 && repeatInterval != null) {
                    LegacyRepeatInterval interval = LegacyRepeatInterval.valueOf(repeatInterval);
                    LegacyRepeatInfo repeatInfo = new LegacyRepeatInfo(interval, repeatValue);
                    RRule rrule = repeatInfo.toRRule();
                    task.setValue(Task.RECURRENCE, rrule.toIcal());
                }
            }
            else if(field.equals(LegacyTaskModel.FLAGS)) {
                if(Integer.parseInt(value) == LegacyTaskModel.FLAG_SYNC_ON_COMPLETE)
                    syncOnComplete = true;
            }
            else {
                return false;
            }

            return true;
        }
    }

}
