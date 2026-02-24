package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ColorWheelPicker : DialogFragment() {

    companion object {
        const val EXTRA_SELECTED = "extra_selected"
        private const val EXTRA_REQUEST_KEY = "extra_request_key"
        private const val REQUEST_PURCHASE = 10010

        fun newColorWheel(requestKey: String, selected: Int): ColorWheelPicker {
            val args = Bundle()
            args.putInt(EXTRA_SELECTED, selected)
            args.putString(EXTRA_REQUEST_KEY, requestKey)
            val dialog = ColorWheelPicker()
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

    private val requestKey: String
        get() = requireArguments().getString(EXTRA_REQUEST_KEY)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selected = savedInstanceState?.getInt(EXTRA_SELECTED) ?: requireArguments().getInt(EXTRA_SELECTED, 0)

        val button = if (inventory.purchasedThemes()) R.string.ok else R.string.name_your_price
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
                    if (inventory.purchasedThemes()) {
                        deliverSelection()
                    } else {
                        startActivityForResult(
                            Intent(context, PurchaseActivity::class.java)
                                .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "colors"),
                            REQUEST_PURCHASE
                        )
                    }
                }
                .setNegativeButton(R.string.cancel, null)
        if (selected != 0) {
            builder.initialColor(selected)
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
        if (requestCode == REQUEST_PURCHASE) {
            if (inventory.hasPro) {
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
        if (callback != null) {
            callback?.onColorPicked(selected)
        } else {
            parentFragmentManager.setFragmentResult(
                requestKey,
                bundleOf(EXTRA_SELECTED to selected)
            )
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        // no result on cancel
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(EXTRA_SELECTED, selected)
    }
}
