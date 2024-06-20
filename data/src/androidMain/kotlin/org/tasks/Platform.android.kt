@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.tasks

import android.os.Parcelable
import kotlinx.parcelize.RawValue
import org.tasks.data.BuildConfig
import java.util.Date

actual typealias CommonParcelable = Parcelable

actual typealias CommonRawValue = RawValue

actual val IS_DEBUG = BuildConfig.DEBUG

actual fun Long.printTimestamp(): String = Date(this).toString()