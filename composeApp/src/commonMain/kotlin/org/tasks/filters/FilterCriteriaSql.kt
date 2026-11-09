package org.tasks.filters

import org.tasks.data.dao.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.data.entity.Task
import org.tasks.data.sql.UnaryCriterion

/**
 * Builds the executable SQL for a saved custom filter from its criteria rows
 * (port of `FilterSettingsActivity.Companion.sql` from the Android `app` module).
 *
 * The output is appended to the base task query, which selects from the tasks
 * table; hence the leading " WHERE ".
 */
val List<CriterionInstance>.sql: String
    get() {
        val sql = StringBuilder(" WHERE ")
        for (instance in this) {
            val value = instance.valueFromCriterion
            when (instance.type) {
                CriterionInstance.TYPE_ADD -> sql.append(" OR ")
                CriterionInstance.TYPE_SUBTRACT -> sql.append(" AND NOT ")
                CriterionInstance.TYPE_INTERSECT -> sql.append(" AND ")
                else -> Unit
            }

            // Special case for the all-tasks universe.
            val cSql = instance.criterion.sql
            if (instance.type == CriterionInstance.TYPE_UNIVERSE || cSql == null) {
                sql.append(activeAndVisible())
            } else {
                val subSql = cSql.replace("?", UnaryCriterion.sanitize(value!!)).trim()
                sql.append(Task.ID).append(" IN (").append(subSql).append(")")
            }
        }
        return sql.toString()
    }

/**
 * Serializes the `valuesForNewTasks` maps of the AND-joined criteria into the
 * `values` column format (port of `FilterSettingsActivity.Companion.values`).
 */
val List<CriterionInstance>.values: String
    get() {
        val values: MutableMap<String, Any> = HashMap()
        for (instance in this) {
            val value = instance.valueFromCriterion
            if (instance.criterion.valuesForNewTasks.isNotEmpty()
                && instance.type == CriterionInstance.TYPE_INTERSECT
            ) {
                for ((key, value1) in instance.criterion.valuesForNewTasks) {
                    values[key.replace("?", value!!)] = value1.toString().replace("?", value)
                }
            }
        }
        return mapToSerializedString(values)
    }
