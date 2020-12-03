package org.tasks.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.auth.Configuration.Companion.GOOGLE_CONFIG
import javax.inject.Inject

class AuthorizationServiceProvider @Inject constructor(
        @ApplicationContext context: Context,
        authStateManager: AuthStateManager
){
    val google = AuthorizationService(
            context,
            authStateManager,
            Configuration(context, GOOGLE_CONFIG)
    )

    fun dispose() {
        google.dispose()
    }
}