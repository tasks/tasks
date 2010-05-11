package com.todoroo.astrid.service;

import android.test.AndroidTestCase;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DialogUtilities;

public class AstridDependencyInjectorTests extends AndroidTestCase {

    protected static class Helper {
        public Object getObject() {
            return null;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // in case some state from other unit tests overwrote injector
        DependencyInjectionService.getInstance().setInjectors(new AbstractDependencyInjector[] {
                new AstridDependencyInjector()
        });
    }

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

    public void testWithInteger() {

        Helper helper = new Helper() {
            @Autowired
            public Integer informationDialogTitleResource;

            @Override
            public Object getObject() {
                return informationDialogTitleResource;
            };
        };

        DependencyInjectionService.getInstance().inject(helper);
        assertEquals(R.string.DLG_information_title, helper.getObject());
    }

    public void testWithClass() {

        Helper helper = new Helper() {
            @Autowired
            public DialogUtilities dialogUtilities;

            @Override
            public Object getObject() {
                return dialogUtilities;
            };
        };

        DependencyInjectionService.getInstance().inject(helper);
        assertTrue(helper.getObject() instanceof DialogUtilities);

        Helper helper2 = new Helper() {
            @Autowired
            public DialogUtilities dialogUtilities;

            @Override
            public Object getObject() {
                return dialogUtilities;
            };
        };

        DependencyInjectionService.getInstance().inject(helper2);

        assertEquals(helper.getObject(), helper2.getObject());

    }
}
