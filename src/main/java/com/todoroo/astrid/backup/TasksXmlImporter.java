/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.WindowManager.BadTokenException;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskTimeLogDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskTimeLog;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TaskToTagMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileReader;
import java.io.IOException;

import javax.inject.Inject;

public class TasksXmlImporter {

    private static final Logger log = LoggerFactory.getLogger(TasksXmlImporter.class);

    private final TagDataDao tagDataDao;
    private final MetadataDao metadataDao;
    private final TaskService taskService;
    private final TaskTimeLogDao taskTimeLogDao;

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
    public TasksXmlImporter(TagDataDao tagDataDao, MetadataDao metadataDao, TaskService taskService, TaskTimeLogDao taskTimeLogDao) {
        this.tagDataDao = tagDataDao;
        this.metadataDao = metadataDao;
        this.taskService = taskService;
        this.taskTimeLogDao = taskTimeLogDao;
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
            log.error(e.getMessage(), e);
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
                        if(TextUtils.equals(format, FORMAT2)) {
                            new Format2TaskImporter(xpp);
                        } else if(TextUtils.equals(format, FORMAT3)) {
                            new Format3TaskImporter(xpp);
                        } else if (TextUtils.equals(format, FORMAT4)) {
                            new Format4TaskImporter(xpp);
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

        protected int format;
        protected XmlPullParser xpp;
        protected Task currentTask = new Task();
        protected Metadata metadata = new Metadata();
        protected TagData tagdata = new TagData();

        public Format2TaskImporter() {}
        public Format2TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            format = 2;
            this.xpp = xpp;
            int appVersion = parseAppVersion(xpp);
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
                        parseMetadata();
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
            long existingTask = 0; //FIXME existingTask is always 0
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

            boolean isExistingTask = existingTask > 0;
            if(isExistingTask) {
                currentTask.setId(existingTask);
            } else {
                currentTask.setId(Task.NO_ID);
            }

            // Save the task to the database.
            taskService.save(currentTask);

            if (!isExistingTask){
                switch (format){
                    case 2:
                    case 3:
                        TaskTimeLog timeLog = TaskTimeLogDao.createTimeLogFromTask(currentTask);
                        if (timeLog != null) {
                            taskTimeLogDao.persist(timeLog);
                            taskService.save(currentTask);
                        }
                    case 4:
                }

            }
            importCount++;
        }

        protected void parseMetadata() {
            if(!currentTask.isSaved()) {
                return;
            }
            metadata.clear();
            deserializeModel(metadata, Metadata.PROPERTIES);
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

        public Format3TaskImporter(){
        }

        public Format3TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            this.xpp = xpp;
            this.format = 3;
            readFromXml(xpp);

        }

        protected void readFromXml(XmlPullParser xpp) throws XmlPullParserException, IOException {
            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                String tag = xpp.getName();
                if (tag == null || xpp.getEventType() == XmlPullParser.END_TAG) {
                    continue;
                }

                try {
                    parseXmlTag(tag);
                } catch (Exception e) {
                    errorCount++;
                    log.error(e.getMessage(), e);
                }
            }
        }

        protected void parseXmlTag(String tag) {
            switch (tag) {
                case BackupConstants.TASK_TAG:
                    parseTask();
                    break;
                case BackupConstants.METADATA_TAG:
                    parseMetadata();
                    break;
                case BackupConstants.TAGDATA_TAG:
                    parseTagdata();
                    break;
            }
        }

        private void parseTagdata() {
            tagdata.clear();
            deserializeModel(tagdata, TagData.PROPERTIES);
            tagDataDao.persist(tagdata);
        }
    }

    private static final String FORMAT4 = "4";

    private class Format4TaskImporter extends Format3TaskImporter {

        protected TaskTimeLog taskTimeLog = new TaskTimeLog();

        public Format4TaskImporter(XmlPullParser xpp) throws XmlPullParserException, IOException {
            this.xpp = xpp;
            this.format = 4;
            readFromXml(xpp);
        }

        @Override
        protected void parseXmlTag(String tag) {
            if (BackupConstants.TIMELOG_TAG.equals(tag)){
                parseTimeLog();
            } else {
                super.parseXmlTag(tag);
            }
        }

        protected void parseTimeLog(){
            if(!currentTask.isSaved()) {
                return;
            }
            taskTimeLog.clear();
            deserializeModel(taskTimeLog, TaskTimeLog.PROPERTIES);
            taskTimeLog.setID(TaskTimeLog.NO_ID);
            taskTimeLog.setTaskId(currentTask.getId());
            taskTimeLogDao.persist(taskTimeLog);
        }
    }

    public int parseAppVersion(XmlPullParser xpp) {
        return Integer.parseInt(xpp.getAttributeValue(null, BackupConstants.ASTRID_ATTR_VERSION));
    }
}
