package org.tasks.auth

import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.tasksOrgAccount
import org.tasks.data.entity.CaldavAccount

val CaldavAccount.tasksOrgProvider: OAuthProvider
    get() = when {
        username?.startsWith("github") == true -> OAuthProvider.GITHUB
        username?.startsWith("apple") == true -> OAuthProvider.APPLE
        else -> OAuthProvider.GOOGLE
    }

data class TasksOrgSignIn(
    val provider: OAuthProvider?,
    val loginHint: String?,
)

fun tasksOrgSignIn(
    existing: CaldavAccount?,
    provider: OAuthProvider?,
    loginHint: String? = null,
): TasksOrgSignIn =
    existing
        ?.let { TasksOrgSignIn(it.tasksOrgProvider, it.name?.takeIf(String::isNotBlank)) }
        ?: TasksOrgSignIn(provider, loginHint)

suspend fun CaldavDao.resolveTasksOrgSignIn(
    provider: OAuthProvider?,
    loginHint: String? = null,
    account: CaldavAccount? = null,
): TasksOrgSignIn = tasksOrgSignIn(account ?: tasksOrgAccount(), provider, loginHint)
