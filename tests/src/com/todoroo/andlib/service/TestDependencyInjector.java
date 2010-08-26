package com.todoroo.andlib.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

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
        deinitialize(name);

        ArrayList<AbstractDependencyInjector> list =
            new ArrayList<AbstractDependencyInjector>(Arrays.asList(DependencyInjectionService.getInstance().getInjectors()));

        TestDependencyInjector instance = new TestDependencyInjector(name);
        list.add(0, instance);
        DependencyInjectionService.getInstance().setInjectors(list.toArray(new AbstractDependencyInjector[list.size()]));
        return instance;
    }

    /**
     * Remove an installed TestDependencyInjector
     * @param string
     */
    public static void deinitialize(String name) {
        ArrayList<AbstractDependencyInjector> list =
            new ArrayList<AbstractDependencyInjector>(Arrays.asList(DependencyInjectionService.getInstance().getInjectors()));

        for(Iterator<AbstractDependencyInjector> i = list.iterator(); i.hasNext(); ) {
            AbstractDependencyInjector injector = i.next();

            // if another one of these injectors already exists in the
            // stack, remove it
            if(injector instanceof TestDependencyInjector) {
                if(((TestDependencyInjector)injector).name.equals(name))
                    i.remove();
            }
        }
    }

}
