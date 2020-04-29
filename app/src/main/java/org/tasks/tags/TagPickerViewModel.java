package org.tasks.tags;

import static com.google.common.collect.Iterables.any;
import static org.tasks.Strings.isNullOrEmpty;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
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
import org.tasks.tags.CheckBoxTriStates.State;

@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public class TagPickerViewModel extends ViewModel {

  private final MutableLiveData<List<TagData>> tags = new MutableLiveData<>();
  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject TagDataDao tagDataDao;

  private final Set<TagData> selected = new HashSet<>();
  private final Set<TagData> partiallySelected = new HashSet<>();
  private String text;

  public void observe(LifecycleOwner owner, Observer<List<TagData>> observer) {
    tags.observe(owner, observer);
  }

  public void setSelected(List<TagData> selected, @Nullable List<TagData> partiallySelected) {
    this.selected.addAll(selected);
    if (partiallySelected != null) {
      this.partiallySelected.addAll(partiallySelected);
    }
  }

  public ArrayList<TagData> getSelected() {
    return new ArrayList<>(selected);
  }

  ArrayList<TagData> getPartiallySelected() {
    return new ArrayList<>(partiallySelected);
  }

  public String getText() {
    return text;
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
    if (!isNullOrEmpty(text) && !any(results, t -> text.equalsIgnoreCase(t.getName()))) {
      results.add(0, new TagData(text));
    }
    tags.setValue(results);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  State getState(TagData tagData) {
    if (partiallySelected.contains(tagData)) {
      return State.PARTIALLY_CHECKED;
    }
    return selected.contains(tagData) ? State.CHECKED : State.UNCHECKED;
  }

  State toggle(TagData tagData, boolean checked) {
    if (tagData.getId() == null) {
      tagData = new TagData(tagData.getName());
      tagDataDao.createNew(tagData);
    }

    partiallySelected.remove(tagData);

    if (checked) {
      selected.add(tagData);
      return State.CHECKED;
    } else {
      selected.remove(tagData);
      return State.UNCHECKED;
    }
  }
}
