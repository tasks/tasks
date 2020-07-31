package org.tasks.sync

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavDao
import org.tasks.data.OpenTaskDao.Companion.ACCOUNT_TYPE_DAVx5
import org.tasks.data.OpenTaskDao.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.dialogs.DialogBuilder
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.jobs.WorkManager
import org.tasks.preferences.fragments.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.REQUEST_GOOGLE_TASKS
import org.tasks.themes.DrawableUtil
import javax.inject.Inject

@AndroidEntryPoint
class AddAccountDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var caldavDao: CaldavDao
    @Inject lateinit var syncAdapters: SyncAdapters
    @Inject lateinit var workManager: WorkManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val services = requireActivity().resources.getStringArray(R.array.synchronization_services).toMutableList()
        val descriptions = requireActivity().resources.getStringArray(R.array.synchronization_services_description).toMutableList()
        val icons = arrayListOf(
                R.drawable.ic_google,
                R.drawable.ic_webdav_logo,
                R.drawable.ic_etesync
        )
        val types = arrayListOf("", "", "")
        requireArguments().getStringArrayList(EXTRA_ACCOUNTS)?.forEach { account ->
            val (type, name) = account.split(":")
            when (type) {
                ACCOUNT_TYPE_DAVx5 -> {
                    services.add(name)
                    descriptions.add(getString(R.string.davx5))
                    types.add(ACCOUNT_TYPE_DAVx5)
                    icons.add(R.drawable.ic_davx5_icon_green_bg)
                }
                ACCOUNT_TYPE_ETESYNC -> {
                    services.add(name)
                    descriptions.add(getString(R.string.etesync))
                    types.add(ACCOUNT_TYPE_ETESYNC)
                    icons.add(R.drawable.ic_etesync)
                }
            }
        }
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
                requireActivity(), R.layout.simple_list_item_2_themed, R.id.text1, services) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.findViewById<TextView>(R.id.text1).text = services[position]
                view.findViewById<TextView>(R.id.text2).text = descriptions[position]
                val icon = view.findViewById<ImageView>(R.id.image_view)
                icon.setImageDrawable(DrawableUtil.getWrapped(context, icons[position]))
                if (position == 1) {
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
                        0 -> {
                            activity?.startActivityForResult(
                                    Intent(activity, GtasksLoginActivity::class.java),
                                    REQUEST_GOOGLE_TASKS)
                            dialog.dismiss()
                        }
                        1 -> {
                            activity?.startActivityForResult(
                                    Intent(activity, CaldavAccountSettingsActivity::class.java),
                                    REQUEST_CALDAV_SETTINGS)
                            dialog.dismiss()
                        }
                        2 -> {
                            activity?.startActivityForResult(
                                    Intent(activity, EteSyncAccountSettingsActivity::class.java),
                                    REQUEST_CALDAV_SETTINGS)
                            dialog.dismiss()
                        }
                        else -> {
                            lifecycleScope.launch {
                                caldavDao.insert(CaldavAccount().apply {
                                    name = services[which]
                                    uuid = "${types[which]}:${name}"
                                    accountType = CaldavAccount.TYPE_OPENTASKS
                                })
                                syncAdapters.sync(true)
                                workManager.updateBackgroundSync()
                                dialog.dismiss()
                                targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
                            }
                        }
                    }
                }
                .setNeutralButton(R.string.help) { _, _ ->
                    activity?.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context?.getString(R.string.help_url_sync))))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    companion object {
        private const val EXTRA_ACCOUNTS = "extra_accounts"

        fun newAccountDialog(
                targetFragment: Fragment, rc: Int, openTaskAccounts: List<String>
        ): AddAccountDialog {
            val dialog = AddAccountDialog()
            dialog.arguments = Bundle().apply {
                putStringArrayList(EXTRA_ACCOUNTS, ArrayList(openTaskAccounts))
            }
            dialog.setTargetFragment(targetFragment, rc)
            return dialog
        }
    }
}