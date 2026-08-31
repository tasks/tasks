package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NucleusWindowsNotificationsTest {
    @Test
    fun leavesOrdinaryTextAlone() {
        assertEquals(
            "Water the plants",
            NucleusWindowsNotifications.dropCharactersXmlCannotCarry("Water the plants"),
        )
        assertEquals(
            "Ne pas oublier - 25 % é",
            NucleusWindowsNotifications.dropCharactersXmlCannotCarry("Ne pas oublier - 25 % é"),
        )
    }

    @Test
    fun leavesTheXmlMetacharactersForNucleus() {
        assertEquals("&<>\"'", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("&<>\"'"))
    }

    @Test
    fun dropsControlCharacters() {
        assertEquals("ab", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("a\u0000\u0007\u001Fb"))

        assertEquals("a\t\n\rb", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("a\t\n\rb"))
    }

    @Test
    fun keepsWellFormedSurrogatePairs() {
        assertEquals(
            "Water the plants 🪴",
            NucleusWindowsNotifications.dropCharactersXmlCannotCarry("Water the plants 🪴"),
        )
        assertEquals("👍🏽", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("👍🏽"))
    }

    @Test
    fun dropsUnpairedSurrogatesAndNoncharacters() {
        val truncated = "Water the plants 🪴".take(17 + 1)
        assertEquals("Water the plants ", NucleusWindowsNotifications.dropCharactersXmlCannotCarry(truncated))
        assertEquals("ab", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("a\uDD2Eb"))
        assertEquals("ab", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("a\uFFFE\uFFFFb"))

        assertEquals("a\uFFFDb", NucleusWindowsNotifications.dropCharactersXmlCannotCarry("a\uFFFDb"))
    }

    @Test
    fun readsTaskIdsOutOfATag() {
        assertEquals(7L, NucleusWindowsNotifications.taskIdFrom(" 7 "))
        assertEquals(null, NucleusWindowsNotifications.taskIdFrom("not-a-task"))
        assertEquals(null, NucleusWindowsNotifications.taskIdFrom(""))
    }

    @Test
    fun countsAHistoryEntryWithNoGroupAsOurs() {
        assertEquals(true, NucleusWindowsNotifications.postedByUs("tasks"))

        assertEquals(true, NucleusWindowsNotifications.postedByUs(""))
        assertEquals(true, NucleusWindowsNotifications.postedByUs("  "))

        assertEquals(false, NucleusWindowsNotifications.postedByUs("something-else"))
    }

    @Test
    fun spotsAnMsixInstallByWhereItRunsFrom() {
        assertEquals(
            true,
            NucleusWindowsNotifications.installedAsAPackage(
                mapOf("app.dir" to "C:\\Program Files\\WindowsApps\\TasksOrg_15.10.0.0_x64__abc\\bin")::get
            ),
        )
        assertEquals(
            true,
            NucleusWindowsNotifications.installedAsAPackage(
                mapOf("app.dir" to "C:\\PROGRAM FILES\\WINDOWSAPPS\\TasksOrg\\bin")::get
            ),
        )
    }

    @Test
    fun doesNotTakeAnUnpackagedInstallForAnMsix() {
        assertEquals(
            false,
            NucleusWindowsNotifications.installedAsAPackage(
                mapOf("app.dir" to "C:\\Users\\alex\\Downloads\\tasks-org\\bin")::get
            ),
        )
        assertEquals(
            false,
            NucleusWindowsNotifications.installedAsAPackage(emptyMap<String, String>()::get),
        )
    }

    @Test
    fun leavesTheAumidToWindowsWhenPackaged() {
        assertEquals(
            null,
            NucleusWindowsNotifications.aumid(
                packaged = true,
                property = mapOf(
                    "app.windows.userModelID" to "TasksOrg_181m5permztht!TasksOrg"
                )::get,
            ),
        )
    }

    @Test
    fun takesTheAumidFromTheLauncherWhenUnpackaged() {
        assertEquals(
            "TasksOrg_181m5permztht!TasksOrg",
            NucleusWindowsNotifications.aumid(
                packaged = false,
                property = mapOf(
                    "app.windows.userModelID" to "TasksOrg_181m5permztht!TasksOrg"
                )::get,
            ),
        )
    }

    @Test
    fun prefersAnExplicitAumidEitherWay() {
        listOf(true, false).forEach { packaged ->
            assertEquals(
                "override",
                NucleusWindowsNotifications.aumid(
                    packaged = packaged,
                    property = mapOf(
                        "tasks.aumid" to "override",
                        "app.windows.userModelID" to "TasksOrg_181m5permztht!TasksOrg",
                    )::get,
                ),
            )
        }
    }

    @Test
    fun answersNullRatherThanLetNucleusInventAnAumid() {
        assertEquals(
            null,
            NucleusWindowsNotifications.aumid(
                packaged = false,
                property = emptyMap<String, String>()::get,
            ),
        )
        assertEquals(
            null,
            NucleusWindowsNotifications.aumid(
                packaged = false,
                property = mapOf("tasks.aumid" to "  ", "app.windows.userModelID" to "")::get,
            ),
        )
    }

    @Test
    fun takesADeclaredExecutableTypeOverTheInstallPath() {
        assertEquals(
            true,
            NucleusWindowsNotifications.packagedMode(
                mapOf("nucleus.executable.type" to "appx")::get
            ),
        )
        assertEquals(
            false,
            NucleusWindowsNotifications.packagedMode(
                mapOf(
                    "nucleus.executable.type" to "exe",
                    "app.dir" to "C:\\Program Files\\WindowsApps\\TasksOrg\\app",
                )::get
            ),
        )
        assertEquals(
            true,
            NucleusWindowsNotifications.packagedMode(
                mapOf("app.dir" to "C:\\Program Files\\WindowsApps\\TasksOrg\\app")::get
            ),
        )
    }
}
