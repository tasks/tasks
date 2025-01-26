package org.tasks.data

import androidx.room.RoomDatabase
import androidx.room.TransactionScope
import androidx.room.Transactor
import androidx.room.useWriterConnection

suspend fun <T> RoomDatabase.withTransaction(block: suspend TransactionScope<T>.() -> T): T =
    useWriterConnection { transactor ->
        transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) {
            block()
        }
    }
