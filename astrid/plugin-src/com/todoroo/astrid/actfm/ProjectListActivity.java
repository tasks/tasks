/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.TagDataAdapter;
import com.todoroo.astrid.dao.TagDataDao.TagDataCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;

/**
 * Activity that displays a user's task lists and allows users
 * to filter their task list.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataListActivity extends ListActivity implements OnItemClickListener {

    // --- constants

    private static final int REQUEST_LOG_IN = 1;
    private static final int REQUEST_SHOW_GOAL = 2;

    private static final int MENU_REFRESH_ID = Menu.FIRST + 0;

    // --- instance variables

    @Autowired ExceptionService exceptionService;
    @Autowired TagDataService tagDataService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired ActFmSyncService actFmSyncService;

    protected TagDataAdapter adapter = null;
    protected AtomicReference<String> queryTemplate = new AtomicReference<String>();

    /* ======================================================================
     * ======================================================= initialization
     * ====================================================================== */

    static {
        AstridDependencyInjector.initialize();
    }

    /**  Called when loading up the activity */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DependencyInjectionService.getInstance().inject(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tagData_list_activity);
        ThemeService.applyTheme(this);

        if(!actFmPreferenceService.isLoggedIn()) {
            Intent login = new Intent(this, ActFmLoginActivity.class);
            login.putExtra(ActFmLoginActivity.EXTRA_DO_NOT_SYNC, true);
            startActivityForResult(login, REQUEST_LOG_IN);
        }

        initializeUIComponents();
        setUpList();
        refreshList(false);
    }

    @SuppressWarnings("nls")
    private void initializeUIComponents() {
        ((ImageButton) findViewById(R.id.extendedAddButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtilities.okDialog(TagDataListActivity.this, "unsupported", null);
            }
        });

        ((ImageButton) findViewById(R.id.extendedAddButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtilities.okDialog(TagDataListActivity.this, "unsupported", null);
            }
        });

        ((ImageView) findViewById(R.id.goals)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TagDataListActivity.this, TaskListActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }

    /**
     * Create options menu (displayed when user presses menu key)
     *
     * @return true if menu should be displayed
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(menu.size() > 0)
            return true;

        MenuItem item;

        item = menu.add(Menu.NONE, MENU_REFRESH_ID, Menu.NONE,
                R.string.PLA_menu_refresh);
        item.setIcon(R.drawable.ic_menu_refresh);

        return true;
    }

    /* ======================================================================
     * ============================================================ lifecycle
     * ====================================================================== */

    @Override
    protected void onStart() {
        super.onStart();
        StatisticsService.sessionStart(this);
        StatisticsService.reportEvent("goal-list"); //$NON-NLS-1$
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_LOG_IN) {
            if(resultCode == RESULT_CANCELED)
                finish();
            else
                refreshList(true);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    /* ======================================================================
     * ====================================================== populating list
     * ====================================================================== */

    /** Sets up the coach list adapter */
    protected void setUpList() {
        queryTemplate.set(new QueryTemplate().where(TagDataCriteria.isTeam()).toString());
        TodorooCursor<TagData> currentCursor = tagDataService.fetchFiltered(queryTemplate.get(),
                null, TagData.PROPERTIES);
        startManagingCursor(currentCursor);

        adapter = new TagDataAdapter(this, R.layout.tagData_adapter_row,
                currentCursor, queryTemplate, false, null);
        setListAdapter(adapter);

        getListView().setOnItemClickListener(this);
    }

    /** refresh the list with latest data from the web */
    private void refreshList(boolean manual) {
        actFmSyncService.fetchTagDataDashboard(manual, new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = adapter.getCursor();
                        cursor.requery();
                        startManagingCursor(cursor);
                    }
                });
            }
        });
    }

    /* ======================================================================
     * ============================================================== actions
     * ====================================================================== */

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent intent = new Intent(this, TagDataViewActivity.class);
        intent.putExtra(TagDataViewActivity.EXTRA_PROJECT_ID, id);
        startActivityForResult(intent, REQUEST_SHOW_GOAL);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {

        // handle my own menus
        switch (item.getItemId()) {
        case MENU_REFRESH_ID: {
            refreshList(true);
            return true;
        }
        }

        return false;
    }
}
