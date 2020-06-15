package org.tasks.injection

import android.content.ContentProvider

abstract class InjectingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        inject(Dagger.Companion[context!!].applicationComponent)
        return true
    }

    protected abstract fun inject(component: ApplicationComponent)
}