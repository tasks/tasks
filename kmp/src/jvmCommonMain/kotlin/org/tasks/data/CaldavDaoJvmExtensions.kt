package org.tasks.data

import org.tasks.caldav.TasksAccountDataRepository
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount

suspend fun CaldavDao.getAccountForNewList(
    tasksAccountDataRepository: TasksAccountDataRepository,
): CaldavAccount? {
    val isTasksGuest = tasksAccountDataRepository.getAccountResponse()?.guest == true
    return getAccounts().firstOrNull { !it.isOpenTasks && !(it.isTasksOrg && isTasksGuest) }
}
