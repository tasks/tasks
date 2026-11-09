package org.tasks.data.sql

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-string tests for [UnaryCriterion.sanitize] - the single chokepoint through which
 * every custom-filter criterion value is passed before it is spliced into executable SQL
 * (see `FilterCriteriaSql.sql`). No database is opened and no SQL is executed: the
 * assertions check the rendered SQL *text* for inertness, so these tests cannot affect
 * any data.
 *
 * The contract: a single quote in a value must be doubled so it stays inside the quoted
 * string literal and cannot terminate it.
 */
class UnaryCriterionSanitizeTest {

    private val title = Field("tasks.title")

    @Test
    fun aBareSingleQuoteIsDoubled() {
        assertEquals("''", UnaryCriterion.sanitize("'"))
    }

    @Test
    fun aQuoteInsideAValueIsDoubled() {
        assertEquals("don''t", UnaryCriterion.sanitize("don't"))
    }

    @Test
    fun severalQuotesAreEachDoubled() {
        assertEquals("a''b''c''", UnaryCriterion.sanitize("a'b'c'"))
    }

    @Test
    fun inputWithoutQuotesIsLeftAlone() {
        assertEquals("plain text", UnaryCriterion.sanitize("plain text"))
        assertEquals("", UnaryCriterion.sanitize(""))
        assertEquals("unicode 花园 ñ", UnaryCriterion.sanitize("unicode 花园 ñ"))
    }

    @Test
    fun aQuoteEscapeAttemptCannotTerminateTheStringLiteral() {
        // If the quote were not doubled, this payload would close the string literal
        // and the "DROP TABLE ..." would parse as a separate statement.
        val hostile = "x'; DROP TABLE tasks; --"
        val rendered = "'${UnaryCriterion.sanitize(hostile)}'"
        // The entire hostile input sits between the outer pair of quotes as data:
        // the first internal quote is doubled, so nothing after it is outside the literal.
        assertEquals("'x''; DROP TABLE tasks; --'", rendered)
    }

    @Test
    fun aStringCriterionRendersThroughTheSameSanitiser() {
        // Criterion.toString() renders the populated SQL (see Criterion). The
        // string value must come out of the criterion already sanitised.
        val eq = UnaryCriterion.eq(title, "don't")
        assertEquals("(tasks.title='don''t')", eq.toString())
    }
}
