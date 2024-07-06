@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.tasks

actual interface CommonParcelable

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class RawValue

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class IgnoredOnParcel

actual typealias CommonRawValue = RawValue

actual typealias CommonIgnoredOnParcel = IgnoredOnParcel

actual val IS_DEBUG = false

actual fun Long.printTimestamp(): String = this.toString()

actual fun formatCoordinates(coordinates: Double, latitude: Boolean) = coordinates.toString()
