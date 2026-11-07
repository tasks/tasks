package org.tasks.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

internal fun noHttpOAuthClient() = TasksOAuthClient(
    HttpClient(MockEngine { error("this test should not make HTTP calls") })
)
