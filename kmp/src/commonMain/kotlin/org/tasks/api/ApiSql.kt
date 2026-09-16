package org.tasks.api

internal class SqlWhere {
    private val clauses = ArrayList<String>()
    val args = ArrayList<Any>()

    fun and(sql: String) = apply { clauses.add(sql) }

    fun and(sql: String, vararg values: Any) = apply {
        clauses.add(sql)
        args.addAll(values)
    }

    fun inLongs(column: String, values: List<Long>) = apply {
        if (values.isEmpty()) return@apply
        clauses.add("$column IN (${values.joinToString(",")})")
    }

    fun inInts(column: String, values: List<Int>) = apply {
        if (values.isEmpty()) return@apply
        clauses.add("$column IN (${values.joinToString(",")})")
    }

    fun likeAny(columns: List<String>, value: String) = apply {
        val pattern = "%${value.escapeLike()}%"
        clauses.add(columns.joinToString(" OR ", prefix = "(", postfix = ")") {
            "$it LIKE ? ESCAPE '\\'"
        })
        repeat(columns.size) { args.add(pattern) }
    }

    fun merge(other: SqlWhere) = apply {
        clauses.addAll(other.clauses)
        args.addAll(other.args)
    }

    fun sql(): String = if (clauses.isEmpty()) "" else clauses.joinToString(" AND ", prefix = " WHERE ")

    companion object {
        private fun String.escapeLike() = buildString(length) {
            this@escapeLike.forEach {
                when (it) {
                    '\\', '%', '_' -> append('\\').append(it)
                    else -> append(it)
                }
            }
        }
    }
}
