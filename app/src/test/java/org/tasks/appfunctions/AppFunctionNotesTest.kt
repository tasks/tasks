package org.tasks.appfunctions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.api.AgentNotes
import java.io.File

class AppFunctionNotesTest {

    @Test
    fun theDescriptionListsExactlyTheSharedNotes() {
        val bullets = metadata
            .lineSequence()
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").removeSuffix("\" />") }
            .toList()

        assertEquals(AgentNotes.ALL, bullets)
    }

    private val metadata: String by lazy {
        val file = File("src/main/res/xml/app_metadata.xml")
        check(file.exists()) { "app_metadata.xml not found from ${File(".").absolutePath}" }
        file.readText()
    }
}
