package com.todoroo.astrid.activity;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.service.TaskCreator;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;

public class TaskEditActivity extends InjectingAppCompatActivity {

  private static final String TOKEN_ID = "id";

  @Inject TaskCreator taskCreator;
  @Inject TaskDao taskDao;
  private CompositeDisposable disposables;

  @Override
  protected void onResume() {
    super.onResume();

    long taskId = getIntent().getLongExtra(TOKEN_ID, 0);

    disposables = new CompositeDisposable();

    if (taskId == 0) {
      startActivity(TaskIntents.getEditTaskIntent(this, taskCreator.createWithValues("")));
      finish();
    } else {
      disposables.add(
          Single.fromCallable(() -> taskDao.fetch(taskId))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  task -> {
                    startActivity(TaskIntents.getEditTaskIntent(this, task));
                    finish();
                  }));
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    disposables.dispose();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
