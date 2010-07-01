/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.ExceptionService.AndroidLogReporter;
import com.todoroo.andlib.service.ExceptionService.ErrorReporter;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;

/**
 * Astrid application dependency injector loads classes in Astrid with the
 * appropriate instantiated objects necessary for their operation. For
 * more information on Dependency Injection, see {@link DependencyInjectionService}
 * and {@link AbstractDependencyInjector}.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AstridDependencyInjector implements AbstractDependencyInjector {

    private static final boolean DEBUG = false;

    /**
     * Boolean bit to prevent multiple copies of this injector to be loaded
     */
    private static boolean initialized = false;

    /**
     * Dependencies this class knows how to handle
     */
    private static final HashMap<String, Object> injectables = new HashMap<String, Object>();

    /**
     * Cache of classes that were instantiated by the injector
     */
    private final HashMap<Class<?>, WeakReference<Object>> createdObjects =
        new HashMap<Class<?>, WeakReference<Object>>();

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     * (i.e. {@link ErrorReporter}.
     */
    @SuppressWarnings("nls")
    private static void addInjectables() {
        injectables.put("debug", DEBUG);

        // com.todoroo.android.service
        injectables.put("applicationName", "astrid");
        injectables.put("exceptionService", ExceptionService.class);
        injectables.put("errorDialogTitleResource", R.string.DLG_error);

        // TODO
        injectables.put("errorDialogBodyGeneric", R.string.DLG_error);
        injectables.put("errorDialogBodyNullError", R.string.DLG_error);
        injectables.put("errorDialogBodySocketTimeout", R.string.DLG_error);

        // com.todoroo.android.utility
        injectables.put("dialogUtilities", DialogUtilities.class);
        injectables.put("informationDialogTitleResource", R.string.DLG_information_title);
        injectables.put("dateUtilities", DateUtilities.class);
        injectables.put("yearsResource", R.plurals.DUt_years);
        injectables.put("monthsResource", R.plurals.DUt_months);
        injectables.put("weeksResource", R.plurals.DUt_weeks);
        injectables.put("daysResource", R.plurals.DUt_days);
        injectables.put("hoursResource", R.plurals.DUt_hours);
        injectables.put("minutesResource", R.plurals.DUt_minutes);
        injectables.put("secondsResource", R.plurals.DUt_seconds);
        injectables.put("daysAbbrevResource", R.plurals.DUt_days);
        injectables.put("hoursAbbrevResource", R.plurals.DUt_hoursShort);
        injectables.put("minutesAbbrevResource", R.plurals.DUt_minutesShort);
        injectables.put("secondsAbbrevResource", R.plurals.DUt_secondsShort);

        // com.todoroo.astrid.dao
        injectables.put("database", Database.class);
        injectables.put("taskDao", TaskDao.class);
        injectables.put("metadataDao", MetadataDao.class);

        // com.todoroo.astrid.service
        injectables.put("taskService", TaskService.class);
        injectables.put("metadataService", MetadataService.class);
        injectables.put("upgradeService", UpgradeService.class);

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
     * {@inheritDoc}
     */
    public Object getInjection(Object object, Field field) {
        if(injectables.containsKey(field.getName())) {
            Object injection = injectables.get(field.getName());

            // if it's a class, instantiate the class
            if(injection instanceof Class<?>) {
                if(createdObjects.containsKey(injection) &&
                        createdObjects.get(injection).get() != null) {
                    injection = createdObjects.get(injection).get();
                } else {
                    Class<?> cls = (Class<?>)injection;
                    try {
                        injection = cls.newInstance();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    }

                    createdObjects.put(cls,
                            new WeakReference<Object>(injection));
                }
            }

            return injection;
        }

        return null;
    }

    /**
     * Install this service as the default Dependency Injector
     */
    public synchronized static void initialize() {
        if(initialized)
            return;
        initialized = true;

        AstridDependencyInjector injector = new AstridDependencyInjector();
        DependencyInjectionService.getInstance().setInjectors(new AbstractDependencyInjector[] {
                injector
        });

        addInjectables();
    }

    AstridDependencyInjector() {
        // prevent instantiation
    }

}
