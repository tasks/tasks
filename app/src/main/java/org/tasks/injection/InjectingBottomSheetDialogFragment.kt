package org.tasks.injection

import android.app.Activity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class InjectingBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var injected = false
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (!injected) {
            inject((activity as InjectingActivity).component.plus(DialogFragmentModule(this)))
            injected = true
        }
    }

    protected abstract fun inject(component: DialogFragmentComponent)
}