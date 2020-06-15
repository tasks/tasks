package org.tasks.tags

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.tags.CheckBoxTriStates.State
import java.util.*
import javax.inject.Inject

class TagPickerViewModel : ViewModel() {
    private val tags = MutableLiveData<List<TagData>>()
    private val disposables = CompositeDisposable()

    @Inject lateinit var tagDataDao: TagDataDao

    private val selected: MutableSet<TagData> = HashSet()
    private val partiallySelected: MutableSet<TagData> = HashSet()
    var text: String? = null
        private set

    fun observe(owner: LifecycleOwner, observer: Observer<List<TagData>>) =
            tags.observe(owner, observer)

    fun setSelected(selected: List<TagData>, partiallySelected: List<TagData>?) {
        this.selected.addAll(selected)
        if (partiallySelected != null) {
            this.partiallySelected.addAll(partiallySelected)
        }
    }

    fun getSelected() = ArrayList(selected)

    fun getPartiallySelected() = ArrayList(partiallySelected)

    fun search(text: String) {
        if (!text.equals(this.text, ignoreCase = true)) {
            disposables.add(
                    Single.fromCallable { tagDataDao.searchTags(text) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { results: List<TagData> -> onUpdate(results.toMutableList()) })
        }
        this.text = text
    }

    private fun onUpdate(results: MutableList<TagData>) {
        if (!isNullOrEmpty(text) && !results.any { text.equals(it.name, ignoreCase = true) }) {
            results.add(0, TagData(text))
        }
        tags.value = results
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    fun getState(tagData: TagData): State {
        if (partiallySelected.contains(tagData)) {
            return State.PARTIALLY_CHECKED
        }
        return if (selected.contains(tagData)) State.CHECKED else State.UNCHECKED
    }

    fun toggle(tagData: TagData, checked: Boolean): State {
        var tagData = tagData
        if (tagData.id == null) {
            tagData = TagData(tagData.name)
            tagDataDao.createNew(tagData)
        }
        partiallySelected.remove(tagData)
        return if (checked) {
            selected.add(tagData)
            State.CHECKED
        } else {
            selected.remove(tagData)
            State.UNCHECKED
        }
    }
}