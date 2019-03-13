package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

public class ActionViewModel extends ViewModel {
  private MutableLiveData<Boolean> completed = new MutableLiveData<>();
  private MutableLiveData<Throwable> error = new MutableLiveData<>();
  private boolean inProgress;

  public LiveData<Boolean> getData() {
    return completed;
  }

  public LiveData<Throwable> getError() {
    return error;
  }

  public boolean inProgress() {
    return inProgress;
  }

  protected void run(Action action) {
    assertMainThread();

    if (!inProgress) {
      inProgress = true;
      Completable.fromAction(action)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnComplete(() -> completed.setValue(true))
          .doOnError(error::setValue)
          .doFinally(() -> {
            assertMainThread();
            inProgress = false;
          })
          .subscribe();
    }
  }
}
