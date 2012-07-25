/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;


public class TestDependencyInjector extends AbstractDependencyInjector {

    private String name;

    public TestDependencyInjector(String name) {
        this.name = name;
    }

    public void addInjectable(String field, Object injection) {
        injectables.put(field, injection);
    }

    @Override
    protected void addInjectables() {
        // do nothing, we populate injectables via the addInjectable method
    }

    @Override
    public String toString() {
        return "TestDI:" + name;
    }

    // --- static stuff

    /**
     * Install TestDependencyInjector above other injectors
     */
    public synchronized static TestDependencyInjector initialize(String name) {
        TestDependencyInjector instance = new TestDependencyInjector(name);
        DependencyInjectionService.getInstance().addInjector(instance);
        return instance;
    }

    /**
     * Remove an installed TestDependencyInjector
     * @param string
     */
    public static void deinitialize(TestDependencyInjector instance) {
        DependencyInjectionService.getInstance().removeInjector(instance);
    }

}
