package org.tasks.sync

import android.app.Dialog
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.dialogs.DialogBuilder
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.preferences.fragments.Synchronization.Companion.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.Synchronization.Companion.REQUEST_GOOGLE_TASKS
import org.tasks.themes.DrawableUtil
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val services = requireActivity().resources.getStringArray(R.array.synchronization_services)
        val descriptions = requireActivity().resources.getStringArray(R.array.synchronization_services_description)
        val typedArray = requireActivity().resources.obtainTypedArray(R.array.synchronization_services_icons)
        val icons = IntArray(typedArray.length())
        for (i in icons.indices) {
            icons[i] = typedArray.getResourceId(i, 0)
        }
        typedArray.recycle()
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
                requireActivity(), R.layout.simple_list_item_2_themed, R.id.text1, services) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.findViewById<TextView>(R.id.text1).text = services[position]
                view.findViewById<TextView>(R.id.text2).text = descriptions[position]
                val icon = view.findViewById<ImageView>(R.id.image_view)
                icon.setImageDrawable(DrawableUtil.getWrapped(context, icons[position]))
                if (position == 2) {
                    icon.drawable.setTint(context.getColor(R.color.icon_tint))
                }
                return view
            }
        }
        return dialogBuilder
                .newDialog()
                .setTitle(R.string.choose_synchronization_service)
                .setSingleChoiceItems(adapter, -1) { dialog, which ->
                    when (which) {
                        0 -> activity?.startActivityForResult(
                                Intent(activity, GtasksLoginActivity::class.java),
                                REQUEST_GOOGLE_TASKS)
                        1 -> activity?.startActivity(
                                Intent(ACTION_VIEW, Uri.parse(getString(R.string.url_davx5))))
                        2 -> activity?.startActivityForResult(
                                Intent(activity, CaldavAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                        3 -> activity?.startActivityForResult(
                                Intent(activity, EtebaseAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                    }
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.help) { _, _ ->
                    activity?.startActivity(
                            Intent(
                                    ACTION_VIEW,
                                    Uri.parse(context?.getString(R.string.help_url_sync))
                            )
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    companion object {
        fun newAccountDialog(targetFragment: Fragment, rc: Int): AddAccountDialog {
            val dialog = AddAccountDialog()
            dialog.setTargetFragment(targetFragment, rc)
            return dialog
        }
    }
}