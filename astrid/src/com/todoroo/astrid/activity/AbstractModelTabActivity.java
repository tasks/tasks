package com.todoroo.astrid.activity;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.utility.Constants;

abstract public class AbstractModelTabActivity<TYPE> extends TabActivity {

    // from AstridActivity

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    // from AbstractModelActivity

    // --- bundle arguments

    /**
     * Action Item ID
     */
    public static final String ID_TOKEN = "i"; //$NON-NLS-1$

    // --- instance variables

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected Database database;

    protected TYPE model = null;

    // --- abstract methods

    abstract protected TYPE fetchModel(long id);

    /**
     * Load Bente Dependency Injector
     */
    static {
        AstridDependencyInjector.initialize();
    }

    public AbstractModelTabActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);
        loadItem(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        loadItem(intent);
    }

    /**
     * Loads action item from the given intent
     * @param intent
     */
    protected void loadItem(Intent intent) {
        long idParam = intent.getLongExtra(ID_TOKEN, -1L);
        if(idParam == -1) {
            exceptionService.reportError("AMA-no-token", null); //$NON-NLS-1$
            finish();
            return;
        }

        database.openForReading();
        model = fetchModel(idParam);

        if(model == null) {
            exceptionService.reportError("AMA-no-task", new NullPointerException("model")); //$NON-NLS-1$ //$NON-NLS-2$
            finish();
            return;
        }
    }
}
