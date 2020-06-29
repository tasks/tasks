package org.tasks.db

import org.tasks.db.DbUtils.dbchunk

object SuspendDbUtils {
    suspend fun <T> Iterable<T>.eachChunk(action: suspend (List<T>) -> Unit) =
            dbchunk().forEach { action.invoke(it) }

    suspend fun <T, R> Iterable<T>.chunkedMap(transform: suspend (List<T>) -> Iterable<R>): List<R> =
            dbchunk().flatMap { transform.invoke(it) }
}