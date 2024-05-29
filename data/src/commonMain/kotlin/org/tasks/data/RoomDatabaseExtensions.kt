package org.tasks.data

import androidx.room.RoomDatabase
import androidx.room.TransactionScope
import androidx.room.Transactor
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement

suspend fun <T> RoomDatabase.withTransaction(block: suspend TransactionScope<T>.() -> T): T =
    useWriterConnection { transactor ->
        transactor.withTransaction(Transactor.SQLiteTransactionType.DEFERRED) {
            block()
        }
    }

suspend fun <T> RoomDatabase.rawQuery(query: String, block: (SQLiteStatement) -> T): T =
    useReaderConnection { transactor -> transactor.usePrepared(query) { block(it) } }
