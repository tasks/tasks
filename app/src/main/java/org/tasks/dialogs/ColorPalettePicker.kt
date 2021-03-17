package org.tasks.dialogs

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.dialogs.ColorWheelPicker.Companion.newColorWheel
import org.tasks.themes.ColorProvider
import org.tasks.themes.ThemeColor
import javax.inject.Inject

@AndroidEntryPoint
class ColorPalettePicker : DialogFragment() {

    companion object {
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
        private const val EXTRA_PALETTE = "extra_palette"
        const val EXTRA_SELECTED = ColorWheelPicker.EXTRA_SELECTED

        fun newColorPalette(
            target: Fragment?,
            rc: Int,
            palette: Palette
        ): ColorPalettePicker {
            return newColorPalette(target, rc, 0, palette)
        }

        fun newColorPalette(
            target: Fragment?,
            rc: Int,
            selected: Int,
            palette: Palette = Palette.COLORS
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
    }

    interface ColorPickedCallback {
        fun onColorPicked(index: Int)
    }

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider

    @BindView(R.id.icons) lateinit var recyclerView: RecyclerView

    private lateinit var colors: List<Pickable>
    private lateinit var palette: Palette
    var callback: ColorPickedCallback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_icon_picker, null)
        ButterKnife.bind(this, view)
        palette = requireArguments().getSerializable(EXTRA_PALETTE) as Palette
        colors = when (palette) {
            Palette.COLORS -> colorProvider.getThemeColors()
            Palette.ACCENTS -> colorProvider.getAccentColors()
            Palette.LAUNCHERS -> ThemeColor.LAUNCHER_COLORS.map { color ->
                ThemeColor(context, requireContext().getColor(color))
            }
            Palette.WIDGET -> colorProvider.getWidgetColors()
        }

        val iconPickerAdapter = ColorPickerAdapter(requireActivity(), inventory, this::onSelected)
        recyclerView.layoutManager = IconLayoutManager(context)
        recyclerView.adapter = iconPickerAdapter
        iconPickerAdapter.submitList(colors)
        val builder =
            dialogBuilder
                .newDialog()
                .setView(view)
        if (palette == Palette.COLORS || palette == Palette.WIDGET) {
            builder.setNeutralButton(R.string.color_wheel) { _, _ ->
                val selected = arguments?.getInt(EXTRA_SELECTED) ?: 0
                newColorWheel(targetFragment, targetRequestCode, selected)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            }
        }
        if (inventory.purchasedThemes()) {
            builder.setNegativeButton(R.string.cancel, null)
        } else {
            builder.setPositiveButton(R.string.upgrade_to_pro) { _: DialogInterface?, _: Int ->
                startActivity(Intent(context, PurchaseActivity::class.java))
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
            Palette.COLORS, Palette.WIDGET -> (colors[index] as ThemeColor).originalColor
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
}