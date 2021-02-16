package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.TestUtilities.readFile
import org.tasks.location.PlaceSearchGoogle.Companion.toJson
import org.tasks.location.PlaceSearchGoogle.Companion.toPlace
import org.tasks.location.PlaceSearchGoogle.Companion.toSearchResults

class PlaceSearchGoogleTest {
    @Test
    fun placeSearchWithMultipleResults() {
        val results = toSearchResults(readFile("google_places/search.json").toJson())

        assertEquals(
                listOf(
                        "ChIJfQQVCCMzjoARce0POzONI8I",
                        "ChIJQXqgXsiAj4ARqtl6U4GD-Cw",
                        "ChIJCWNdVNgr3YAR4pLlOt8CfEk",
                        "ChIJhTEH6lev3IARDMKC_pGF6nI"
                ),
                results.map { it.id }
        )
    }

    @Test
    fun validatePlace() {
        val result = toSearchResults(readFile("google_places/search.json").toJson())[2]

        assertEquals("ChIJCWNdVNgr3YAR4pLlOt8CfEk", result.id)
        assertEquals("Portillo's Hot Dogs", result.name)
        assertEquals("La Palma Avenue, Buena Park, CA, USA", result.address)
    }

    @Test
    fun fetchPlace() {
        val result = toPlace(readFile("google_places/fetch.json").toJson())

        assertEquals("Magic Kingdom Park", result.name)
        assertEquals("1180 Seven Seas Drive, Lake Buena Vista, FL 32836, USA", result.address)
        assertEquals(28.417663, result.latitude, 0.0)
        assertEquals(-81.581212, result.longitude, 0.0)
        assertEquals("+1 407-939-5277", result.phone)
        assertEquals(
                "https://disneyworld.disney.go.com/destinations/magic-kingdom/?CMP=OKC-80007944_GM_WDW_destination_magickingdompark_NA",
                result.url
        )
    }
}