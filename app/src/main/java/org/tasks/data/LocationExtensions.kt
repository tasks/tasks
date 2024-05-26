package org.tasks.data

import android.content.Context

fun Location.open(context: Context?) {
    place.open(context)
}

val Location.displayName: String
    get() = place.displayName
