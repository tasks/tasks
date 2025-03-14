package org.tasks.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.getTimeString
import org.tasks.preferences.Preferences
import org.tasks.themes.Theme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.withMillisOfDay
import javax.inject.Inject

abstract class BaseDateTimePicker : BottomSheetDialogFragment() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences

    protected var morning = 32401000
    protected var afternoon = 46801000
    protected var evening = 61201000
    protected var night = 72001000

    interface OnDismissHandler {
        fun onDismiss()
    }

    private var onDismissHandler: OnDismissHandler? = null

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

    override fun onResume() {
        super.onResume()

        refreshButtons()
    }

    override fun onCancel(dialog: DialogInterface) = sendSelected()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            dialog
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { bottomSheet ->
                    val behavior = BottomSheetBehavior.from(bottomSheet)
                    behavior.skipCollapsed = true

                    val insets = ViewCompat.getRootWindowInsets(requireActivity().window.decorView)
                    if (insets?.isVisible(WindowInsetsCompat.Type.ime()) == true) {
                        lifecycleScope.launch {
                            delay(100)
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    } else {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

            if (!closeAutomatically()) {
                addButtons(dialog)
            }
        }
        return dialog
    }

    private fun addButtons(dialog: BottomSheetDialog) {
        val coordinator = dialog
                .findViewById<CoordinatorLayout>(com.google.android.material.R.id.coordinator)
        val containerLayout =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.container)
        val buttons = theme.getLayoutInflater(requireContext())
            .inflate(R.layout.dialog_date_time_picker_buttons, null)
        buttons.findViewById<View>(R.id.cancel_button).setOnClickListener { dismiss() }
        buttons.findViewById<View>(R.id.ok_button).setOnClickListener { sendSelected() }
        buttons.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
        ).apply {
            gravity = Gravity.BOTTOM
        }
        containerLayout!!.addView(buttons)

        buttons.post {
            (coordinator!!.layoutParams as ViewGroup.MarginLayoutParams).apply {
                buttons.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                this.bottomMargin = buttons.measuredHeight
                containerLayout.requestLayout()
            }
        }
    }

    protected fun closeAutomatically(): Boolean = arguments?.getBoolean(EXTRA_AUTO_CLOSE) ?: false

    protected fun setupShortcutsAndCalendar() {
        morning = preferences.dateShortcutMorning + 1000
        afternoon = preferences.dateShortcutAfternoon + 1000
        evening = preferences.dateShortcutEvening + 1000
        night = preferences.dateShortcutNight + 1000
        val is24HourFormat = requireContext().is24HourFormat
        val now = currentTimeMillis()
        morningButton.text = getTimeString(now.withMillisOfDay(morning), is24HourFormat)
        afternoonButton.text = getTimeString(now.withMillisOfDay(afternoon), is24HourFormat)
        eveningButton.text = getTimeString(now.withMillisOfDay(evening), is24HourFormat)
        nightButton.text = getTimeString(now.withMillisOfDay(night), is24HourFormat)
    }

    protected abstract val calendarView: CalendarView

    protected abstract val morningButton: MaterialButton

    protected abstract val afternoonButton: MaterialButton

    protected abstract val eveningButton: MaterialButton

    protected abstract val nightButton: MaterialButton

    protected abstract fun sendSelected()

    protected abstract fun refreshButtons()

    companion object {
        const val EXTRA_AUTO_CLOSE = "extra_auto_close"
    }
}