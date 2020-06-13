package org.tasks.injection

import android.content.ContentProvider

abstract class InjectingContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        inject(
                DaggerContentProviderComponent.builder()
                        .applicationModule(ApplicationModule(context!!.applicationContext))
                        .productionModule(ProductionModule())
                        .contentProviderModule(ContentProviderModule())
                        .build())
        return true
    }

    protected abstract fun inject(component: ContentProviderComponent)
}