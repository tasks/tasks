/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AstridDependencyInjectorTest {

    protected static class Helper {
        public Object getObject() {
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        // in case some state from other unit tests overwrote injector
        DependencyInjectionService.getInstance().addInjector(
                new AstridDependencyInjector()
        );
    }

    @Test
    public void testWithString() {
        Helper helper = new Helper() {
            @Autowired
            public String applicationName;

            @Override
            public Object getObject() {
                return applicationName;
            };
        };

        DependencyInjectionService.getInstance().inject(helper);
        assertTrue(((String)helper.getObject()).length() > 0);
    }

    @Test
    public void testWithClass() {

        Helper helper = new Helper() {
            @Autowired
            public TaskService taskService;

            @Override
            public Object getObject() {
                return taskService;
            };
        };

        DependencyInjectionService.getInstance().inject(helper);
        assertTrue(helper.getObject() instanceof TaskService);

        Helper helper2 = new Helper() {
            @Autowired
            public TaskService taskService;

            @Override
            public Object getObject() {
                return taskService;
            };
        };

        DependencyInjectionService.getInstance().inject(helper2);

        assertTrue(helper.getObject() == helper2.getObject());

    }
}
