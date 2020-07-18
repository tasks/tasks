package org.tasks.sync

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import org.tasks.R
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.dialogs.DialogBuilder
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.preferences.fragments.REQUEST_CALDAV_SETTINGS
import org.tasks.preferences.fragments.REQUEST_GOOGLE_TASKS
import org.tasks.themes.DrawableUtil

object AddAccountDialog {
    fun showAddAccountDialog(activity: Activity, dialogBuilder: DialogBuilder) {
        val services = activity.resources.getStringArray(R.array.synchronization_services)
        val descriptions = activity.resources.getStringArray(R.array.synchronization_services_description)
        val typedArray = activity.resources.obtainTypedArray(R.array.synchronization_services_icons)
        val icons = IntArray(typedArray.length())
        for (i in icons.indices) {
            icons[i] = typedArray.getResourceId(i, 0)
        }
        typedArray.recycle()
        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
                activity, R.layout.simple_list_item_2_themed, R.id.text1, services) {
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
        dialogBuilder
                .newDialog()
                .setTitle(R.string.choose_synchronization_service)
                .setSingleChoiceItems(
                        adapter,
                        -1
                ) { dialog: DialogInterface, which: Int ->
                    when (which) {
                        0 -> activity.startActivityForResult(
                                Intent(activity, GtasksLoginActivity::class.java),
                                REQUEST_GOOGLE_TASKS)
                        1 -> activity.startActivityForResult(
                                Intent(activity, CaldavAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                        2 -> activity.startActivityForResult(
                                Intent(activity, EteSyncAccountSettingsActivity::class.java),
                                REQUEST_CALDAV_SETTINGS)
                    }
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.help) { _, _ ->
                    activity.startActivity(
                            Intent(
                                    Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.help_url_sync))))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }
}