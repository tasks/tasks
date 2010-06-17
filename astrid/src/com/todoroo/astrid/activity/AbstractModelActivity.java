/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * This activity displays a <code>WebView</code> that allows users to log in to the
 * synchronization provider requested. A callback method determines whether
 * their login was successful and therefore whether to dismiss the dialog.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class AbstractModelActivity<TYPE extends AbstractModel> extends AstridActivity {

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

    public AbstractModelActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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