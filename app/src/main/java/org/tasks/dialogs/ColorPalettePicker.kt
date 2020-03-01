package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import org.tasks.Callback
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseDialog
import org.tasks.dialogs.ColorWheelPicker.Companion.newColorWheel
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import org.tasks.themes.ThemeAccent
import org.tasks.themes.ThemeCache
import org.tasks.themes.ThemeColor
import javax.inject.Inject

class ColorPalettePicker : InjectingDialogFragment() {

    companion object {
        private const val FRAG_TAG_PURCHASE = "frag_tag_purchase"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        private const val EXTRA_PALETTE = "extra_palette"
        const val EXTRA_SELECTED = ColorWheelPicker.EXTRA_SELECTED

        fun newColorPalette(
            target: Fragment?,
            rc: Int,
            selected: Int
        ): ColorPalettePicker {
            return newColorPalette(target, rc, selected, ColorPickerAdapter.Palette.COLORS)
        }

        fun newColorPalette(
            target: Fragment?,
            rc: Int,
            palette: ColorPickerAdapter.Palette
        ): ColorPalettePicker {
            return newColorPalette(target, rc, 0, palette)
        }

        fun newColorPalette(
            target: Fragment?,
            rc: Int,
            selected: Int,
            palette: ColorPickerAdapter.Palette
        ): ColorPalettePicker {
            val args = Bundle()
            args.putSerializable(EXTRA_PALETTE, palette)
            args.putInt(EXTRA_SELECTED, selected)
            val dialog = ColorPalettePicker()
            dialog.setTargetFragment(target, rc)
            dialog.arguments = args
            return dialog
        }
    }

    interface Pickable : Parcelable {
        val pickerColor: Int
        val isFree: Boolean
        val index: Int
    }

    interface ColorPickedCallback {
        fun onColorPicked(index: Int)
    }

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var themeCache: ThemeCache

    @BindView(R.id.icons) lateinit var recyclerView: RecyclerView

    lateinit var colors: List<Pickable>
    lateinit var palette: ColorPickerAdapter.Palette
    var callback: ColorPickedCallback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_icon_picker, null)
        ButterKnife.bind(this, view)
        palette = arguments!!.getSerializable(EXTRA_PALETTE) as ColorPickerAdapter.Palette
        colors = when (palette) {
            ColorPickerAdapter.Palette.COLORS -> ThemeColor.COLORS.mapIndexed { index, color ->
                ThemeColor(context, index, ContextCompat.getColor(context!!, color))
            }
            ColorPickerAdapter.Palette.ACCENTS -> ThemeAccent.ACCENTS.mapIndexed { index, _ ->
                ThemeAccent(context, index)
            }
            ColorPickerAdapter.Palette.LAUNCHERS -> ThemeColor.LAUNCHER_COLORS.mapIndexed { index, color ->
                ThemeColor(context, index, ContextCompat.getColor(context!!, color))
            }
            ColorPickerAdapter.Palette.WIDGET_BACKGROUND -> themeCache.widgetThemes
        }

        val iconPickerAdapter = ColorPickerAdapter(
            context as Activity,
            inventory,
            Callback { index: Int -> onSelected(index) })
        recyclerView.layoutManager = IconLayoutManager(context)
        recyclerView.adapter = iconPickerAdapter
        iconPickerAdapter.submitList(colors)
        val builder =
            dialogBuilder
                .newDialog()
                .setView(view)
        if (palette == ColorPickerAdapter.Palette.COLORS) {
            builder.setNeutralButton(R.string.color_wheel) { _, _ ->
                val selected = arguments?.getInt(EXTRA_SELECTED) ?: 0
                newColorWheel(targetFragment, targetRequestCode, selected)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            }
        }
        if (inventory.purchasedThemes()) {
            builder.setNegativeButton(android.R.string.cancel, null)
        } else {
            builder.setPositiveButton(R.string.button_subscribe) { _: DialogInterface?, _: Int ->
                PurchaseDialog.newPurchaseDialog().show(parentFragmentManager, FRAG_TAG_PURCHASE)
            }
        }
        return builder.show()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (activity is ColorPickedCallback) {
            callback = activity
        }
    }

    private fun onSelected(index: Int) {
        val result = when (palette) {
            ColorPickerAdapter.Palette.COLORS ->
                (colors.find { it.index == index } as ThemeColor).primaryColor
            else -> index
        }
        dialog?.dismiss()
        if (targetFragment == null) {
            callback?.onColorPicked(result)
        } else {
            val data = Intent().putExtra(EXTRA_SELECTED, result)
            targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, data)
        }
    }

    override fun inject(component: DialogFragmentComponent) = component.inject(this)
}