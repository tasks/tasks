package org.tasks.dialogs

import android.app.Activity
import android.content.DialogInterface
import androidx.fragment.app.DialogFragment
import org.tasks.preferences.Preferences
import org.tasks.themes.Theme
import javax.inject.Inject

abstract class BaseDateTimePicker : DialogFragment() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences

    interface OnDismissHandler {
        fun onDismiss()
    }

    protected var onDismissHandler: OnDismissHandler? = null
    protected val autoclose by lazy {
        arguments?.getBoolean(EXTRA_AUTO_CLOSE) ?: false
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (activity is OnDismissHandler) {
            onDismissHandler = activity
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismissHandler?.onDismiss()
    }

    override fun onCancel(dialog: DialogInterface) = sendSelected()

    protected abstract fun sendSelected()

    companion object {
        const val EXTRA_AUTO_CLOSE = "extra_auto_close"
    }
}
