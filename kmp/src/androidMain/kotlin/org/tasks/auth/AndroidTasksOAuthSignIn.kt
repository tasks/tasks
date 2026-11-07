package org.tasks.auth

import org.tasks.caldav.CaldavClientFactory
import org.tasks.data.dao.CaldavDao
import org.tasks.http.OkHttpClientFactory
import org.tasks.security.Encryption

fun OkHttpClientFactory.tasksOAuthSignIn(
    caldavDao: CaldavDao,
    encryption: Encryption,
    serverEnvironment: TasksServerEnvironment,
    clientFactory: CaldavClientFactory,
    purchaseClaim: PurchaseClaim,
    redirect: SchemeRedirect,
): TasksOAuthSignIn = TasksOAuthSignIn(
    caldavDao = caldavDao,
    encryption = encryption,
    serverEnvironment = serverEnvironment,
    clientFactory = clientFactory,
    purchaseClaim = purchaseClaim,
    oauthClient = oauthClient(foreground = true),
    redirect = redirect,
)
