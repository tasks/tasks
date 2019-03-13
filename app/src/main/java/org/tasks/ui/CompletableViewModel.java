package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Callable;

public abstract class CompletableViewModel<T> extends ViewModel {
  private MutableLiveData<T> data = new MutableLiveData<>();
  private MutableLiveData<Throwable> error = new MutableLiveData<>();
  private boolean inProgress;

  public LiveData<T> getData() {
    return data;
  }

  public LiveData<Throwable> getError() {
    return error;
  }

  public boolean inProgress() {
    return inProgress;
  }

  protected void run(Callable<T> callable) {
    assertMainThread();

    if (!inProgress) {
      inProgress = true;
      Single.fromCallable(callable)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .doOnSuccess(data::setValue)
          .doOnError(error::setValue)
          .doFinally(() -> {
            assertMainThread();
            inProgress = false;
          })
          .subscribe();
    }
  }
}
