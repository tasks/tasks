/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import android.content.Context;

import java.lang.reflect.Field;
import java.util.HashMap;

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
     */
    protected void addInjectables(Context context) {
        // your injectables here
    }

    // ---

    /**
     * Constructor
     */
    protected AbstractDependencyInjector(Context context) {
        addInjectables(context);
    }

    /**
     * Dependencies this class knows how to handle
     */
    protected final HashMap<String, Object> injectables = new HashMap<>();

    /**
     * Gets the injected object for this field. If implementing class does not
     * know how to handle this dependency, it should return null
     *
     * @param field
     *            field tagged with {link Autowired} annotation
     * @return object to assign to this field, or null
     */
    public Object getInjection(Field field) {
        return injectables.get(field.getName());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
