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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeCache
import org.tasks.themes.ThemeCache.EXTRA_THEME_OVERRIDE
import javax.inject.Inject

class ThemePickerDialog : InjectingDialogFragment() {

    companion object {
        const val EXTRA_SELECTED = "extra_selected"

        fun newThemePickerDialog(target: Fragment, rc: Int, selected: Int): ThemePickerDialog {
            val args = Bundle()
            args.putInt(EXTRA_SELECTED, selected)
            val dialog = ThemePickerDialog()
            dialog.setTargetFragment(target, rc)
            dialog.arguments = args
            return dialog
        }
    }

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var accent: ThemeAccent
    @Inject lateinit var themeCache: ThemeCache
    @Inject lateinit var themeBase: ThemeBase

    var adapter: ArrayAdapter<String>? = null
    var dialog: AlertDialog? = null
    var selected = -1

    override fun inject(component: DialogFragmentComponent) = component.inject(this)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val themes = resources.getStringArray(R.array.base_theme_names)

        selected = savedInstanceState?.getInt(EXTRA_SELECTED) ?: arguments!!.getInt(EXTRA_SELECTED)

        adapter = object : ArrayAdapter<String>(activity!!, R.layout.simple_list_item_single_choice, themes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textColor = if (inventory.purchasedThemes() || position < 2) {
                    R.color.text_primary
                } else {
                    R.color.text_tertiary
                }
                val text: TextView = view.findViewById(R.id.text1)
                text.setTextColor(ContextCompat.getColor(context, textColor))
                return view
            }
        }

        dialog = dialogBuilder.newDialog()
                .setTitle(R.string.theme)
                .setSingleChoiceItems(adapter, arguments!!.getInt(EXTRA_SELECTED)) { _, which ->
                    selected = which
                    if (available()) {
                        deliverResult()
                    } else {
                        updateButton()
                        activity?.intent?.putExtra(EXTRA_THEME_OVERRIDE, which)
                        Handler().post {
                            themeCache.getThemeBase(which).setDefaultNightMode()
                            activity?.recreate()
                            activity?.overridePendingTransition(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit);
                        }
                    }
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
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
            android.R.string.cancel
        } else {
            R.string.button_subscribe
        }

        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = getString(stringRes)
    }

    private fun available() = inventory.purchasedThemes() || selected < 2
}