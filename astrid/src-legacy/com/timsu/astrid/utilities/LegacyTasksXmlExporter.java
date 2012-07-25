/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForXml;
import com.todoroo.astrid.backup.BackupDateUtilities;

@SuppressWarnings({"nls", "deprecation"})
public class LegacyTasksXmlExporter {

    private TaskController taskController;
    private TagController tagController;
    private AlertController alertController;
    private SyncDataController syncDataController;
    private Context ctx;
    private String output;
    private final boolean isService;
    private int exportCount;
    private XmlSerializer xml;
    private HashMap<TagIdentifier, TagModelForView> tagMap;

    public static final String ASTRID_TAG = "astrid";
    public static final String ASTRID_ATTR_VERSION = "version";
    public static final String TASK_TAG = "task";
    public static final String TAG_TAG = "tag";
    public static final String TAG_ATTR_NAME = "name";
    public static final String ALERT_TAG = "alert";
    public static final String ALERT_ATTR_DATE = "date";
    public static final String SYNC_TAG = "sync";
    public static final String SYNC_ATTR_SERVICE = "service";
    public static final String SYNC_ATTR_REMOTE_ID = "remote_id";
    public static final String XML_ENCODING = "utf-8";
    public static final String ASTRID_DIR = "/astrid";
    private static final String EXPORT_FILE_NAME = "user.%s.xml";
    private static final String BACKUP_FILE_NAME = "auto.%s.xml";
    public static final int FILENAME_DATE_BEGIN_INDEX = 5;
    public static final int FILENAME_DATE_END_INDEX = 11;

    /** last version before 3.0, used for XML header */
    private static final int LEGACY_VERSION = 137;

    public LegacyTasksXmlExporter(boolean isService) {
        this.isService = isService;
        this.exportCount = 0;
    }

    private void initTagMap() {
        tagMap = tagController.getAllTagsAsMap();
    }

    private void serializeTags(TaskIdentifier task)
            throws IOException {
        LinkedList<TagIdentifier> tags = tagController.getTaskTags(task);
        for (TagIdentifier tag : tags) {
            if(!tagMap.containsKey(tag) || tagMap.get(tag) == null)
                continue;
            xml.startTag(null, TAG_TAG);
            xml.attribute(null, TAG_ATTR_NAME, tagMap.get(tag).toString());
            xml.endTag(null, TAG_TAG);
        }
    }

    private void serializeSyncMappings(TaskIdentifier task)
            throws IOException {
        HashSet<SyncMapping> syncMappings = syncDataController.getSyncMappings(task);
        for (SyncMapping sync : syncMappings) {
            xml.startTag(null, SYNC_TAG);
            xml.attribute(null, SYNC_ATTR_SERVICE,
                    Integer.toString(sync.getSyncServiceId()));
            xml.attribute(null, SYNC_ATTR_REMOTE_ID, sync.getRemoteId());
            xml.endTag(null, SYNC_TAG);
        }
    }

    private void serializeAlerts(TaskIdentifier task)
            throws IOException {
        List<Date> alerts = alertController.getTaskAlerts(task);
        for (Date alert : alerts) {
            xml.startTag(null, ALERT_TAG);
            xml.attribute(null, ALERT_ATTR_DATE, BackupDateUtilities.getIso8601String(alert));
            xml.endTag(null, ALERT_TAG);
        }
    }

    private void serializeTasks()
            throws IOException {
        Cursor c = taskController.getBackupTaskListCursor();
        if (! c.moveToFirst()) {
            return; // No tasks.
        }
        do {
            TaskModelForXml task = new TaskModelForXml(c);
            TaskIdentifier taskId = task.getTaskIdentifier();
            xml.startTag(null, TASK_TAG);
            HashMap<String, String> taskAttributes = task.getTaskAttributes();
            for (String key : taskAttributes.keySet()) {
                String value = taskAttributes.get(key);
                xml.attribute(null, key, value);
            }
            serializeTags(taskId);
            serializeAlerts(taskId);
            serializeSyncMappings(taskId);
            xml.endTag(null, TASK_TAG);
            this.exportCount++;
        } while (c.moveToNext());
        c.close();
    }

    private void doTasksExport() throws IOException {
        File xmlFile = new File(this.output);
        xmlFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(xmlFile);
        xml = Xml.newSerializer();
        xml.setOutput(fos, XML_ENCODING);

        xml.startDocument(null, null);
        xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

        xml.startTag(null, ASTRID_TAG);
        xml.attribute(null, ASTRID_ATTR_VERSION,
                Integer.toString(LEGACY_VERSION));

        openControllers();
        initTagMap();
        serializeTasks();
        closeControllers();

        xml.endTag(null, ASTRID_TAG);
        xml.endDocument();
        xml.flush();
        fos.close();

        if (!isService) {
            displayToast();
        }
    }

    private void displayToast() {
        // no toast in legacy exporter
    }

    private void displayErrorToast(String error) {
        Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
    }

    private void closeControllers() {
        tagController.close();
        taskController.close();
        alertController.close();
        syncDataController.close();
    }

    private void openControllers() {
        taskController.open();
        tagController.open();
        alertController.open();
        syncDataController.open();
    }

    public String exportTasks(File directory) {
        if (setupFile(directory)) {
            try {
                doTasksExport();
            } catch (IOException e) {
                return null;
            }
            return output;
        }
        return null;
    }

    public static File getExportDirectory() {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            path = path + ASTRID_DIR;
            return new File(path);
        }
        return null;
    }

    private boolean setupFile(File directory) {
        File astridDir = directory;
        if (astridDir != null) {
            // Check for /sdcard/astrid directory. If it doesn't exist, make it.
            if (astridDir.exists() || astridDir.mkdir()) {
                String fileName;
                if (isService) {
                    fileName = BACKUP_FILE_NAME;
                } else {
                    fileName = EXPORT_FILE_NAME;
                }
                fileName = String.format(fileName, BackupDateUtilities.getDateForExport());
                setOutput(astridDir.getAbsolutePath() + "/" + fileName);
                return true;
            } else {
                // Unable to make the /sdcard/astrid directory.
                String error = ctx.getString(R.string.DLG_error_sdcard, astridDir.getAbsolutePath());
                Log.e("TasksXmlExporter", error);
                if (!isService) {
                    displayErrorToast(error);
                }
                return false;
            }
        } else {
            // Unable to access the sdcard because it's not in the mounted state.
            String error = ctx.getString(R.string.DLG_error_sdcard_general);
            Log.e("TasksXmlExporter", error);
            if (!isService) {
                displayErrorToast(error);
            }
            return false;
        }
    }

    private void setOutput(String file) {
       this.output = file;
    }

//    private final Runnable doBackgroundExport = new Runnable() {
//        public void run() {
//            /*Looper.prepare();
//            try {
//                doTasksExport();
//            } catch (IOException e) {
//                Log.e("TasksXmlExporter", "IOException in doTasksExport " + e.getMessage());
//            }
//            Looper.loop();*/
//        }
//    };

    public void setTaskController(TaskController taskController) {
        this.taskController = taskController;
    }

    public void setTagController(TagController tagController) {
        this.tagController = tagController;
    }

    public void setAlertController(AlertController alertController) {
        this.alertController = alertController;
    }

    public void setSyncDataController(SyncDataController syncDataController) {
        this.syncDataController = syncDataController;
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
        setTaskController(new TaskController(ctx));
        setTagController(new TagController(ctx));
        setAlertController(new AlertController(ctx));
        setSyncDataController(new SyncDataController(ctx));
    }
}
