package com.todoroo.andlib.service;

public class RobolectricTestDependencyInjector extends AbstractDependencyInjector {

    private String name;

    public RobolectricTestDependencyInjector(String name) {
        this.name = name;
    }

    public void addInjectable(String field, Object injection) {
        injectables.put(field, injection);
    }

    @Override
    public String toString() {
        return "TestDI:" + name;
    }

    // --- static stuff

    /**
     * Install TestDependencyInjector above other injectors
     */
    public synchronized static RobolectricTestDependencyInjector initialize(String name) {
        RobolectricTestDependencyInjector instance = new RobolectricTestDependencyInjector(name);
        DependencyInjectionService.getInstance().addInjector(instance);
        return instance;
    }

    /**
     * Remove an installed TestDependencyInjector
     */
    public static void deinitialize(RobolectricTestDependencyInjector instance) {
        DependencyInjectionService.getInstance().removeInjector(instance);
    }

}
