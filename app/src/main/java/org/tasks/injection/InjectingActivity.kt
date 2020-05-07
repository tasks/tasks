package org.tasks.injection

interface InjectingActivity {
    fun inject(component: ActivityComponent)

    val component: ActivityComponent
}