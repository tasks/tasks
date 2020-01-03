package org.tasks.tags;

import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Lists.newArrayList;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import com.google.common.base.Strings;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;

public class TagPickerViewModel extends ViewModel {

  private final MutableLiveData<List<TagData>> tags = new MutableLiveData<>();
  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject TagDataDao tagDataDao;

  private String text;
  private Set<TagData> selected = new HashSet<>();

  public void observe(LifecycleOwner owner, Observer<List<TagData>> observer) {
    tags.observe(owner, observer);
  }

  public void setTags(List<TagData> tags) {
    selected.addAll(tags);
  }

  public ArrayList<TagData> getTags() {
    return newArrayList(selected);
  }

  public String getText() {
    return text;
  }

  public void clear() {
    search("");
  }

  public void search(String text) {
    if (!text.equalsIgnoreCase(this.text)) {
      disposables.add(
          Single.fromCallable(() -> tagDataDao.searchTags(text))
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(this::onUpdate));
    }
    this.text = text;
  }

  private void onUpdate(List<TagData> results) {
    if (!Strings.isNullOrEmpty(text) && !any(results, t -> text.equalsIgnoreCase(t.getName()))) {
      results.add(0, new TagData(text));
    }
    tags.setValue(results);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public boolean isChecked(TagData tagData) {
    return selected.contains(tagData);
  }

  void toggle(TagData tagData, boolean checked) {
    if (tagData.getId() == null) {
      tagData = new TagData(tagData.getName());
      tagDataDao.createNew(tagData);
    }

    if (checked) {
      selected.add(tagData);
    } else {
      selected.remove(tagData);
    }
  }
}
