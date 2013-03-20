/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.timsu.astrid.R;
import com.todoroo.aacenc.RecognizerApi.RecognizerApiListener;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.CommentsActivity;
import com.todoroo.astrid.actfm.CommentsFragment;
import com.todoroo.astrid.actfm.TagCommentsFragment;
import com.todoroo.astrid.actfm.TaskCommentsFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.IntentFilter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SearchFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.subtasks.SubtasksHelper;
import com.todoroo.astrid.ui.DateChangedAlerts;
import com.todoroo.astrid.ui.QuickAddBar;
import com.todoroo.astrid.voice.VoiceRecognizer;

/**
 * This wrapper activity contains all the glue-code to handle the callbacks between the different
 * fragments that could be visible on the screen in landscape-mode.
 * So, it basically contains all the callback-code from the filterlist-fragment, the tasklist-fragments
 * and the taskedit-fragment (and possibly others that should be handled).
 * Using this AstridWrapperActivity helps to avoid duplicated code because its all gathered here for sub-wrapperactivities
 * to use.
 *
 * @author Arne
 *
 */
public class AstridActivity extends SherlockFragmentActivity
    implements FilterListFragment.OnFilterItemClickedListener,
    TaskListFragment.OnTaskListItemClickedListener,
    RecognizerApiListener {

    public static final int LAYOUT_SINGLE = 0;
    public static final int LAYOUT_DOUBLE = 1;
    public static final int LAYOUT_TRIPLE = 2;

    public static final int RESULT_RESTART_ACTIVITY = 50;

    protected int fragmentLayout = LAYOUT_SINGLE;

    private final RepeatConfirmationReceiver repeatConfirmationReceiver = new RepeatConfirmationReceiver();

    @Autowired
    private TaskDao taskDao;

    public FilterListFragment getFilterListFragment() {
        FilterListFragment frag = (FilterListFragment) getSupportFragmentManager()
                .findFragmentByTag(FilterListFragment.TAG_FILTERLIST_FRAGMENT);

        return frag;
    }

    public TaskListFragment getTaskListFragment() {
        TaskListFragment frag = (TaskListFragment) getSupportFragmentManager()
                .findFragmentByTag(TaskListFragment.TAG_TASKLIST_FRAGMENT);

        return frag;
    }

    public TaskEditFragment getTaskEditFragment() {
        TaskEditFragment frag = (TaskEditFragment) getSupportFragmentManager()
                .findFragmentByTag(TaskEditFragment.TAG_TASKEDIT_FRAGMENT);

        return frag;
    }

    public CommentsFragment getTagUpdatesFragment() {
        CommentsFragment frag = (CommentsFragment) getSupportFragmentManager()
                .findFragmentByTag(CommentsFragment.TAG_UPDATES_FRAGMENT);

        return frag;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DependencyInjectionService.getInstance().inject(this);
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);
        StatisticsService.sessionStart(this);

        new StartupService().onStartupApplication(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        android.content.IntentFilter repeatFilter = new android.content.IntentFilter(
                AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED);
        repeatFilter.addAction(AstridApiConstants.BROADCAST_EVENT_TASK_REPEAT_FINISHED);
        registerReceiver(repeatConfirmationReceiver, repeatFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        StatisticsService.sessionPause();
        AndroidUtilities.tryUnregisterReceiver(this, repeatConfirmationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    /**
     * Handles items being clicked from the filterlist-fragment. Return true if item is handled.
     */
    public boolean onFilterItemClicked(FilterListItem item) {
        if (this instanceof TaskListActivity && (item instanceof Filter) ) {
            ((TaskListActivity) this).setSelectedItem((Filter) item);
        }
        if (item instanceof SearchFilter) {
            onSearchRequested();
            StatisticsService.reportEvent(StatisticsConstants.FILTER_SEARCH);
            return false;
        } else {
            // If showing both fragments, directly update the tasklist-fragment
            Intent intent = getIntent();

            if(item instanceof Filter) {
                Filter filter = (Filter)item;

                Bundle extras = configureIntentAndExtrasWithFilter(intent, filter);
                if (fragmentLayout == LAYOUT_TRIPLE && getTaskEditFragment() != null) {
                    onBackPressed(); // remove the task edit fragment when switching between lists
                }
                setupTasklistFragmentWithFilter(filter, extras);

                // no animation for dualpane-layout
                AndroidUtilities.callOverridePendingTransition(this, 0, 0);
                StatisticsService.reportEvent(StatisticsConstants.FILTER_LIST);
                return true;
            } else if(item instanceof IntentFilter) {
                try {
                    ((IntentFilter)item).intent.send();
                } catch (CanceledException e) {
                    // ignore
                }
            }
            return false;
        }
    }

    protected Bundle configureIntentAndExtrasWithFilter(Intent intent, Filter filter) {
        if(filter instanceof FilterWithCustomIntent) {
            int lastSelectedList = intent.getIntExtra(FilterListFragment.TOKEN_LAST_SELECTED, 0);
            intent = ((FilterWithCustomIntent)filter).getCustomIntent();
            intent.putExtra(FilterListFragment.TOKEN_LAST_SELECTED, lastSelectedList);
        } else {
            intent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        }

        setIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras != null)
            extras = (Bundle) extras.clone();
        return extras;
    }

    public void setupActivityFragment(TagData tagData) {
        if (fragmentLayout == LAYOUT_SINGLE)
            return;

        if (fragmentLayout == LAYOUT_TRIPLE)
            findViewById(R.id.taskedit_fragment_container).setVisibility(View.VISIBLE);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        TagCommentsFragment updates = new TagCommentsFragment(tagData);
        transaction.replace(R.id.taskedit_fragment_container, updates, CommentsFragment.TAG_UPDATES_FRAGMENT);
        transaction.commit();
    }

    public void setupTasklistFragmentWithFilter(Filter filter, Bundle extras) {
        Class<?> customTaskList = null;

        if (SubtasksHelper.shouldUseSubtasksFragmentForFilter(filter))
            customTaskList = SubtasksHelper.subtasksClassForFilter(filter);

        setupTasklistFragmentWithFilterAndCustomTaskList(filter, extras, customTaskList);
    }

    public void setupTasklistFragmentWithFilterAndCustomTaskList(Filter filter, Bundle extras, Class<?> customTaskList) {
        TaskListFragment newFragment = TaskListFragment.instantiateWithFilterAndExtras(filter, extras, customTaskList);

        try {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.tasklist_fragment_container, newFragment,
                    TaskListFragment.TAG_TASKLIST_FRAGMENT);
            transaction.commit();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getSupportFragmentManager().executePendingTransactions();
                }
            });
        } catch (Exception e) {
            // Don't worry about it
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskListItemClicked(long taskId) {
        Task task = taskDao.fetch(taskId, Task.IS_READONLY, Task.IS_PUBLIC, Task.USER_ID);
        if (task != null)
            onTaskListItemClicked(taskId, task.isEditable());
    }

    public void onTaskListItemClicked(String uuid) {
        Task task = taskDao.fetch(uuid, Task.ID, Task.IS_READONLY, Task.IS_PUBLIC, Task.USER_ID);
        if (task != null)
            onTaskListItemClicked(task.getId(), task.isEditable());
    }

    @Override
    public void onTaskListItemClicked(long taskId, boolean editable) {
        if (editable) {
            editTask(taskId);
        } else {
            showComments(taskId);
        }
    }

    private void editTask(long taskId) {
        Intent intent = new Intent(this, TaskEditActivity.class);
        intent.putExtra(TaskEditFragment.TOKEN_ID, taskId);
        getIntent().putExtra(TaskEditFragment.TOKEN_ID, taskId); // Needs to be in activity intent so that TEA onResume doesn't create a blank activity
        if (getIntent().hasExtra(TaskListFragment.TOKEN_FILTER))
            intent.putExtra(TaskListFragment.TOKEN_FILTER, getIntent().getParcelableExtra(TaskListFragment.TOKEN_FILTER));

        if (fragmentLayout != LAYOUT_SINGLE) {
            TaskEditFragment editActivity = getTaskEditFragment();
            findViewById(R.id.taskedit_fragment_container).setVisibility(View.VISIBLE);

            if(editActivity == null) {
                editActivity = new TaskEditFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.taskedit_fragment_container, editActivity, TaskEditFragment.TAG_TASKEDIT_FRAGMENT);
                transaction.addToBackStack(null);
                transaction.commit();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Force the transaction to occur so that we can be guaranteed of the fragment existing if we try to present it
                        getSupportFragmentManager().executePendingTransactions();
                    }
                });
            } else {
                editActivity.save(true);
                editActivity.repopulateFromScratch(intent);
            }

            TaskListFragment tlf = getTaskListFragment();
            if (tlf != null)
                tlf.loadTaskListContent(true);

        } else {
            startActivityForResult(intent, TaskListFragment.ACTIVITY_EDIT_TASK);
            AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
        }
    }

    private void showComments(long taskId) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(TaskCommentsFragment.EXTRA_TASK, taskId);
        startActivity(intent);
        AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_left_in, R.anim.slide_left_out);
    }

    @Override
    public void onBackPressed() {
        if (isFinishing())
            return;
        super.onBackPressed();
    }

    // --- fragment helpers

    protected void removeFragment(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if(fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commit();
        }
    }

    protected Fragment setupFragment(String tag, int container, Class<? extends Fragment> cls, boolean createImmediate, boolean replace) {
        final FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        if(fragment == null || replace) {
            Fragment oldFragment = fragment;
            try {
                fragment = cls.newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (container == 0) {
                if (oldFragment != null && replace)
                    ft.remove(oldFragment);
                ft.add(fragment, tag);
            }
            else
                ft.replace(container, fragment, tag);
            ft.commit();
            if (createImmediate)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fm.executePendingTransactions();
                    }
                });
        }
        return fragment;
    }

    // Voice recognizer callbacks
    @Override
    public void onSpeechResult(String result) {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null) {
            EditText box = tlf.quickAddBar.getQuickAddBox();
            if (box != null)
                box.setText(result);
        }

    }

    @Override
    public void onSpeechError(int error) {
        TaskListFragment tlf = getTaskListFragment();
        if (tlf != null) {
            QuickAddBar quickAdd = tlf.quickAddBar;
            if (quickAdd != null) {
                VoiceRecognizer vr = quickAdd.getVoiceRecognizer();
                if (vr != null)
                    vr.cancel();
            }
        }

        int errorStr = 0;
        switch(error) {
        case SpeechRecognizer.ERROR_NETWORK:
        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            errorStr = R.string.speech_err_network;
            break;
        case SpeechRecognizer.ERROR_NO_MATCH:
            Toast.makeText(this, R.string.speech_err_no_match, Toast.LENGTH_LONG).show();
            break;
        default:
            errorStr = R.string.speech_err_default;
            break;
        }

        if (errorStr > 0)
            DialogUtilities.okDialog(this, getString(errorStr), null);
    }

    public void switchToActiveTasks() {
        onFilterItemClicked(CoreFilterExposer.buildInboxFilter(getResources()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_RESTART_ACTIVITY) {
            finish();
            startActivity(getIntent());
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * @return LAYOUT_SINGLE, LAYOUT_DOUBLE, or LAYOUT_TRIPLE
     */
    public int getFragmentLayout() {
        return fragmentLayout;
    }

    private class RepeatConfirmationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            long taskId = intent.getLongExtra(
                    AstridApiConstants.EXTRAS_TASK_ID, 0);

            if (taskId > 0) {
                long oldDueDate = intent.getLongExtra(
                        AstridApiConstants.EXTRAS_OLD_DUE_DATE, 0);
                long newDueDate = intent.getLongExtra(
                        AstridApiConstants.EXTRAS_NEW_DUE_DATE, 0);
                Task task = PluginServices.getTaskService().fetchById(taskId,
                        DateChangedAlerts.REPEAT_RESCHEDULED_PROPERTIES);

                try {
                    boolean lastTime = AstridApiConstants.BROADCAST_EVENT_TASK_REPEAT_FINISHED.equals(intent.getAction());
                    DateChangedAlerts.showRepeatTaskRescheduledDialog(
                            AstridActivity.this, task, oldDueDate, newDueDate, lastTime);

                } catch (BadTokenException e) { // Activity not running when tried to show dialog--rebroadcast
                    new Thread() {
                        @Override
                        public void run() {
                            sendBroadcast(intent);
                        }
                    }.start();
                }
            }
        }
    }

}
