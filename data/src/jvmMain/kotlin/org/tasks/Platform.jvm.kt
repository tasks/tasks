@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.tasks

actual interface CommonParcelable

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class RawValue

actual typealias CommonRawValue = RawValue

actual val IS_DEBUG = false

actual fun Long.printTimestamp(): String = this.toString()
