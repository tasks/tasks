package org.tasks.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule

abstract class InjectingTestCase {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    protected fun runOnMainSync(runnable: Runnable) =
            InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable)

    protected val context: Context
        get() = getApplicationContext()
}