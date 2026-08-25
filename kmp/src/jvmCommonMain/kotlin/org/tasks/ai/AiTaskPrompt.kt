package org.tasks.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * OpenRouter's `strict: true` requires every property to appear in `required` and
 * `additionalProperties` to be false, so optional fields are typed `["string","null"]`
 * rather than omitted.
 */
val TASK_SCHEMA: JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("tasks") {
            put("type", "array")
            putJsonObject("items") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("title") { put("type", "string") }
                    putJsonObject("notes") { putJsonArray("type") { add("string"); add("null") } }
                    putJsonObject("list") { putJsonArray("type") { add("string"); add("null") } }
                    putJsonObject("tags") {
                        put("type", "array")
                        putJsonObject("items") { put("type", "string") }
                    }
                    putJsonObject("due") { putJsonArray("type") { add("string"); add("null") } }
                    putJsonObject("start") { putJsonArray("type") { add("string"); add("null") } }
                    putJsonObject("priority") { putJsonArray("type") { add("string"); add("null") } }
                    putJsonObject("recurrence") { putJsonArray("type") { add("string"); add("null") } }
                }
                putJsonArray("required") {
                    add("title"); add("notes"); add("list"); add("tags")
                    add("due"); add("start"); add("priority"); add("recurrence")
                }
                put("additionalProperties", false)
            }
        }
    }
    putJsonArray("required") { add("tasks") }
    put("additionalProperties", false)
}

/**
 * Builds the messages sent to OpenRouter. The transmitted payload is exactly [input] plus
 * [listNames] and [tagNames] — matching what the privacy disclosure states.
 */
fun buildMessages(
    input: String,
    listNames: List<String>,
    tagNames: List<String>,
    nowIso: String,
    timeZoneId: String,
): List<ChatMessage> {
    val lists = if (listNames.isEmpty()) {
        "(none)"
    } else {
        listNames.joinToString("\n") { "- $it" }
    }
    val tags = if (tagNames.isEmpty()) {
        "(none)"
    } else {
        tagNames.joinToString("\n") { "- $it" }
    }
    val system = """
        You turn a person's free-form request into structured to-do tasks.

        The current local date and time is $nowIso in time zone $timeZoneId. Resolve every
        relative expression ("tomorrow", "next Tuesday", "in two weeks") against that.

        Rules:
        - Split the request into separate tasks when the person describes multiple distinct
          items. Otherwise return exactly one task.
        - "title" is a short imperative task name with the scheduling language removed. It is
          not a transcript of what the person said.
        - "list" must be one of the available list names below, copied verbatim, or null.
        - "tags" must be drawn from the available tag names below, copied verbatim. Return an
          empty array rather than inventing a tag.
        - "due" and "start" are local-time ISO-8601: "YYYY-MM-DD", or "YYYY-MM-DDTHH:MM" when a
          time is stated. Always include the time whenever the person states one.
        - "priority" is one of "high", "medium", "low", or "none".
        - "recurrence" is a bare RFC-5545 RRULE such as "FREQ=WEEKLY;BYDAY=MO", or null.
        - Use null for anything the person did not state. Do not guess.

        Available lists:
        $lists

        Available tags:
        $tags
    """.trimIndent()

    return listOf(
        ChatMessage(role = "system", content = system),
        ChatMessage(role = "user", content = input),
    )
}
