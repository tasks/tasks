package com.timsu.astrid.utilities;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForXml;

public class TasksXmlImporter {
    public static final String TAG = "TasksXmlImporter";
    public static final String ASTRID_TAG = TasksXmlExporter.ASTRID_TAG;
    public static final String ASTRID_ATTR_VERSION = TasksXmlExporter.ASTRID_ATTR_VERSION;
    public static final String TASK_TAG = TasksXmlExporter.TASK_TAG;
    public static final String TAG_TAG = TasksXmlExporter.TAG_TAG;
    public static final String ALERT_TAG = TasksXmlExporter.ALERT_TAG;
    public static final String SYNC_TAG = TasksXmlExporter.SYNC_TAG;
    public static final String TASK_ID = AbstractController.KEY_ROWID;
    public static final String TASK_NAME = TaskModelForXml.NAME;
    public static final String TASK_CREATION_DATE = TaskModelForXml.CREATION_DATE;
    public static final String TAG_ATTR_NAME = TasksXmlExporter.TAG_ATTR_NAME;
    public static final String ALERT_ATTR_DATE = TasksXmlExporter.ALERT_ATTR_DATE;
    public static final String SYNC_ATTR_SERVICE = TasksXmlExporter.SYNC_ATTR_SERVICE;
    public static final String SYNC_ATTR_REMOTE_ID = TasksXmlExporter.SYNC_ATTR_REMOTE_ID;

    private TaskController taskController;
    private TagController tagController;
    private AlertController alertController;
    private SyncDataController syncDataController;
    private XmlPullParser xpp;
    private String input;
    private Handler importHandler;
    private final Context context;
    private int taskCount;
    private int importCount;
    private int skipCount;

    static ProgressDialog progressDialog;

    public TasksXmlImporter(Context context) {
        this.context = context;
        setContext(context);
    }

