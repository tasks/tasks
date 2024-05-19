package org.tasks.data.db

import org.tasks.data.db.DbUtils.MAX_SQLITE_ARGS
import org.tasks.data.db.DbUtils.dbchunk

object SuspendDbUtils {
    suspend fun <T> Iterable<T>.eachChunk(action: suspend (List<T>) -> Unit) =
            eachChunk(MAX_SQLITE_ARGS, action)

    suspend fun <T> Iterable<T>.eachChunk(size: Int, action: suspend (List<T>) -> Unit) =
            chunked(size).forEach { action(it) }

    suspend fun <T, R> Iterable<T>.chunkedMap(transform: suspend (List<T>) -> Iterable<R>): List<R> =
            dbchunk().flatMap { transform(it) }
}