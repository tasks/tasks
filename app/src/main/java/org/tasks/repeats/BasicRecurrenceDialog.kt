package org.tasks.repeats

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_ACCOUNT_TYPE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.newIntent
import org.tasks.repeats.RecurrenceUtils.newRecur
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
        val args = requireArguments()
        val rule = args.getString(EXTRA_RRULE)
        val rrule =
            rule
                .takeIf { !it.isNullOrBlank() }
                ?.let {
                    try {
                        newRecur(it)
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                }
        val customPicked = isCustomValue(rrule)
        val repeatOptions: List<String> =
            Lists.newArrayList(*requireContext().resources.getStringArray(R.array.repeat_options))
        val adapter = SingleCheckedArrayAdapter(requireContext(), repeatOptions)
        var selected = 0
        if (customPicked) {
            adapter.insert(repeatRuleToString.toString(rule), 0)
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
            .setSingleChoiceItems(adapter, selected) { dialog, selectedIndex: Int ->
                val i = if (customPicked) {
                    if (selectedIndex == 0) {
                        dialog.dismiss()
                        return@setSingleChoiceItems
                    }
                    selectedIndex - 1
                } else {
                    selectedIndex
                }
                val result = when (i) {
                    0 -> null
                    5 -> {
                        customRecurrence.launch(
                            newIntent(
                                context = requireContext(),
                                rrule = rule,
                                dueDate = args.getLong(EXTRA_DATE),
                                accountType = args.getInt(EXTRA_ACCOUNT_TYPE)
                            )
                        )
                        return@setSingleChoiceItems
                    }
                    else -> {
                        val frequency = when(i) {
                            1 -> Recur.Frequency.DAILY
                            2 -> Recur.Frequency.WEEKLY
                            3 -> Recur.Frequency.MONTHLY
                            4 -> Recur.Frequency.YEARLY
                            else -> throw IllegalArgumentException()
                        }
                        newRecur().apply {
                            interval = 1
                            setFrequency(frequency.name)
                        }
                    }
                }
                val intent = Intent()
                intent.putExtra(EXTRA_RRULE, result?.toString())
                targetFragment!!.onActivityResult(targetRequestCode, RESULT_OK, intent)
                dialog.dismiss()
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
            target: Fragment?,
            rc: Int,
            rrule: String?,
            dueDate: Long,
            accountType: Int,
        ): BasicRecurrenceDialog {
            val dialog = BasicRecurrenceDialog()
            dialog.setTargetFragment(target, rc)
            dialog.arguments = Bundle().apply {
                rrule?.let { putString(EXTRA_RRULE, it) }
                putLong(EXTRA_DATE, dueDate)
                putInt(EXTRA_ACCOUNT_TYPE, accountType)
            }
            return dialog
        }
    }
}
