package org.tasks.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonErrorBodyTest {

    @Test
    fun readsTheErrorCode() {
        assertEquals(
            "insufficient_subscription",
            """{"error":"insufficient_subscription","provider":"stripe"}""".errorCode(),
        )
    }

    @Test
    fun readsAnyNamedField() {
        assertEquals(
            "b**@e**.com",
            """{"error":"purchase_token_in_use","existing_account":"b**@e**.com"}"""
                .jsonField("existing_account"),
        )
    }

    @Test
    fun aFieldThatIsNotThereIsNull() {
        assertNull("""{"error":"nope"}""".jsonField("existing_account"))
    }

    @Test
    fun aBodyThatIsNotAnObjectIsNull() {
        assertNull("""["error"]""".errorCode())
        assertNull(""""error"""".errorCode())
        assertNull("".errorCode())
    }

    @Test
    fun aBodyThatIsNotJsonIsNull() {
        assertNull("<html>502 Bad Gateway</html>".errorCode())
        assertNull("""{"error":""".errorCode())
    }

    @Test
    fun aFieldThatIsNotAStringIsNull() {
        assertNull("""{"error":{"code":"nested"}}""".errorCode())
        assertNull("""{"error":["a"]}""".errorCode())
    }

    @Test
    fun aNonStringScalarReadsAsItIsWritten() {
        assertEquals("402", """{"error":402}""".errorCode())
        assertEquals("true", """{"error":true}""".errorCode())
    }
}
