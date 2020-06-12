package org.tasks.injection

import android.app.Application
import android.content.ContentProvider

abstract class InjectingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        inject(
                DaggerContentProviderComponent.builder()
                        .applicationModule(ApplicationModule(context!!.applicationContext as Application))
                        .productionModule(ProductionModule())
                        .contentProviderModule(ContentProviderModule())
                        .build())
        return true
    }

    protected abstract fun inject(component: ContentProviderComponent)
}