package org.tasks.activities.attribution

import android.content.Context
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class AttributionViewModel : ViewModel() {
    private val attributions = MutableLiveData<List<LibraryAttribution>?>()
    private val disposables = CompositeDisposable()
    private var loaded = false

    fun observe(activity: AppCompatActivity, observer: Observer<List<LibraryAttribution>?>) {
        attributions.observe(activity, observer)
        load(activity)
    }

    private fun load(context: Context) {
        if (loaded) {
            return
        }
        loaded = true
        disposables.add(
                Single.fromCallable {
                    val licenses = context.assets.open("licenses.json")
                    val reader = InputStreamReader(licenses, StandardCharsets.UTF_8)
                    val list = GsonBuilder().create().fromJson(reader, AttributionList::class.java)
                    list.libraries
                }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ value: List<LibraryAttribution>? -> attributions.setValue(value) }) { t: Throwable? -> Timber.e(t) })
    }

    override fun onCleared() {
        disposables.dispose()
    }

    internal class AttributionList {
        var libraries: List<LibraryAttribution>? = null
    }

    class LibraryAttribution {
        var copyrightHolder: String? = null

        @get:Keep
        var license: String? = null
        var libraryName: String? = null

    }
}