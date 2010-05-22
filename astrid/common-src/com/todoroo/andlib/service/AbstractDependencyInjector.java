package com.todoroo.andlib.service;

import java.lang.reflect.Field;

/**
 * A Dependency Injector knows how to inject certain dependencies based
 * on the field that is passed in.
 * 
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface AbstractDependencyInjector {
    
    /**
     * Gets the injected object for this field. If implementing class does not
     * know how to handle this dependency, it should return null
     * 
     * @param object
     *            object to perform dependency injection on
     * @param field
     *            field tagged with {link Autowired} annotation
     * @return object to assign to this field, or null
     */
    abstract Object getInjection(Object object, Field field);

}
