/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONObject;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.SynchronizationProvider;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.producteev.ProducteevPreferences;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.rmilk.MilkUtilities;
import com.todoroo.astrid.rmilk.api.ServiceInternalException;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.utility.Preferences;

public class ProducteevSyncProvider extends SynchronizationProvider<ProducteevTaskContainer> {

    private MilkDataService dataService = null;
    private ProducteevInvoker invoker = null;
    private int defaultDashboard;
    private final ProducteevPreferences preferences = new ProducteevPreferences();

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected DialogUtilities dialogUtilities;

    public ProducteevSyncProvider() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- public methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of RTM, deleting all synchronization metadata
     */
    public void signOut() {
        preferences.setToken(null);
        preferences.clearLastSyncDate();

        dataService.clearMetadata(); // TODO clear metadata
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- authentication
    // ----------------------------------------------------------------------

    /**
     * Deal with a synchronization exception. If requested, will show an error
     * to the user (unless synchronization is happening in background)
     *
     * @param context
     * @param tag
     *            error tag
     * @param e
     *            exception
     * @param showError
     *            whether to display a dialog
     */
    @Override
    protected void handleException(String tag, Exception e, boolean showError) {
        preferences.setLastError(e.toString());

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

        // occurs when network error
        } else if(e instanceof ServiceInternalException &&
                ((ServiceInternalException)e).getEnclosedException() instanceof
                IOException) {
            Exception enclosedException = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-ioexception", enclosedException); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, enclosedException,
                        context.getString(R.string.rmilk_ioerror));
            }
        } else {
            if(e instanceof ServiceInternalException)
                e = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, e, null);
            }
        }
    }

    @Override
    protected void initiate(Context context) {
        dataService = MilkDataService.getInstance();

        // authenticate the user. this will automatically call the next step
        // authenticate(context);

        Toast.makeText(context, "hi! this is empty", Toast.LENGTH_LONG);
    }

    /**
     * Perform authentication with RTM. Will open the SyncBrowser if necessary
     */
    @SuppressWarnings("nls")
    private void authenticate(final Context context) {
        final Resources r = context.getResources();
        FlurryAgent.onEvent("producteev-started");

        preferences.recordSyncStart();

        try {
            String authToken = preferences.getToken();

            String z = stripslashes(0, "71o3346pr40o5o4nt4n7t6n287t4op28","2");
            String v = stripslashes(2, "9641n76n9s1736q1578q1o1337q19233","4ae");
            invoker = new ProducteevInvoker(z, v);

            // check if we have a token & it works
            if(authToken != null) {
                invoker.setToken(authToken);
            }

            if(authToken == null) {
                String email = Preferences.getStringValue(R.string.producteev_PPr_email);
                String password = Preferences.getStringValue(R.string.producteev_PPr_password);

                invoker.authenticate(email, password);
            }

            performSync();
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-authenticate", e, true);
        } finally {
            preferences.stopOngoing();
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    @SuppressWarnings("nls")
    protected void performSync() {
        try {
            // load user information
            JSONObject user = invoker.usersView(null);
            defaultDashboard = user.getJSONObject("user").getInt("default_dashboard");

            // read all tasks
            JSONObject tasks = invoker.tasksShowList(defaultDashboard,
                    null); // TODO

            SyncData<ProducteevTaskContainer> syncData = populateSyncData(tasks);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            MilkUtilities.recordSuccessfulSync();

            FlurryAgent.onEvent("rtm-sync-finished"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-sync", e, true); //$NON-NLS-1$
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.IMPORTANCE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.NOTES,
    };

    /**
     * Populate SyncData data structure
     */
    private SyncData<ProducteevTaskContainer> populateSyncData(JSONObject tasks) {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(PROPERTIES);

        // return new SyncData<ProducteevTaskContainer>(tasks, localCreated, localUpdated);
        return null;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------



    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper classes
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==116?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }

    @Override
    protected void updateNotification(Context context, Notification n) {
    }

    @Override
    protected void create(ProducteevTaskContainer task) throws IOException {
    }

    @Override
    protected void push(ProducteevTaskContainer task,
            ProducteevTaskContainer remote) throws IOException {
    }

    @Override
    protected ProducteevTaskContainer pull(ProducteevTaskContainer task)
            throws IOException {
        return null;
    }

    @Override
    protected ProducteevTaskContainer read(TodorooCursor<Task> task)
            throws IOException {
        return null;
    }

    @Override
    protected void write(ProducteevTaskContainer task) throws IOException {
    }

    @Override
    protected int matchTask(ArrayList<ProducteevTaskContainer> tasks,
            ProducteevTaskContainer target) {
        return 0;
    }

    @Override
    protected void transferIdentifiers(ProducteevTaskContainer source,
            ProducteevTaskContainer destination) {
    }

}
