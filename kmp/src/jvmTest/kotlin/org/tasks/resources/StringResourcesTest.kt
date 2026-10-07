package org.tasks.resources

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StringResourcesTest {
    @Test
    fun suspendLookupsGoThroughWrappers() {
        val root = File("..").canonicalFile
        val wrappers = File("src/commonMain/kotlin/org/tasks/resources/StringResources.kt").canonicalFile
        val pattern = Regex("""org\.jetbrains\.compose\.resources\.(getString|getPluralString|getStringArray)\b""")
        val offenders = listOf("kmp", "app", "composeApp", "wear")
            .map { File(root, "$it/src") }
            .filter { it.isDirectory }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" } }
            .filter { it.canonicalFile != wrappers }
            .filter { pattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(root).path }
            .sorted()
        assertEquals(emptyList<String>(), offenders)
    }
}
