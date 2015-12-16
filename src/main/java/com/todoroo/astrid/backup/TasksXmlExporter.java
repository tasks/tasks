/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.util.Xml;
import android.widget.Toast;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.date.DateTimeUtils.newDateTime;

public class TasksXmlExporter {

    public static final String PREF_BACKUP_LAST_DATE = "backupDate"; //$NON-NLS-1$

    // --- public interface

    public enum ExportType {
        EXPORT_TYPE_SERVICE,
        EXPORT_TYPE_MANUAL,
        EXPORT_TYPE_ON_UPGRADE
    }

    // --- implementation

    private final TagDataDao tagDataDao;
    private final MetadataDao metadataDao;
    private final TaskService taskService;
    private final Preferences preferences;

    // 3 is started on Version 4.6.10
    private static final int FORMAT = 3;
    private Context context;
    private int exportCount = 0;
    private XmlSerializer xml;

    private ProgressDialog progressDialog;
    private Handler handler;
    private File backupDirectory;
    private String latestSetVersionName;

    private void post(Runnable runnable) {
        if (handler != null) {
            handler.post(runnable);
        }
    }

    private void setProgress(final int taskNumber, final int total) {
        post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMax(total);
                progressDialog.setProgress(taskNumber);
            }
        });
    }

    @Inject
    public TasksXmlExporter(TagDataDao tagDataDao, MetadataDao metadataDao, TaskService taskService,
                            Preferences preferences) {
        this.tagDataDao = tagDataDao;
        this.metadataDao = metadataDao;
        this.taskService = taskService;
        this.preferences = preferences;
    }

    public void exportTasks(final Context context, final ExportType exportType, DialogBuilder dialogBuilder) {
        this.context = context;
        this.exportCount = 0;
        this.backupDirectory = preferences.getBackupDirectory();
        this.latestSetVersionName = null;

        handler = exportType == ExportType.EXPORT_TYPE_MANUAL ? new Handler() : null;
        if(exportType == ExportType.EXPORT_TYPE_MANUAL) {
            progressDialog = dialogBuilder.newProgressDialog();
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
            if(context instanceof Activity) {
                progressDialog.setOwnerActivity((Activity) context);
            }
        } else {
            progressDialog = new ProgressDialog(context);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String output = setupFile(backupDirectory,
                            exportType);
                    int tasks = taskService.countTasks();

                    if(tasks > 0) {
                        doTasksExport(output);
                    }

                    preferences.setLong(PREF_BACKUP_LAST_DATE, DateUtilities.now());

                    if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
                        onFinishExport(output);
                    }
                } catch (IOException e) {
                    Timber.e(e, e.getMessage());
                } finally {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if(progressDialog.isShowing() && context instanceof Activity) {
                                DialogUtilities.dismissDialog((Activity) context, progressDialog);
                                ((Activity) context).finish();
                            }
                        }
                    });
                }
            }
        }).start();
    }


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
                Integer.toString(preferences.getLastSetVersion()));
        xml.attribute(null, BackupConstants.ASTRID_ATTR_FORMAT,
                Integer.toString(FORMAT));

        serializeTasks();
        serializeTagDatas();

        xml.endTag(null, BackupConstants.ASTRID_TAG);
        xml.endDocument();
        xml.flush();
        fos.close();
    }

    private void serializeTagDatas() {
        tagDataDao.allTags(new Callback<TagData>() {
            @Override
            public void apply(TagData tag) {
                try {
                    xml.startTag(null, BackupConstants.TAGDATA_TAG);
                    serializeModel(tag, TagData.PROPERTIES, TagData.ID);
                    xml.endTag(null, BackupConstants.TAGDATA_TAG);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void serializeTasks() throws IOException {
        TodorooCursor<Task> cursor;
        cursor = taskService.query(Query.select(
                Task.PROPERTIES).orderBy(Order.asc(Task.ID)));
        try {
            int length = cursor.getCount();
            for(int i = 0; i < length; i++) {
                cursor.moveToNext();
                Task task = new Task(cursor);

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

    private synchronized void serializeMetadata(Task task) {
        metadataDao.byTask(task.getId(), new Callback<Metadata>() {
            @Override
            public void apply(Metadata metadata) {
                try {
                    xml.startTag(null, BackupConstants.METADATA_TAG);
                    serializeModel(metadata, Metadata.PROPERTIES, Metadata.ID, Metadata.TASK);
                    xml.endTag(null, BackupConstants.METADATA_TAG);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Turn a model into xml attributes
     */
    private void serializeModel(AbstractModel model, Property<?>[] properties, Property<?>... excludes) {
        outer: for(Property<?> property : properties) {
            for(Property<?> exclude : excludes) {
                if (property.name.equals(exclude.name)) {
                    continue outer;
                }
            }

            try {
                property.accept(xmlWritingVisitor, model);
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
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
                Timber.e(e, e.getMessage());
            } catch (IllegalArgumentException | IOException | IllegalStateException e) {
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
                Timber.e(e, e.getMessage());
            } catch (IllegalArgumentException | IOException | IllegalStateException e) {
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
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public Void visitString(Property<String> property, AbstractModel data) {
            try {
                String value = data.getValue(property);
                if(value == null) {
                    return null;
                }
                xml.attribute(null, property.name, value);
            } catch (UnsupportedOperationException e) {
                // didn't read this value, do nothing
                Timber.v(e, e.getMessage());
            } catch (IllegalArgumentException | IOException | IllegalStateException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private void onFinishExport(final String outputFile) {
        post(new Runnable() {
            @Override
            public void run() {
                if(exportCount == 0) {
                    Toast.makeText(context, context.getString(R.string.export_toast_no_tasks), Toast.LENGTH_LONG).show();
                } else {
                    CharSequence text = String.format(context.getString(R.string.export_toast),
                            context.getResources().getQuantityString(R.plurals.Ntasks, exportCount,
                                    exportCount), outputFile);
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Creates directories if necessary and returns fully qualified file
     * @return output file name
     * @throws IOException
     */
    private String setupFile(File directory, ExportType exportType) throws IOException {
        if (directory != null) {
            // Check for /sdcard/astrid directory. If it doesn't exist, make it.
            if (directory.exists() || directory.mkdir()) {
                String fileName;
                switch(exportType) {
                case EXPORT_TYPE_SERVICE:
                    fileName = String.format(BackupConstants.BACKUP_FILE_NAME, getDateForExport());
                    break;
                case EXPORT_TYPE_MANUAL:
                    fileName = String.format(BackupConstants.EXPORT_FILE_NAME, getDateForExport());
                    break;
                case EXPORT_TYPE_ON_UPGRADE:
                    fileName = String.format(BackupConstants.UPGRADE_FILE_NAME, latestSetVersionName);
                    break;
                default:
                     throw new IllegalArgumentException("Invalid export type"); //$NON-NLS-1$
                }
                return directory.getAbsolutePath() + File.separator + fileName;
            } else {
                // Unable to make the /sdcard/astrid directory.
                throw new IOException(context.getString(R.string.DLG_error_sdcard,
                        directory.getAbsolutePath()));
            }
        } else {
            // Unable to access the sdcard because it's not in the mounted state.
            throw new IOException(context.getString(R.string.DLG_error_sdcard_general));
        }
    }

    private static String getDateForExport() {
        return newDateTime().toString("yyMMdd-HHmm");
    }
}
