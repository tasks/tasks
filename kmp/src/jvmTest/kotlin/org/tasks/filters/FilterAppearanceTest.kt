package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData

class FilterAppearanceTest {
    private fun lists(
        name: String = "Personal",
        color: Int = 0xff0000,
        icon: String? = "list",
        ctag: String? = "ctag-1",
        lastSync: Long = 0,
        error: String? = "",
    ) = mapOf(
        "uuid-1" to CaldavFilter(
            calendar = CaldavCalendar(
                uuid = "uuid-1",
                name = name,
                color = color,
                icon = icon,
                ctag = ctag,
                lastSync = lastSync,
            ),
            account = CaldavAccount(uuid = "account-1", error = error, lastSync = lastSync),
        )
    )

    private fun tags(name: String = "home", color: Int? = 0, icon: String? = "label") = mapOf(
        "tag-1" to TagFilter(TagData(remoteId = "tag-1", name = name, color = color, icon = icon))
    )

    @Test
    fun aSyncThatOnlyMovesTheCtagChangesNothing() {
        assertEquals(
            lists().appearances(),
            lists(ctag = "ctag-2", lastSync = 1234, error = "unreachable").appearances(),
        )
    }

    @Test
    fun aRenamedListLooksDifferent() {
        assertNotEquals(lists().appearances(), lists(name = "Work").appearances())
    }

    @Test
    fun aRecolouredListLooksDifferent() {
        assertNotEquals(lists().appearances(), lists(color = 0x00ff00).appearances())
    }

    @Test
    fun aRelabelledListLooksDifferent() {
        assertNotEquals(lists().appearances(), lists(icon = "star").appearances())
    }

    @Test
    fun anAddedListLooksDifferent() {
        val second = mapOf(
            "uuid-2" to CaldavFilter(
                calendar = CaldavCalendar(uuid = "uuid-2", name = "Work"),
                account = CaldavAccount(uuid = "account-1"),
            )
        )

        assertNotEquals(lists().appearances(), (lists() + second).appearances())
    }

    @Test
    fun aRenamedTagLooksDifferent() {
        assertEquals(tags().appearances(), tags().appearances())
        assertNotEquals(tags().appearances(), tags(name = "work").appearances())
    }
}