    private void setProgressMessage(final String message) {
        importHandler.post(new Runnable() {
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }

    public void importTasks(final Runnable runAfterImport) {
        importHandler = new Handler();
        importHandler.post(new Runnable() {
            @Override
            public void run() {
                TasksXmlImporter.progressDialog = new ProgressDialog(context);
                progressDialog.setIcon(android.R.drawable.ic_dialog_info);
                progressDialog.setTitle(R.string.import_progress_title);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage(context.getString(R.string.import_progress_open));
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    performImport();
                    if (runAfterImport != null) {
                        importHandler.post(runAfterImport);
                    }
                } catch (FileNotFoundException e) {
                    Log.e("TasksXmlImporter", e.getMessage());
                } catch (XmlPullParserException e) {
                    Log.e("TasksXmlImporter", e.getMessage());
                }
                Looper.loop();
            }
        }).start();
    }

    private void performImport() throws FileNotFoundException, XmlPullParserException {
        taskCount = 0;
        importCount = 0;
        skipCount = 0;
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        xpp = factory.newPullParser();
        xpp.setInput(new FileReader(input));
        setProgressMessage(context.getString(R.string.import_progress_opened));

        openControllers();
        try {
            TaskModelForXml currentTask = null;
            while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                String tag = xpp.getName();
                if (xpp.getEventType() == XmlPullParser.END_TAG) {
                    // Ignore end tag.
                    continue;
                }
                if (tag != null) {
                    if (tag.equals(ASTRID_TAG)) {
                        // Process <astrid ... >
                        // Perform version compatibility check?
                    }
                    else if (tag.equals(TASK_TAG)) {
                        // Parse <task ... >
                        currentTask = parseTask();
                    } else if (currentTask != null) {
                        // These tags all require that we have a task to associate them with.
                        if (tag.equals(TAG_TAG)) {
                            // Process <tag ... >
                            parseTag(currentTask.getTaskIdentifier());
                        } else if (tag.equals(ALERT_TAG)) {
                            // Process <alert ... >
                            parseAlert(currentTask.getTaskIdentifier());
                        } else if (tag.equals(SYNC_TAG)) {
                            // Process <sync ... >
                            parseSync(currentTask.getTaskIdentifier());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "import error " + e.getMessage());
        } finally {
            closeControllers();
            progressDialog.dismiss();
            showSummary();
        }
    }

   private boolean parseSync(TaskIdentifier taskId) {
       String service = xpp.getAttributeValue(null, SYNC_ATTR_SERVICE);
       String remoteId = xpp.getAttributeValue(null, SYNC_ATTR_REMOTE_ID);
       if (service != null && remoteId != null) {
           int serviceInt = Integer.parseInt(service);
           SyncMapping sm = new SyncMapping(taskId, serviceInt, remoteId);
           syncDataController.saveSyncMapping(sm);
           return true;
       }
       return false;
    } 

    private boolean parseAlert(TaskIdentifier taskId) {
       String alert = xpp.getAttributeValue(null, ALERT_ATTR_DATE);
       if (alert != null) {
           Date alertDate = DateUtilities.getDateFromIso8601String(alert);
           if (alertDate != null) {
               if (! alertController.addAlert(taskId, alertDate)) {
                   return false;
               }
           } else {
               return false;
           }
       } else {
           return false;
       }
       return true;
   }

    private boolean parseTag(TaskIdentifier taskId) {
        String tagName = xpp.getAttributeValue(null, TAG_ATTR_NAME);
        if (tagName != null) {
            TagIdentifier tagId;
            TagModelForView tagModel;
            tagModel = tagController.fetchTagFromName(tagName);
            if (tagModel == null) {
                // Tag not found, create a new one.
                tagId = tagController.createTag(tagName);
            } else {
                tagId = tagModel.getTagIdentifier();
            }
            if (! tagController.addTag(taskId, tagId)) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    private TaskModelForXml parseTask() {
        taskCount++;
        setProgressMessage(context.getString(R.string.import_progress_read, taskCount));
        TaskModelForXml task = null;
        String taskName = xpp.getAttributeValue(null, TASK_NAME);

        Date creationDate = null;
        String createdString = xpp.getAttributeValue(null, TASK_CREATION_DATE);
        if (createdString != null) {
            creationDate = DateUtilities.getDateFromIso8601String(createdString);
        }
        // If the task's name and creation date match an existing task, skip it.
        if (creationDate != null && taskName != null) {
            task = taskController.fetchTaskForXml(taskName, creationDate);
        }
        if (task != null) {
            // Skip this task.
            skipCount++;
            setProgressMessage(context.getString(R.string.import_progress_skip, taskCount));
            // Set currentTask to null so we skip its alerts/syncs/tags, too.
            return null;
        }
        // Else, make a new task model and add away.
        task = new TaskModelForXml();
        int numAttributes = xpp.getAttributeCount();
        for (int i = 0; i < numAttributes; i++) {
            String fieldName = xpp.getAttributeName(i);
            String fieldValue = xpp.getAttributeValue(i);
            task.setField(fieldName, fieldValue);
        }
        // Save the task to the database.
        taskController.saveTask(task, false);
        importCount++;
        setProgressMessage(context.getString(R.string.import_progress_add, taskCount));
        return task;
    }

    private void showSummary() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.import_summary_title);
        String message = context.getString(R.string.import_summary_message,
                input, taskCount, importCount, skipCount);
        builder.setMessage(message);
        builder.setPositiveButton(context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
        });
        builder.show();
    }

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
        setTaskController(new TaskController(ctx));
        setTagController(new TagController(ctx));
        setAlertController(new AlertController(ctx));
        setSyncDataController(new SyncDataController(ctx));
    }

    private void closeControllers() {
        taskController.close();
        tagController.close();
        alertController.close();
        syncDataController.close();
    }

    private void openControllers() {
        taskController.open();
        tagController.open();
        alertController.open();
        syncDataController.open();
    }

    public void setInput(String input) {
        this.input = input;
    }
}
