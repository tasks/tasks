package org.tasks.location

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.todoroo.andlib.utility.AndroidUtilities.atLeastR
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.BuildConfig
import org.tasks.PermissionUtil.verifyPermissions
import org.tasks.R
import org.tasks.databinding.DialogLocationPermissionsBinding
import org.tasks.dialogs.DialogBuilder
import org.tasks.preferences.FragmentPermissionRequestor
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.PermissionRequestor.REQUEST_BACKGROUND_LOCATION
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationPermissionDialog : DialogFragment() {

    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var permissionChecker: PermissionChecker
    @Inject lateinit var permissionRequestor: FragmentPermissionRequestor

    lateinit var binding: DialogLocationPermissionsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogLocationPermissionsBinding.inflate(layoutInflater)

        if (atLeastR()) {
            binding.foregroundLocation.visibility = View.VISIBLE
        }

        binding.foregroundLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                permissionRequestor.requestForegroundLocation()
            }
        }

        binding.backgroundLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                permissionRequestor.requestBackgroundLocation()
            }
        }

        return dialogBuilder.newDialog(R.string.missing_permissions)
                .setView(binding.root)
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                .setNeutralButton(R.string.TLA_menu_settings) { _, _ ->
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    })
                }
                .show()
    }

    override fun onResume() {
        super.onResume()

        if (atLeastR()) {
            binding.foregroundLocation.isChecked = permissionChecker.canAccessForegroundLocation()
            binding.foregroundLocation.isClickable = !binding.foregroundLocation.isChecked
            binding.backgroundLocation.isEnabled = binding.foregroundLocation.isChecked
        }
        binding.backgroundLocation.isChecked = permissionChecker.canAccessBackgroundLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Timber.d("onRequestPermissionResult(requestCode = $requestCode, permissions = [${permissions.joinToString()}], grantResults = [${grantResults.joinToString()}])")
        when(requestCode) {
            REQUEST_BACKGROUND_LOCATION -> {
                if (verifyPermissions(grantResults)) {
                    targetFragment?.onActivityResult(targetRequestCode, RESULT_OK, null)
                    dismiss()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        targetFragment?.onActivityResult(targetRequestCode, RESULT_CANCELED, null)
    }

    companion object {
        fun newLocationPermissionDialog(
                targetFragment: Fragment,
                rc: Int
        ): LocationPermissionDialog = LocationPermissionDialog().apply {
            setTargetFragment(targetFragment, rc)
        }
    }
}