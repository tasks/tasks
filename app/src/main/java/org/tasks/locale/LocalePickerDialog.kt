package org.tasks.locale

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.injection.DialogFragmentComponent
import org.tasks.injection.InjectingDialogFragment
import java.util.*
import javax.inject.Inject

class LocalePickerDialog : InjectingDialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var locale: Locale

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val locales = ArrayList<Locale>()
        locales.add(locale.withLanguage(null)) // device locale
        for (override in resources.getStringArray(R.array.localization)) {
            locales.add(locale.withLanguage(override))
        }
        val display = locales.map(Locale::getDisplayName)
        return dialogBuilder
                .newDialog()
                .setSingleChoiceItems(display, display.indexOf(locale.displayName)) { dialog, which ->
                    val locale = locales[which]
                    val data = Intent().putExtra(EXTRA_LOCALE, locale)
                    targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun inject(component: DialogFragmentComponent) = component.inject(this)

    companion object {
        const val EXTRA_LOCALE = "extra_locale"
        fun newLocalePickerDialog(): LocalePickerDialog {
            return LocalePickerDialog()
        }
    }
}