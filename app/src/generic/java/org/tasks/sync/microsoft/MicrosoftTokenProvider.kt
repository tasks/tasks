package org.tasks.sync.microsoft

import org.tasks.auth.OAuthTokenStore
import org.tasks.auth.TasksOAuthClient
import org.tasks.data.dao.CaldavDao
import org.tasks.security.KeyStoreEncryption
import javax.inject.Inject

class MicrosoftTokenProvider @Inject constructor(
    encryption: KeyStoreEncryption,
    caldavDao: CaldavDao,
    oauthClient: TasksOAuthClient,
) : MicrosoftTokenSource by OAuthTokenStore(encryption, caldavDao, oauthClient).asMicrosoftTokenSource()
