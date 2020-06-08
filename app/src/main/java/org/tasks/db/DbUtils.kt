package org.tasks.db

object DbUtils {
    const val MAX_SQLITE_ARGS = 990

    fun <T> Iterable<T>.dbchunk(): List<List<T>> = chunked(MAX_SQLITE_ARGS)

    fun <T> Iterable<T>.eachChunk(action: (List<T>) -> Unit) = dbchunk().forEach(action)

    fun <T, R> Iterable<T>.chunkedMap(transform: (List<T>) -> Iterable<R>): List<R> =
            dbchunk().flatMap(transform)
}