package org.tasks.locale

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject

@AndroidEntryPoint
class LocalePickerDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val locales = ArrayList<java.util.Locale>()
        val selected =
            AppCompatDelegate.getApplicationLocales().let { java.util.Locale.forLanguageTag(it.toLanguageTags()) }
        val default = java.util.Locale.getDefault()
        locales.add(default) // device locale
        val tags = ArrayList<String>()
        val xrp = resources.getXml(R.xml.locales_config)
        var event = xrp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && xrp.name == "locale") {
                tags.add(xrp.getAttributeValue(0))
            }
            event = xrp.next()
        }
        for (override in tags) {
            locales.add(java.util.Locale.forLanguageTag(override))
        }
        val display = locales.map { it.getDisplayName(it) }
        return dialogBuilder
                .newDialog()
                .setSingleChoiceItems(display, display.indexOf(selected.getDisplayName(selected))) { dialog, which ->
                    val locale = locales[which].toLanguageTag()
                    val data = Intent().putExtra(EXTRA_LOCALE, locale)
                    dialog.dismiss()
                    targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    companion object {
        const val EXTRA_LOCALE = "extra_locale"

        fun newLocalePickerDialog(): LocalePickerDialog = LocalePickerDialog()
    }
}