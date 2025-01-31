package org.tasks.data.dao

import androidx.room.Dao
import androidx.room.Transaction
import co.touchlab.kermit.Logger
import org.tasks.data.db.Database
import org.tasks.data.entity.Task
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@Dao
abstract class CompletionDao(private val db: Database) {
    @Transaction
    open suspend fun complete(
        tasks: List<Task>,
        completionDate: Long,
        afterSave: suspend (List<Task>) -> Unit,
    ) {
        Logger.d("CompletionDao") { "complete tasks=$tasks completionDate=$completionDate" }
        val modified = currentTimeMillis()
        val updated = tasks
            .map {
                it.copy(
                    completionDate = completionDate,
                    modificationDate = modified,
                )
            }
        db.alarmDao().deleteSnoozed(tasks.map { it.id })
        db.taskDao().updateInternal(updated)
        afterSave(updated)
    }
}