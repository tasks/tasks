package org.tasks.location

import android.os.Bundle
import androidx.annotation.DrawableRes
import org.tasks.Callback
import org.tasks.data.Place

interface PlaceSearchProvider {
    fun restoreState(savedInstanceState: Bundle?)

    fun saveState(outState: Bundle)

    @DrawableRes
    fun getAttributionRes(dark: Boolean): Int

    suspend fun search(query: String, bias: MapPosition?): List<PlaceSearchResult>

    suspend fun fetch(placeSearchResult: PlaceSearchResult): Place
}