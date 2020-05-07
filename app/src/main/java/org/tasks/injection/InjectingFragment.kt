package org.tasks.injection

import android.app.Activity
import androidx.fragment.app.Fragment

abstract class InjectingFragment : Fragment() {
    private var injected = false
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (!injected) {
            inject((activity as InjectingActivity).component.plus(FragmentModule(this)))
            injected = true
        }
    }

    protected abstract fun inject(component: FragmentComponent)
}