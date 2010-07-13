package com.todoroo.andlib.test;

import android.test.AndroidTestCase;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Base test case for Astrid tests
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TodorooTestCase extends AndroidTestCase {

    static {
        AstridDependencyInjector.initialize();
    }

	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    ContextManager.setContext(this.getContext());
	    DependencyInjectionService.getInstance().inject(this);
	}

}
