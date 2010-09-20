/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk;

import org.weloveastrid.rmilk.data.MilkListService;
import org.weloveastrid.rmilk.data.MilkMetadataService;

import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService.ErrorReporter;

/**
 * RTM Dependency Injection for service classes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkDependencyInjector extends AbstractDependencyInjector {

    /**
     * variable to prevent multiple copies of this injector to be loaded
     */
    private static MilkDependencyInjector instance = null;

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     * (i.e. {@link ErrorReporter}.
     */
    @Override
    @SuppressWarnings("nls")
    protected void addInjectables() {
        injectables.put("milkMetadataService", MilkMetadataService.class);
        injectables.put("milkListService", MilkListService.class);
    }

    /**
     * Install this dependency injector
     */
    public static void initialize() {
        if(instance != null)
            return;
        synchronized(MilkDependencyInjector.class) {
            if(instance == null)
                instance = new MilkDependencyInjector();
            DependencyInjectionService.getInstance().addInjector(instance);
        }
    }

    MilkDependencyInjector() {
        // prevent instantiation
        super();
    }

}
