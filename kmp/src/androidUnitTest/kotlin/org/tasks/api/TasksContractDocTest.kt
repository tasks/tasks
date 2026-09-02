package org.tasks.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TasksContractDocTest {
    private val doc: List<String> by lazy {
        val candidates = listOf(
            File("../CONTENT_PROVIDER.md"),
            File("CONTENT_PROVIDER.md"),
            File("app/../CONTENT_PROVIDER.md"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError(
                "CONTENT_PROVIDER.md not found from ${File(".").absolutePath}"
            )
        file.readLines()
    }

    @Test
    fun theReferenceDocumentsExactlyTheColumnsTheContractDeclares() {
        val documented = tablesByCollection(COLUMN_HEADER)
        TasksContract.COLLECTIONS.forEach { path ->
            assertEquals(
                "columns for /$path",
                TasksContract.columnsFor(path).sorted(),
                documented[path].orEmpty().sorted(),
            )
        }
    }

    @Test
    fun theReferenceDocumentsExactlyTheParametersTheContractDeclares() {
        val documented = tablesByCollection(PARAMETER_HEADER)
        TasksContract.COLLECTIONS.forEach { path ->
            assertEquals(
                "parameters for /$path",
                TasksContract.paramsFor(path).sorted(),
                documented[path].orEmpty().sorted(),
            )
        }
    }

    @Test
    fun everyEnumValueIsInTheReference() {
        val text = doc.joinToString("\n")
        val values = TasksContract.Tasks.PRIORITIES +
                TasksContract.Tasks.REPEAT_FROMS +
                TasksContract.Tasks.SORTS +
                TasksContract.Alarms.TYPES +
                TasksContract.Lists.ACCESS_LEVELS +
                TasksContract.Accounts.TYPES +
                TasksContract.Accounts.ERRORS
        values.forEach {
            assertTrue("'$it' is not in CONTENT_PROVIDER.md", text.contains("`$it`"))
        }
    }

    @Test
    fun everyEndpointIsInTheReference() {
        val text = doc.joinToString("\n")
        TasksContract.COLLECTIONS.forEach {
            assertTrue("/$it is not in CONTENT_PROVIDER.md", text.contains("/${TasksContract.VERSION}/$it"))
        }
        assertTrue(text.contains(TasksContract.AUTHORITY))
        assertTrue(text.contains(TasksContract.PERMISSION_READ))
        assertTrue(text.contains(TasksContract.PERMISSION_WRITE))
    }

    @Test
    fun everyParameterTheExamplesUseIsRealAndSpelledRight() {
        val text = doc.joinToString("\n")
        val used = (QUERY_PARAM.findAll(text) + BUILT_PARAM.findAll(text))
            .map { it.groupValues[1] }
            .toSet()
        val known = TasksContract.COLLECTIONS.flatMap { TasksContract.paramsFor(it) }.toSet() +
                TasksContract.PARAM_IF_MODIFIED_AT
        assertEquals(emptySet<String>(), used - known)
    }

    @Test
    fun theReferenceCarriesAChangelog() {
        assertTrue(doc.any { it.trim().equals("# Changelog", ignoreCase = true) })
    }

    private fun tablesByCollection(header: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        var section: String? = null
        var inTable = false
        doc.forEach { line ->
            when {
                line.startsWith("# ") -> {
                    section = SECTIONS[line.removePrefix("# ").trim()]
                    inTable = false
                }
                line.trimEnd() == header -> inTable = section != null
                inTable && line.startsWith("| ---") -> Unit
                inTable && line.startsWith("|") -> {
                    val cell = line.removePrefix("|").substringBefore("|")
                    NAME.findAll(cell)
                        .map { it.groupValues[1] }
                        .forEach { result.getOrPut(section!!) { mutableListOf() }.add(it) }
                }
                else -> inTable = false
            }
        }
        return result
    }

    companion object {
        private const val COLUMN_HEADER = "| Column | Type | W | Description |"
        private const val PARAMETER_HEADER = "| Parameter | Type | Description |"
        private val NAME = Regex("`([a-z_][a-z0-9_]*)`")
        private val QUERY_PARAM = Regex("[?&]([a-z_][a-z0-9_]*)=")
        private val BUILT_PARAM = Regex("\"([a-z_][a-z0-9_]*)=\\$")

        private val SECTIONS = mapOf(
            "Tasks" to TasksContract.Tasks.PATH,
            "Alarms" to TasksContract.Alarms.PATH,
            "Task tags" to TasksContract.TaskTags.PATH,
            "Lists" to TasksContract.Lists.PATH,
            "Tags" to TasksContract.Tags.PATH,
            "Places" to TasksContract.Places.PATH,
            "Accounts" to TasksContract.Accounts.PATH,
        )
    }
}
