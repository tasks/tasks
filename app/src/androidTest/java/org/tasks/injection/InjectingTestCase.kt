package org.tasks.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import timber.log.Timber

abstract class InjectingTestCase {
    @Before
    open fun setUp() {
        Thread.setDefaultUncaughtExceptionHandler { _, e: Throwable? -> Timber.e(e) }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val component = DaggerTestComponent.builder()
                .applicationModule(ApplicationModule(context))
                .testModule(TestModule()).build()
        inject(component)
    }

    protected abstract fun inject(component: TestComponent)
}