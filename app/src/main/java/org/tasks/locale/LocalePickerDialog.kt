package org.tasks.locale

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.xmlpull.v1.XmlPullParser
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocalePickerDialog : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var locale: Locale

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val locales = ArrayList<Locale>()
        val xrp = resources.getXml(R.xml.locales_config)
        var event = xrp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && xrp.name == "locale") {
                locales.add(Locale.forLanguageTag(xrp.getAttributeValue(0)))
            }
            event = xrp.next()
        }
        val display = locales.map { it.getDisplayName(it) }
        return dialogBuilder
                .newDialog()
                .setSingleChoiceItems(display, display.indexOf(locale.getDisplayName(locale))) { dialog, which ->
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