/*
 * Copyright 2017 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tasks.auth

import net.openid.appauth.*
import java.util.concurrent.atomic.AtomicReference

class AuthStateManager {
    private val currentAuthState = AtomicReference<AuthState>()

    fun signOut() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        val currentState = current
        val clearedState = currentState.authorizationServiceConfiguration
                ?.let { AuthState(it) }
                ?: return
        if (currentState.lastRegistrationResponse != null) {
            clearedState.update(currentState.lastRegistrationResponse)
        }
        replace(clearedState)
    }

    val current: AuthState
        get() {
            if (currentAuthState.get() != null) {
                return currentAuthState.get()
            }
            val state = AuthState()
            return if (currentAuthState.compareAndSet(null, state)) {
                state
            } else {
                currentAuthState.get()
            }
        }

    fun replace(state: AuthState): AuthState {
        currentAuthState.set(state)
        return state
    }

    fun updateAfterAuthorization(
            response: AuthorizationResponse?,
            ex: AuthorizationException?
    ): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    fun updateAfterTokenResponse(
            response: TokenResponse?,
            ex: AuthorizationException?
    ): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    fun updateAfterRegistration(
            response: RegistrationResponse?,
            ex: AuthorizationException?
    ): AuthState {
        val current = current
        if (ex != null) {
            return current
        }
        current.update(response)
        return replace(current)
    }
}