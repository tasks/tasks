/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import java.lang.reflect.Field;

import android.test.AndroidTestCase;

public class DependencyInjectionTests extends AndroidTestCase {

    public void testNoAutowire() {
        DependencyInjectionService service = new DependencyInjectionService();

        Object test = new Object();
        service.inject(test);
    }

    public void testSimpleStringInjectionAutowire() {
        DependencyInjectionService service = new DependencyInjectionService();
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        if(field.getName().equals("foo"))
                            return "bar";
                        return null;
                    }
                }
        );

        // test various permissions
        Object test = new Object() {
            @Autowired
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertEquals("bar", test.toString());

        test = new Object() {
            @Autowired
            String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertEquals("bar", test.toString());

        test = new Object() {
            @Autowired
            protected String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertEquals("bar", test.toString());

        test = new Object() {
            @Autowired
            private String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertEquals("bar", test.toString());

        // test no annotation
        test = new Object() {
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertNull( test.toString());
    }

    public void testHierarchicalStringInjectionAutowire() {
        DependencyInjectionService service = new DependencyInjectionService();
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        return "malarkey";
                    }
                }
        );
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        if(field.getName().equals("foo"))
                            return "bar";
                        return null;
                    }
                });

        Object test = new Object() {
            @Autowired
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test);
        assertEquals("bar", test.toString());

        test = new Object() {
            @Autowired
            public String forks;

            @Override
            public String toString() {
                return forks;
            }
        };
        service.inject(test);
        assertEquals("malarkey", test.toString());
    }

    public void testMissingInjection() {
        DependencyInjectionService service = new DependencyInjectionService();
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        if(field.getName().equals("wozzle"))
                            return "bar";
                        return null;
                    }
                }
        );

        Object test = new Object() {
            @Autowired
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };

        try {
            service.inject(test);
            fail("could inject with missing injector");
        } catch (RuntimeException e) {
            // expected
        }

        assertNull(test.toString());
    }

    public void testMultipleInjection() {
        DependencyInjectionService service = new DependencyInjectionService();
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        if(field.getName().equals("foo"))
                            return "bar";
                        return null;
                    }
                }
        );

        Object test1 = new Object() {
            @Autowired
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        Object test2 = new Object() {
            @Autowired
            public String foo;

            @Override
            public String toString() {
                return foo;
            }
        };
        service.inject(test1);
        service.inject(test2);
        assertEquals("bar", test1.toString());
        assertEquals("bar", test2.toString());
    }

    public static class ParentInjectee {
        @Autowired
        protected String foo;
    }

    public static class ChildInjectee extends ParentInjectee {
        @Autowired
        protected String bar;
    }

    public void testInheritedInjection() {
        DependencyInjectionService service = new DependencyInjectionService();
        service.addInjector(
                new AbstractDependencyInjector() {

                    @Override
                    public Object getInjection(Object object, Field field) {
                        if(field.getName().equals("foo"))
                            return "gotfoo";
                        else if(field.getName().equals("bar"))
                            return "hasbar";
                        return null;
                    }
                }
        );

        ChildInjectee child = new ChildInjectee();
        service.inject(child);
        assertEquals("gotfoo", child.foo);
        assertEquals("hasbar", child.bar);
    }
}
