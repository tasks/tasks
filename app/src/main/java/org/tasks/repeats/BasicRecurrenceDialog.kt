package org.tasks.repeats

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.BasicRecurrence
import org.tasks.compose.pickers.BasicRecurrenceOption
import org.tasks.compose.pickers.isCustomRecurrence
import org.tasks.dialogs.DialogBuilder
import org.tasks.repeats.CustomRecurrenceActivity.Companion.EXTRA_ACCOUNT_TYPE
import org.tasks.repeats.CustomRecurrenceActivity.Companion.newIntent
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BasicRecurrenceDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var repeatRuleToString: RepeatRuleToString
    @Inject lateinit var theme: Theme

    private val customRecurrence =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setResult(it.data?.getStringExtra(EXTRA_RRULE))
            }
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
        val customPicked = rrule?.isCustomRecurrence() == true
        val customLabel = if (customPicked) repeatRuleToString.toStringBlocking(rule) else null
        val selectedFrequency = if (!customPicked) rrule?.frequency else null
        return dialogBuilder
            .newDialog()
            .setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    BasicRecurrence(
                        customLabel = customLabel,
                        selectedFrequency = selectedFrequency,
                        onSelected = { option -> onSelected(option, rule, args) },
                    )
                }
            }
            .show()
    }

    private fun onSelected(option: BasicRecurrenceOption, rule: String?, args: Bundle) {
        when (option) {
            BasicRecurrenceOption.KeepCustom -> dismiss()
            BasicRecurrenceOption.DoesNotRepeat -> {
                setResult(null)
                dismiss()
            }
            is BasicRecurrenceOption.Frequency -> {
                val recur = newRecur().apply {
                    interval = 1
                    setFrequency(option.frequency.name)
                }
                setResult(recur.toString())
                dismiss()
            }
            BasicRecurrenceOption.Custom -> {
                customRecurrence.launch(
                    newIntent(
                        context = requireContext(),
                        rrule = rule,
                        dueDate = args.getLong(EXTRA_DATE),
                        accountType = args.getInt(EXTRA_ACCOUNT_TYPE)
                    )
                )
            }
        }
    }

    private fun setResult(rrule: String?) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(EXTRA_RRULE to rrule)
        )
    }

    companion object {
        const val EXTRA_RRULE = "extra_rrule"
        const val REQUEST_KEY = "basic_recurrence_result"
        private const val EXTRA_DATE = "extra_date"

        fun newBasicRecurrenceDialog(
            rrule: String?,
            dueDate: Long,
            accountType: Int,
        ): BasicRecurrenceDialog {
            val dialog = BasicRecurrenceDialog()
            dialog.arguments = Bundle().apply {
                rrule?.let { putString(EXTRA_RRULE, it) }
                putLong(EXTRA_DATE, dueDate)
                putInt(EXTRA_ACCOUNT_TYPE, accountType)
            }
            return dialog
        }
    }
}
