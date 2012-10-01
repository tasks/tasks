/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.AstridPreferences;

public class TasksXmlExporter {

    // --- public interface

    /**
     * Import tasks from the given file
     *
     * @param context context
     * @param exportType from service, manual, or on upgrade
     * @param runAfterExport runnable to run after exporting
     * @param backupDirectoryOverride new backupdirectory, or null to use default
     */
    public static void exportTasks(Context context, ExportType exportType,
            Runnable runAfterExport, File backupDirectoryOverride, String versionName) {
        new TasksXmlExporter(context, exportType, runAfterExport,
                backupDirectoryOverride, versionName);
    }

    public static enum ExportType {
        EXPORT_TYPE_SERVICE,
        EXPORT_TYPE_MANUAL,
        EXPORT_TYPE_ON_UPGRADE
    }

    // --- implementation

    private static final int FORMAT = 2;

    private final Context context;
    private int exportCount = 0;
    private XmlSerializer xml;
    private final Database database = PluginServices.getDatabase();
    private final TaskService taskService = PluginServices.getTaskService();
    private final MetadataService metadataService = PluginServices.getMetadataService();
    private final ExceptionService exceptionService = PluginServices.getExceptionService();

    private final ProgressDialog progressDialog;
    private final Handler handler;
    private final File backupDirectory;
    private final String latestSetVersionName;

    private void setProgress(final int taskNumber, final int total) {
        handler.post(new Runnable() {
            public void run() {
                progressDialog.setMax(total);
                progressDialog.setProgress(taskNumber);
            }
        });
    }

