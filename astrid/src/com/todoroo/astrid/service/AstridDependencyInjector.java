/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService.AndroidLogReporter;
import com.todoroo.andlib.service.ExceptionService.ErrorReporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.utility.Constants;

/**
 * Astrid application dependency injector loads classes in Astrid with the
 * appropriate instantiated objects necessary for their operation. For
 * more information on Dependency Injection, see {@link DependencyInjectionService}
 * and {@link AbstractDependencyInjector}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AstridDependencyInjector extends AbstractDependencyInjector {

    /**
     * Boolean bit to prevent multiple copies of this injector to be loaded
     */
    private static AstridDependencyInjector instance = null;

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     * (i.e. {@link ErrorReporter}.
     */
    @Override
    @SuppressWarnings("nls")
    protected void addInjectables() {
        injectables.put("debug", Constants.DEBUG);

        // com.todoroo.android.service
        injectables.put("applicationName", "astrid");

        // com.todoroo.android.utility
        injectables.put("nNumberPickerLayout", R.layout.n_number_picker_dialog);
        injectables.put("numberPickerLayout", R.layout.number_picker);
        injectables.put("numberPickerIncrementId", R.id.increment);
        injectables.put("numberPickerDecrementId", R.id.decrement);
        injectables.put("numberPickerId", R.id.numberPicker);
        injectables.put("numberPickerInputId", R.id.timepicker_input);
        injectables.put("numberPickerDialogLayout", R.layout.number_picker_dialog);

        // com.todoroo.astrid.dao
        injectables.put("database", Database.class);
        injectables.put("taskDao", TaskDao.class);
        injectables.put("metadataDao", MetadataDao.class);
        injectables.put("storeObjectDao", StoreObjectDao.class);

        // com.todoroo.astrid.service
        injectables.put("taskService", TaskService.class);
        injectables.put("metadataService", MetadataService.class);
        injectables.put("upgradeService", UpgradeService.class);
        injectables.put("addOnService", AddOnService.class);

        // com.timsu.astrid.data
        injectables.put("tasksTable", "tasks");
        injectables.put("tagsTable", "tags");
        injectables.put("tagTaskTable", "tagTaskMap");
        injectables.put("alertsTable", "alerts");
        injectables.put("syncTable", "sync");

        // these make reference to fields defined above
        injectables.put("errorReporters", new ErrorReporter[] {
                new AndroidLogReporter(),
                new FlurryReporter()
        });
    }

    /**
     * Install this service as the default Dependency Injector
     */
    public static void initialize() {
        if(instance != null)
            return;
        synchronized(AstridDependencyInjector.class) {
            if(instance == null)
                instance = new AstridDependencyInjector();
            DependencyInjectionService.getInstance().setInjectors(new AbstractDependencyInjector[] {
                    instance
            });
        }
    }

    AstridDependencyInjector() {
        // prevent instantiation
        super();
    }

    /**
     * Flush dependency injection cache. Useful for unit tests.
     */
    public synchronized static void flush() {
        instance.flushCreated();
    }
}
