package org.tasks.dialogs

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeBase.EXTRA_THEME_OVERRIDE
import javax.inject.Inject

@AndroidEntryPoint
class ThemePickerDialog : DialogFragment() {

    companion object {
        const val EXTRA_SELECTED = "extra_selected"
        const val EXTRA_WIDGET = "extra_widget"

        fun newThemePickerDialog(
            target: Fragment,
            rc: Int,
            selected: Int,
            widget: Boolean = false
        ): ThemePickerDialog {
            val args = Bundle()
            args.putInt(EXTRA_SELECTED, selected)
            args.putBoolean(EXTRA_WIDGET, widget)
            val dialog = ThemePickerDialog()
            dialog.setTargetFragment(target, rc)
            dialog.arguments = args
            return dialog
        }
    }

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var accent: ThemeAccent
    @Inject lateinit var themeBase: ThemeBase

    var adapter: ArrayAdapter<String>? = null
    var dialog: AlertDialog? = null
    var selected = -1
    var widget = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selected = savedInstanceState?.getInt(EXTRA_SELECTED) ?: requireArguments().getInt(EXTRA_SELECTED)
        widget = arguments?.getBoolean(EXTRA_WIDGET) ?: false
        val themes = resources.getStringArray(
            if (widget) R.array.widget_themes else R.array.base_theme_names
        )

        adapter = object : ArrayAdapter<String>(requireActivity(), R.layout.simple_list_item_single_choice, themes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textColor = if (isAvailable(position)) {
                    R.color.text_primary
                } else {
                    R.color.text_tertiary
                }
                val text: TextView = view.findViewById(R.id.text1)
                text.setTextColor(context.getColor(textColor))
                return view
            }
        }

        dialog = dialogBuilder.newDialog()
                .setTitle(R.string.theme)
                .setSingleChoiceItems(adapter, requireArguments().getInt(EXTRA_SELECTED)) { _, which ->
                    selected = which
                    if (available()) {
                        deliverResult()
                    } else {
                        updateButton()
                        activity?.intent?.putExtra(EXTRA_THEME_OVERRIDE, which)
                        Handler().post {
                            ThemeBase(which).setDefaultNightMode()
                            activity?.recreate()
                            activity?.overridePendingTransition(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
                        }
                    }
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    deliverResult()
                }
                .show()
        updateButton()
        return dialog as Dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        if (available()) {
            deliverResult()
        } else {
            targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
        }
    }

    private fun deliverResult() {
        dialog?.dismiss()
        targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, Intent().putExtra(EXTRA_SELECTED, selected))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_SELECTED, selected)
    }

    private fun updateButton() {
        val stringRes = if (available()) {
            R.string.cancel
        } else {
            R.string.button_subscribe
        }

        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = getString(stringRes)
    }

    private fun available() = isAvailable(selected)

    private fun isAvailable(index: Int) =
        inventory.purchasedThemes() || ThemeBase(index).isFree || widget
}