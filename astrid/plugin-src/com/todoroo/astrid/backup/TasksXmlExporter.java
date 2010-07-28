package com.todoroo.astrid.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
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
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Preferences;

public class TasksXmlExporter {

    // --- public interface

    /**
     * Import tasks from the given file
     *
     * @param input
     * @param runAfterImport
     */
    public static void exportTasks(Context context, boolean isService) {
        new TasksXmlExporter(context, isService);
    }

    // --- implementation

    private static final int FORMAT = 2;

    private final Context context;
    private final boolean isService;
    private int exportCount;
    private XmlSerializer xml;
    private final TaskService taskService = PluginServices.getTaskService();
    private final MetadataService metadataService = PluginServices.getMetadataService();
    private final ExceptionService exceptionService = PluginServices.getExceptionService();

    private TasksXmlExporter(Context context, boolean isService) {
        this.context = context;
        this.isService = isService;
        this.exportCount = 0;

        try {
            String output = setupFile(BackupConstants.getExportDirectory());
            doTasksExport(output);
        } catch (Exception e) {
            if(!isService)
                displayErrorToast(e);
            exceptionService.reportError("backup-exception", e); //$NON-NLS-1$
            // TODO record last backup error
        }
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
                Integer.toString(Preferences.getCurrentVersion()));
        xml.attribute(null, BackupConstants.ASTRID_ATTR_FORMAT,
                Integer.toString(FORMAT));

        serializeTasks();

        xml.endTag(null, BackupConstants.ASTRID_TAG);
        xml.endDocument();
        xml.flush();
        fos.close();

        if (!isService) {
            displayToast(output);
        }
    }

    private void serializeTasks() throws IOException {
        TodorooCursor<Task> cursor = taskService.query(Query.select(
                Task.PROPERTIES).orderBy(Order.asc(Task.ID)));
        try {
            Task task = new Task();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                task.readFromCursor(cursor);

                xml.startTag(null, BackupConstants.TASK_TAG);
                serializeModel(task, Task.PROPERTIES);
                serializeMetadata(task);
                xml.endTag(null, BackupConstants.TASK_TAG);
                this.exportCount++;
            }
        } finally {
            cursor.close();
        }
    }

    private void serializeMetadata(Task task) throws IOException {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            Metadata metadata = new Metadata();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);

                xml.startTag(null, BackupConstants.TAG_TAG);
                serializeModel(metadata, Metadata.PROPERTIES);
                xml.endTag(null, BackupConstants.TAG_TAG);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Turn a model into xml attributes
     * @param model
     */
    private void serializeModel(AbstractModel model, Property<?>[] properties) {
        for(Property<?> property : properties) {
            try {
                Log.e("read", "reading " + property.name + " from " + model.getDatabaseValues());
                property.accept(xmlWritingVisitor, model);
            } catch (Exception e) {
                Log.e("caught", "caught while reading " + property.name + " from " + model.getDatabaseValues(), e);
            }
        }
    }

    private final XmlWritingPropertyVisitor xmlWritingVisitor = new XmlWritingPropertyVisitor();

    private class XmlWritingPropertyVisitor implements PropertyVisitor<Void, AbstractModel> {
        @Override
        public Void visitInteger(Property<Integer> property, AbstractModel data) {
            try {
                xml.attribute(null, property.name, data.getValue(property).toString());
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
                xml.attribute(null, property.name, data.getValue(property).toString());
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
                xml.attribute(null, property.name, data.getValue(property).toString());
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

    private void displayToast(String output) {
        CharSequence text = String.format(context.getString(R.string.export_toast),
                context.getResources().getQuantityString(R.plurals.Ntasks, exportCount,
                exportCount), output);
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private void displayErrorToast(Exception error) {
        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
    }

    /**
     * Creates directories if necessary and returns fully qualified file
     * @param directory
     * @return output file name
     * @throws IOException
     */
    private String setupFile(File directory) throws IOException {
        File astridDir = directory;
        if (astridDir != null) {
            // Check for /sdcard/astrid directory. If it doesn't exist, make it.
            if (astridDir.exists() || astridDir.mkdir()) {
                String fileName;
                if (isService) {
                    fileName = BackupConstants.BACKUP_FILE_NAME;
                } else {
                    fileName = BackupConstants.EXPORT_FILE_NAME;
                }
                fileName = String.format(fileName, BackupDateUtilities.getDateForExport());
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
