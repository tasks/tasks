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
import org.tasks.auth.SignInActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.dialogs.DialogBuilder
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.extensions.getMutableIntList
import org.tasks.extensions.getMutableStringList
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_GOOGLE_TASKS
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.themes.DrawableUtil
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder

    private val hasTasksAccount: Boolean
        get() = arguments?.getBoolean(EXTRA_HAS_TASKS_ACCOUNT) ?: false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val services = resources.getMutableStringList(R.array.synchronization_services)
        val descriptions = resources.getMutableStringList(R.array.synchronization_services_description)
        val icons = resources.getMutableIntList(R.array.synchronization_services_icons)
        if (hasTasksAccount) {
            services.removeAt(0)
            descriptions.removeAt(0)
            icons.removeAt(0)
        }
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
                requireActivity(), R.layout.simple_list_item_2_themed, R.id.text1, services) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.findViewById<TextView>(R.id.text1).text = services[position]
                view.findViewById<TextView>(R.id.text2).text = descriptions[position]
                val icon = view.findViewById<ImageView>(R.id.image_view)
                val iconResId = icons[position]
                icon.setImageDrawable(DrawableUtil.getWrapped(context, iconResId))
                if (iconResId == R.drawable.ic_webdav_logo) {
                    icon.drawable.setTint(context.getColor(R.color.icon_tint))
                }
                return view
            }
        }
        return dialogBuilder
                .newDialog()
                .setTitle(R.string.choose_synchronization_service)
                .setSingleChoiceItems(adapter, -1) { dialog, which ->
                    when (if (hasTasksAccount) which + 1 else which) {
                        0 -> activity?.startActivityForResult(
                                Intent(activity, SignInActivity::class.java),
                                REQUEST_TASKS_ORG)
                        1 -> activity?.startActivityForResult(
                                Intent(activity, GtasksLoginActivity::class.java),
                                REQUEST_GOOGLE_TASKS)
                        2 -> activity?.startActivity(
                                Intent(ACTION_VIEW, Uri.parse(getString(R.string.url_davx5))))
                        3 -> activity?.startActivityForResult(
                                Intent(activity, CaldavAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                        4 -> activity?.startActivityForResult(
                                Intent(activity, EtebaseAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                        5 -> activity?.startActivity(
                                Intent(ACTION_VIEW, Uri.parse(getString(R.string.url_decsync))))
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
        private const val EXTRA_HAS_TASKS_ACCOUNT = "extra_has_tasks_account"

        fun newAccountDialog(
                targetFragment: Fragment,
                rc: Int,
                hasTasksAccount: Boolean
        ): AddAccountDialog {
            val dialog = AddAccountDialog()
            dialog.arguments = Bundle().apply {
                putBoolean(EXTRA_HAS_TASKS_ACCOUNT, hasTasksAccount)
            }
            dialog.setTargetFragment(targetFragment, rc)
            return dialog
        }
    }
}