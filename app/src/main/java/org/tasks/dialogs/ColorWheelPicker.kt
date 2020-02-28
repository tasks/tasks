package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import org.tasks.ui.NavigationDrawerFragment.REQUEST_PURCHASE
import javax.inject.Inject

private const val REQUEST_PURCHASE = 10010

class ColorWheelPicker : InjectingDialogFragment() {

    companion object {
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        const val EXTRA_SELECTED = "extra_selected"

        fun newColorWheel(target: Fragment?, rc: Int, selected: Int): ColorWheelPicker {
            val args = Bundle()
            args.putInt(EXTRA_SELECTED, selected)
            val dialog = ColorWheelPicker()
            dialog.setTargetFragment(target, rc)
            dialog.arguments = args
            return dialog
        }
    }

    interface ColorPickedCallback {
        fun onColorPicked(color: Int)
    }

    @Inject lateinit var inventory: Inventory

    var dialog: AlertDialog? = null
    var selected = -1
    var callback: ColorPickedCallback? = null

    override fun inject(component: DialogFragmentComponent) = component.inject(this)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selected = savedInstanceState?.getInt(EXTRA_SELECTED) ?: arguments!!.getInt(EXTRA_SELECTED, 0)

        val button = if (inventory.hasPro()) android.R.string.ok else R.string.button_subscribe
        val builder = ColorPickerDialogBuilder
                .with(activity)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                .density(7)
                .setOnColorChangedListener { which ->
                    selected = which
                }
                .setOnColorSelectedListener { which ->
                    selected = which
                }
                .lightnessSliderOnly()
                .setPositiveButton(button) { _, _, _ ->
                    if (inventory.hasPro()) {
                        deliverSelection()
                    } else {
                        startActivityForResult(Intent(activity, PurchaseActivity::class.java), REQUEST_PURCHASE)
                    }
                }
        if (selected != 0) {
            builder.initialColor(selected)
        }
        val buttonText = if (inventory.hasPro()) R.string.material_palette else R.string.free_colors
        builder.setNegativeButton(buttonText) { _, _ ->
            newColorPalette(targetFragment, targetRequestCode, ColorPickerAdapter.Palette.COLORS)
                .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
        }
        dialog = builder.build()
        return dialog as Dialog
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (activity is ColorPickedCallback) {
            callback = activity
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == org.tasks.dialogs.REQUEST_PURCHASE) {
            if (inventory.hasPro()) {
                deliverSelection()
            } else {
                dialog?.cancel()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun deliverSelection() {
        dialog?.dismiss()
        if (targetFragment == null) {
            callback?.onColorPicked(selected)
        } else {
            val data = Intent().putExtra(EXTRA_SELECTED, selected)
            targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, data)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_SELECTED, selected)
    }
}