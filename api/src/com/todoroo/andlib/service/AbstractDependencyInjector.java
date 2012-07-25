/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

import com.todoroo.andlib.service.ExceptionService.ErrorReporter;

/**
 * A Dependency Injector knows how to inject certain dependencies based
 * on the field that is passed in. You will need to write your own initialization
 * code to insert this dependency injector into the DI chain.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class AbstractDependencyInjector {

    /**
     * Initialize list of injection variables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     * (i.e. {@link ErrorReporter}.
     */
    protected void addInjectables() {
        // your injectables here
    }

    // ---

    /**
     * Constructor
     */
    protected AbstractDependencyInjector() {
        addInjectables();
    }

    /**
     * Dependencies this class knows how to handle
     */
    protected final HashMap<String, Object> injectables = new HashMap<String, Object>();

    /**
     * Cache of classes that were instantiated by the injector
     */
    protected final HashMap<Class<?>, WeakReference<Object>> createdObjects =
        new HashMap<Class<?>, WeakReference<Object>>();

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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Flush dependency injection cache. Useful for unit tests.
     */
    protected void flushCreated() {
        createdObjects.clear();
    }

}
