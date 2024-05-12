package org.tasks.data

import android.content.Context
import android.net.Uri
import org.tasks.data.entity.Place
import org.tasks.extensions.Context.openUri
import org.tasks.location.MapPosition

fun Place.open(context: Context?) =
    context?.openUri("geo:$latitude,$longitude?q=${Uri.encode(displayName)}")

val Place.mapPosition: MapPosition
    get() = MapPosition(latitude, longitude)
