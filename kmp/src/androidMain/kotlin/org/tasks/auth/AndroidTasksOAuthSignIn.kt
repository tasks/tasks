package org.tasks.auth

import org.tasks.analytics.Analytics
import org.tasks.caldav.CaldavClientFactory
import org.tasks.data.dao.CaldavDao
import org.tasks.fcm.PushTokenManager
import org.tasks.http.OkHttpClientFactory
import org.tasks.security.Encryption

fun OkHttpClientFactory.tasksOAuthSignIn(
    caldavDao: CaldavDao,
    encryption: Encryption,
    serverEnvironment: TasksServerEnvironment,
    clientFactory: CaldavClientFactory,
    purchaseClaim: PurchaseClaim,
    pushTokenManager: PushTokenManager,
    analytics: Analytics,
    redirect: SchemeRedirect,
): TasksOAuthSignIn = TasksOAuthSignIn(
    caldavDao = caldavDao,
    encryption = encryption,
    serverEnvironment = serverEnvironment,
    clientFactory = clientFactory,
    purchaseClaim = purchaseClaim,
    pushTokenManager = pushTokenManager,
    analytics = analytics,
    oauthClient = oauthClient(foreground = true),
    redirect = redirect,
)