    private TasksXmlExporter(final Context context, final ExportType exportType,
            final Runnable runAfterExport, File backupDirectoryOverride, String versionName) {
        this.context = context;
        this.exportCount = 0;
        this.backupDirectory = backupDirectoryOverride == null ?
                BackupConstants.defaultExportDirectory() : backupDirectoryOverride;
        this.latestSetVersionName = versionName;

        handler = new Handler();
        progressDialog = new ProgressDialog(context);
        if(exportType == ExportType.EXPORT_TYPE_MANUAL) {
            progressDialog.setIcon(android.R.drawable.ic_dialog_info);
            progressDialog.setTitle(R.string.export_progress_title);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
            if(context instanceof Activity)
                progressDialog.setOwnerActivity((Activity)context);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String output = setupFile(backupDirectory,
                            exportType);
                    int tasks = taskService.countTasks();

                    if(tasks > 0)
                        doTasksExport(output);

                    Preferences.setLong(BackupPreferences.PREF_BACKUP_LAST_DATE,
                            DateUtilities.now());
                    Preferences.setString(BackupPreferences.PREF_BACKUP_LAST_ERROR, null);

                    if (exportType == ExportType.EXPORT_TYPE_MANUAL)
                        onFinishExport(output);
                } catch (IOException e) {
                    switch(exportType) {
                    case EXPORT_TYPE_MANUAL:
                        exceptionService.displayAndReportError(context,
                            context.getString(R.string.backup_TXI_error), e);
                        break;
                    case EXPORT_TYPE_SERVICE:
                        exceptionService.reportError("background-backup", e); //$NON-NLS-1$
                        Preferences.setString(BackupPreferences.PREF_BACKUP_LAST_ERROR, e.toString());
                        break;
                    case EXPORT_TYPE_ON_UPGRADE:
                        exceptionService.reportError("background-backup", e); //$NON-NLS-1$
                        Preferences.setString(BackupPreferences.PREF_BACKUP_LAST_ERROR, e.toString());
                        break;
                    }
                } finally {
                    if(runAfterExport != null)
                        runAfterExport.run();
                }
            }
        }).start();
    }


    @SuppressWarnings("nls")
    private void doTasksExport(String output) throws IOException {
        File xmlFile = new File(output);
        xmlFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(xmlFile);
        xml = Xml.newSerializer();
        xml.setOutput(fos, BackupConstants.XML_ENCODING);

        xml.startDocument(null, null);
        xml.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

        xml.startTag(null, BackupConstants.ASTRID_TAG);
        xml.attribute(null, BackupConstants.ASTRID_ATTR_VERSION,
                Integer.toString(AstridPreferences.getCurrentVersion()));
        xml.attribute(null, BackupConstants.ASTRID_ATTR_FORMAT,
                Integer.toString(FORMAT));

        serializeTasks();

        xml.endTag(null, BackupConstants.ASTRID_TAG);
        xml.endDocument();
        xml.flush();
        fos.close();
    }

    private void serializeTasks() throws IOException {
        TodorooCursor<Task> cursor;
        cursor = taskService.query(Query.select(
                Task.PROPERTIES).orderBy(Order.asc(Task.ID)));
        try {
            Task task = new Task();
            int length = cursor.getCount();
            for(int i = 0; i < length; i++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);

                setProgress(i, length);

                xml.startTag(null, BackupConstants.TASK_TAG);
                serializeModel(task, Task.PROPERTIES, Task.ID);
                serializeMetadata(task);
                xml.endTag(null, BackupConstants.TASK_TAG);
                this.exportCount++;
            }
        } finally {
            cursor.close();
        }
    }

    private synchronized void serializeMetadata(Task task) throws IOException {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            Metadata metadata = new Metadata();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);

                xml.startTag(null, BackupConstants.METADATA_TAG);
                serializeModel(metadata, Metadata.PROPERTIES, Metadata.ID, Metadata.TASK);
                xml.endTag(null, BackupConstants.METADATA_TAG);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Turn a model into xml attributes
     * @param model
     */
    private void serializeModel(AbstractModel model, Property<?>[] properties, Property<?>... excludes) {
        outer: for(Property<?> property : properties) {
            for(Property<?> exclude : excludes)
                if(property.name.equals(exclude.name))
                    continue outer;

            try {
                property.accept(xmlWritingVisitor, model);
            } catch (Exception e) {
                Log.e("astrid-exporter", //$NON-NLS-1$
                        "Caught exception while reading " + property.name + //$NON-NLS-1$
                        " from " + model.getDatabaseValues(), e); //$NON-NLS-1$
            }
        }
    }

    private final XmlWritingPropertyVisitor xmlWritingVisitor = new XmlWritingPropertyVisitor();
    public static final String XML_NULL = "null"; //$NON-NLS-1$

    public class XmlWritingPropertyVisitor implements PropertyVisitor<Void, AbstractModel> {

        @Override
        public Void visitInteger(Property<Integer> property, AbstractModel data) {
            try {
                Integer value = data.getValue(property);
                String valueString = (value == null) ? XML_NULL : value.toString();
                xml.attribute(null, property.name, valueString);
            } catch (UnsupportedOperationException e) {
                // didn't read this value, do nothing
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, AbstractModel data) {
            try {
                Long value = data.getValue(property);
                String valueString = (value == null) ? XML_NULL : value.toString();
                xml.attribute(null, property.name, valueString);
            } catch (UnsupportedOperationException e) {
                // didn't read this value, do nothing
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public Void visitDouble(Property<Double> property, AbstractModel data) {
            try {
                Double value = data.getValue(property);
                String valueString = (value == null) ? XML_NULL : value.toString();
                xml.attribute(null, property.name, valueString);
            } catch (UnsupportedOperationException e) {
                // didn't read this value, do nothing
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, AbstractModel data) {
            try {
                String value = data.getValue(property);
                if(value == null)
                    return null;
                xml.attribute(null, property.name, value);
            } catch (UnsupportedOperationException e) {
                // didn't read this value, do nothing
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private void onFinishExport(final String outputFile) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if(exportCount == 0)
                    Toast.makeText(context, context.getString(R.string.export_toast_no_tasks), Toast.LENGTH_LONG).show();
                else {
                    CharSequence text = String.format(context.getString(R.string.export_toast),
                            context.getResources().getQuantityString(R.plurals.Ntasks, exportCount,
                                    exportCount), outputFile);
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    if(progressDialog.isShowing() && context instanceof Activity)
                        DialogUtilities.dismissDialog((Activity) context, progressDialog);
                }
            }
        });
    }

    /**
     * Creates directories if necessary and returns fully qualified file
     * @param directory
     * @return output file name
     * @throws IOException
     */
    private String setupFile(File directory, ExportType exportType) throws IOException {
        File astridDir = directory;
        if (astridDir != null) {
            // Check for /sdcard/astrid directory. If it doesn't exist, make it.
            if (astridDir.exists() || astridDir.mkdir()) {
                String fileName = ""; //$NON-NLS-1$
                switch(exportType) {
                case EXPORT_TYPE_SERVICE:
                    fileName = String.format(BackupConstants.BACKUP_FILE_NAME, BackupDateUtilities.getDateForExport());
                    break;
                case EXPORT_TYPE_MANUAL:
                    fileName = String.format(BackupConstants.EXPORT_FILE_NAME, BackupDateUtilities.getDateForExport());
                    break;
                case EXPORT_TYPE_ON_UPGRADE:
                    fileName = String.format(BackupConstants.UPGRADE_FILE_NAME, latestSetVersionName);
                    break;
                default:
                     throw new IllegalArgumentException("Invalid export type"); //$NON-NLS-1$
                }
                return astridDir.getAbsolutePath() + File.separator + fileName;
            } else {
                // Unable to make the /sdcard/astrid directory.
                throw new IOException(context.getString(R.string.DLG_error_sdcard,
                        astridDir.getAbsolutePath()));
            }
        } else {
            // Unable to access the sdcard because it's not in the mounted state.
            throw new IOException(context.getString(R.string.DLG_error_sdcard_general));
        }
    }

}
