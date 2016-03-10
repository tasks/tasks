/**
 * TODO: make this lightweight, don't extend the entire TaskListActivity
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskService;

import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

import static org.tasks.intents.TaskIntents.getEditTaskStack;

/**
 * @author joshuagross
 *
 * Create a new task based on incoming links from the "share" menu
 */
public final class ShareLinkActivity extends InjectingAppCompatActivity {

    @Inject StartupService startupService;
    @Inject TaskService taskService;
    @Inject TaskCreator taskCreator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startupService.onStartupApplication(this);

        readIntent();
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        readIntent();
    }

    private void readIntent() {
        Intent intent = getIntent();
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (subject == null) {
            subject = "";
        }

        Task task = taskCreator.basicQuickAddTask(subject);
        if (task != null) {
            task.setNotes(intent.getStringExtra(Intent.EXTRA_TEXT));
            taskService.save(task);
            getEditTaskStack(this, null, task.getId()).startActivities();
        }
        finish();
    }
}
