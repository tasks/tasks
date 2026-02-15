package org.tasks.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.billing.PurchaseActivityViewModel
import org.tasks.databinding.DialogIconPickerBinding
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
        private const val EXTRA_REQUEST_KEY = "extra_request_key"
        const val EXTRA_SELECTED = ColorWheelPicker.EXTRA_SELECTED

        fun newColorPalette(
            requestKey: String,
            palette: Palette
        ): ColorPalettePicker {
            return newColorPalette(requestKey, 0, palette)
        }

        fun newColorPalette(
            requestKey: String,
            selected: Int,
            palette: Palette = Palette.COLORS
        ): ColorPalettePicker {
            val args = Bundle()
            args.putSerializable(EXTRA_PALETTE, palette)
            args.putInt(EXTRA_SELECTED, selected)
            args.putString(EXTRA_REQUEST_KEY, requestKey)
            val dialog = ColorPalettePicker()
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

    private lateinit var colors: List<Pickable>
    private lateinit var palette: Palette
    var callback: ColorPickedCallback? = null

    private val requestKey: String
        get() = requireArguments().getString(EXTRA_REQUEST_KEY)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogIconPickerBinding.inflate(LayoutInflater.from(context))
        palette = requireArguments().getSerializable(EXTRA_PALETTE) as Palette
        colors = when (palette) {
            Palette.COLORS -> colorProvider.getThemeColors()
            Palette.LAUNCHERS -> ThemeColor.LAUNCHER_COLORS.map { color ->
                ThemeColor(context, requireContext().getColor(color))
            }
            Palette.WIDGET -> colorProvider.getWidgetColors()
        }

        val iconPickerAdapter = ColorPickerAdapter(requireActivity(), inventory, this::onSelected)
        with(binding.icons) {
            layoutManager = IconLayoutManager(context)
            adapter = iconPickerAdapter
        }
        iconPickerAdapter.submitList(colors)
        val builder =
            dialogBuilder
                .newDialog()
                .setView(binding.root)
        if (palette == Palette.COLORS || palette == Palette.WIDGET) {
            builder.setNeutralButton(R.string.color_wheel) { _, _ ->
                val selected = arguments?.getInt(EXTRA_SELECTED) ?: 0
                newColorWheel(requestKey, selected)
                    .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            }
        }
        if (inventory.purchasedThemes()) {
            builder.setNegativeButton(R.string.cancel, null)
        } else {
            builder.setPositiveButton(R.string.name_your_price) { _: DialogInterface?, _: Int ->
                startActivity(
                    Intent(context, PurchaseActivity::class.java)
                        .putExtra(PurchaseActivityViewModel.EXTRA_SOURCE, "colors")
                )
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
        if (callback != null) {
            callback?.onColorPicked(result)
        } else {
            parentFragmentManager.setFragmentResult(
                requestKey,
                bundleOf(EXTRA_SELECTED to result)
            )
        }
    }
}
