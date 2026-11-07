package org.tasks.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppAuthTokenMigrationTest {

    private val tokenEndpoint = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    private val authorizationEndpoint = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
    private val scope = "user.read Tasks.ReadWrite openid offline_access email"
    private val clientId = "9d4babd5-e7ba-4286-ba4b-17274495a901"
    private val farFuture = 4_000_000_000_000L
    private val longAgo = 1_000_000_000_000L

    private fun storedBySignIn(
        refreshToken: String? = "\"the-refresh-token\"",
        responseRefreshToken: String? = "\"the-refresh-token\"",
        accessToken: String? = "\"the-access-token\"",
        expiresAt: String? = "$farFuture",
    ) = """
        {
          ${refreshToken?.let { "\"refreshToken\": $it," } ?: ""}
          "scope": "$scope",
          "lastAuthorizationResponse": {
            "request": {
              "configuration": {
                "authorizationEndpoint": "$authorizationEndpoint",
                "tokenEndpoint": "$tokenEndpoint",
                "discoveryDoc": {
                  "issuer": "https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0",
                  "authorization_endpoint": "$authorizationEndpoint",
                  "token_endpoint": "$tokenEndpoint"
                }
              },
              "clientId": "$clientId",
              "responseType": "code",
              "redirectUri": "msauth://org.tasks/8wnYBRqh5nnQgFzbIXfxXSs41xE%3D",
              "scope": "$scope",
              "state": "the-state",
              "codeVerifier": "the-verifier",
              "additionalParameters": {}
            },
            "state": "the-state",
            "code": "the-code",
            "additional_parameters": {}
          },
          "mLastTokenResponse": {
            "request": {
              "configuration": {
                "authorizationEndpoint": "$authorizationEndpoint",
                "tokenEndpoint": "$tokenEndpoint",
                "discoveryDoc": {
                  "issuer": "https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0",
                  "authorization_endpoint": "$authorizationEndpoint",
                  "token_endpoint": "$tokenEndpoint"
                }
              },
              "clientId": "$clientId",
              "grantType": "refresh_token",
              "refreshToken": "the-previous-refresh-token",
              "additionalParameters": {}
            },
            "token_type": "Bearer"
            ${accessToken?.let { ", \"access_token\": $it" } ?: ""}
            ${expiresAt?.let { ", \"expires_at\": $it" } ?: ""}
            ${responseRefreshToken?.let { ", \"refresh_token\": $it" } ?: ""},
            "scope": "$scope",
            "additionalParameters": {}
          }
        }
    """.trimIndent()

    @Test
    fun aCredentialStoredBySignInBecomesOneTheAppCanRefresh() {
        assertEquals(
            OAuthTokenData(
                accessToken = "the-access-token",
                refreshToken = "the-refresh-token",
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                expiresAt = farFuture,
            ),
            AppAuthTokenMigration.toOAuthTokenData(storedBySignIn()),
        )
    }

    @Test
    fun theTokenEndpointComesFromTheTokenRequestBecauseSignInNeverStoredAConfig() {
        assertEquals(
            tokenEndpoint,
            AppAuthTokenMigration.toOAuthTokenData(storedBySignIn())?.tokenEndpoint,
        )
    }

    @Test
    fun theConsumersTenantThatIssuedTheRefreshTokenIsKept() {
        assertEquals(
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
            AppAuthTokenMigration.toOAuthTokenData(storedBySignIn())?.tokenEndpoint,
        )
    }

    @Test
    fun aTopLevelConfigIsUsedWhenThereIsOne() {
        val withConfig = """
            {
              "refreshToken": "r",
              "config": { "tokenEndpoint": "https://example.com/token" },
              "mLastTokenResponse": { "request": { "clientId": "$clientId" } }
            }
        """.trimIndent()

        assertEquals(
            "https://example.com/token",
            AppAuthTokenMigration.toOAuthTokenData(withConfig)?.tokenEndpoint,
        )
    }

    @Test
    fun theRefreshTokenIsTakenFromTheTokenResponseWhenThereIsNoTopLevelOne() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(
            storedBySignIn(refreshToken = null, responseRefreshToken = "\"from-the-response\"")
        )

        assertEquals("from-the-response", migrated?.refreshToken)
    }

    @Test
    fun anUnexpiredAccessTokenIsKeptEvenWithoutARefreshToken() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(
            storedBySignIn(refreshToken = null, responseRefreshToken = null)
        )

        assertEquals("the-access-token", migrated?.accessToken)
        assertEquals("", migrated?.refreshToken)
    }

    @Test
    fun aCredentialWithNothingLeftToUseIsStillMigratedForTheTokenStoreToReject() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(
            storedBySignIn(refreshToken = null, responseRefreshToken = null, expiresAt = "$longAgo")
        )

        assertEquals(
            OAuthTokenData(
                accessToken = "the-access-token",
                refreshToken = "",
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                expiresAt = longAgo,
            ),
            migrated,
        )
    }

    @Test
    fun aCredentialWithoutAClientIdIsRefused() {
        val noClientId = """
            {
              "refreshToken": "r",
              "config": { "tokenEndpoint": "$tokenEndpoint" }
            }
        """.trimIndent()

        assertNull(AppAuthTokenMigration.toOAuthTokenData(noClientId))
    }

    @Test
    fun aCredentialWithoutATokenEndpointIsRefused() {
        val noEndpoint = """
            {
              "refreshToken": "r",
              "mLastTokenResponse": { "request": { "clientId": "$clientId" } }
            }
        """.trimIndent()

        assertNull(AppAuthTokenMigration.toOAuthTokenData(noEndpoint))
    }

    @Test
    fun aMissingAccessTokenIsTreatedAsExpired() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(storedBySignIn(accessToken = null))

        assertEquals("", migrated?.accessToken)
        assertEquals(0L, migrated?.expiresAt)
    }

    @Test
    fun anUnreadableExpiryIsTreatedAsUnknownRatherThanDroppingTheCredential() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(storedBySignIn(expiresAt = "null"))

        assertEquals("the-refresh-token", migrated?.refreshToken)
        assertEquals(0L, migrated?.expiresAt)
    }

    @Test
    fun aFieldOfTheWrongShapeIsIgnoredRatherThanDroppingTheCredential() {
        val migrated = AppAuthTokenMigration.toOAuthTokenData(
            storedBySignIn(refreshToken = "{}", expiresAt = "\"soon\"")
        )

        assertEquals("the-refresh-token", migrated?.refreshToken)
        assertEquals(0L, migrated?.expiresAt)
    }

    @Test
    fun aCredentialTheAppAlreadyOwnsIsNotMistakenForAnOldOne() {
        val alreadyMigrated = OAuthTokenData(
            accessToken = "a",
            refreshToken = "b",
            tokenEndpoint = tokenEndpoint,
            clientId = clientId,
            expiresAt = 1,
        ).serialize()

        assertNull(AppAuthTokenMigration.toOAuthTokenData(alreadyMigrated))
    }

    @Test
    fun somethingThatIsNotJsonIsRefused() {
        assertNull(AppAuthTokenMigration.toOAuthTokenData("not json at all"))
    }
}
