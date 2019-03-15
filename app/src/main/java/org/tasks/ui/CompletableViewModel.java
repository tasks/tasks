package org.tasks.ui;

import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Callable;

public abstract class CompletableViewModel<T> extends ViewModel {
  private final MutableLiveData<T> data = new MutableLiveData<>();
  private final MutableLiveData<Throwable> error = new MutableLiveData<>();
  private final CompositeDisposable disposables = new CompositeDisposable();
  private boolean inProgress;

  public void observe(
      LifecycleOwner lifecycleOwner, Observer<T> dataObserver, Observer<Throwable> errorObserver) {
    data.observe(lifecycleOwner, dataObserver);
    error.observe(lifecycleOwner, errorObserver);
  }

  public boolean inProgress() {
    return inProgress;
  }

  protected void run(Callable<T> callable) {
    assertMainThread();

    if (!inProgress) {
      inProgress = true;
      disposables.add(
          Single.fromCallable(callable)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .doFinally(
                  () -> {
                    assertMainThread();
                    inProgress = false;
                  })
              .subscribe(data::setValue, error::setValue));
    }
  }

  @Override
  protected void onCleared() {
    disposables.dispose();
  }
}
