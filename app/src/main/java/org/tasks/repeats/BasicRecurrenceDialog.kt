package org.tasks.repeats

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.dialogs.DialogBuilder
import org.tasks.repeats.CustomRecurrenceActivity.Companion.newIntent
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.ui.SingleCheckedArrayAdapter
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BasicRecurrenceDialog : DialogFragment() {
    @Inject lateinit var context: Activity
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var repeatRuleToString: RepeatRuleToString

    private val customRecurrence =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            targetFragment?.onActivityResult(targetRequestCode, it.resultCode, it.data)
            dismiss()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments
        val dueDate = arguments!!.getLong(EXTRA_DATE, currentTimeMillis())
        val rule = arguments.getString(EXTRA_RRULE)
        var rrule: Recur? = null
        try {
            if (!isNullOrEmpty(rule)) {
                rrule = newRecur(rule!!)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        val customPicked = isCustomValue(rrule)
        val repeatOptions: List<String> =
            Lists.newArrayList(*requireContext().resources.getStringArray(R.array.repeat_options))
        val adapter = SingleCheckedArrayAdapter(requireContext(), repeatOptions)
        var selected = 0
        if (customPicked) {
            adapter.insert(repeatRuleToString!!.toString(rule), 0)
        } else if (rrule != null) {
            selected = when (rrule.frequency) {
                Recur.Frequency.DAILY -> 1
                Recur.Frequency.WEEKLY -> 2
                Recur.Frequency.MONTHLY -> 3
                Recur.Frequency.YEARLY -> 4
                else -> 0
            }
        }
        return dialogBuilder
            .newDialog()
            .setSingleChoiceItems(
                adapter,
                selected
            ) { dialogInterface: DialogInterface, i: Int ->
                var i = i
                if (customPicked) {
                    if (i == 0) {
                        dialogInterface.dismiss()
                        return@setSingleChoiceItems
                    }
                    i--
                }
                val result: Recur?
                when (i) {
                    0 -> result = null
                    5 -> {
                        customRecurrence.launch(newIntent(requireContext(), rule, dueDate))
                        return@setSingleChoiceItems
                    }
                    else -> {
                        result = newRecur()
                        result.interval = 1
                        when (i) {
                            1 -> result.setFrequency(Recur.Frequency.DAILY.name)
                            2 -> result.setFrequency(Recur.Frequency.WEEKLY.name)
                            3 -> result.setFrequency(Recur.Frequency.MONTHLY.name)
                            4 -> result.setFrequency(Recur.Frequency.YEARLY.name)
                        }
                    }
                }
                val intent = Intent()
                intent.putExtra(EXTRA_RRULE, result?.toString())
                targetFragment!!.onActivityResult(targetRequestCode, RESULT_OK, intent)
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun isCustomValue(rrule: Recur?): Boolean {
        if (rrule == null) {
            return false
        }
        val frequency = rrule.frequency
        return (frequency == Recur.Frequency.WEEKLY || frequency == Recur.Frequency.MONTHLY) && !rrule.dayList.isEmpty() || frequency == Recur.Frequency.HOURLY || frequency == Recur.Frequency.MINUTELY || rrule.until != null || rrule.interval > 1 || rrule.count > 0
    }

    companion object {
        const val EXTRA_RRULE = "extra_rrule"
        private const val EXTRA_DATE = "extra_date"
        fun newBasicRecurrenceDialog(
            target: Fragment?, rc: Int, rrule: String?, dueDate: Long
        ): BasicRecurrenceDialog {
            val dialog = BasicRecurrenceDialog()
            dialog.setTargetFragment(target, rc)
            val arguments = Bundle()
            if (rrule != null) {
                arguments.putString(EXTRA_RRULE, rrule)
            }
            arguments.putLong(EXTRA_DATE, dueDate)
            dialog.arguments = arguments
            return dialog
        }
    }
}
