package org.tasks.kmp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import org.tasks.kmp.BuildConfig
import org.tasks.extensions.formatNumber
import java.util.Locale

actual fun formatNumber(number: Int) = Locale.getDefault().formatNumber(number)

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
)

actual val PROD_ID = "+//IDN tasks.org//android-${BuildConfig.VERSION_CODE}//EN"

actual val DEV_URL: String = BuildConfig.DEV_URL

