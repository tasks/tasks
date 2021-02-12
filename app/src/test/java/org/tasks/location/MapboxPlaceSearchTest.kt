package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TestUtilities.readFile
import org.tasks.location.MapboxSearchProvider.Companion.jsonToSearchResults

class MapboxPlaceSearchTest {
    @Test
    fun searchWithMultipleResults() {
        val results = jsonToSearchResults(readFile("mapbox/search.json"))

        assertEquals(
                listOf(
                        "poi.627065251624",
                        "poi.472446436443",
                        "poi.89343",
                        "poi.549755920662",
                        "poi.755914248504"
                ),
                results.map { it.id }
        )
    }

    @Test
    fun validatePlace() {
        val place = jsonToSearchResults(readFile("mapbox/search.json"))[1]

        assertEquals("poi.472446436443", place.id)
        assertEquals("Portillo's", place.name)
        assertEquals(-87.962508, place.place.longitude, 0.0)
        assertEquals(41.895473, place.place.latitude, 0.0)
        assertEquals(
                "155 S Il Route 83, Elmhurst, Illinois 60126, United States",
                place.address
        )
    }

    @Test
    fun emptySearchResults() =
            assertTrue(jsonToSearchResults(readFile("mapbox/empty_search.json")).isEmpty())
}