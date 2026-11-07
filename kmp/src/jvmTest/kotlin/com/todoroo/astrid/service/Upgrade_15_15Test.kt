@file:Suppress("ClassName")

package com.todoroo.astrid.service

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.tasks.auth.OAuthTokenData
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.security.Encryption
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Upgrade_15_15Test {

    private val plaintext = object : Encryption {
        override suspend fun encrypt(text: String): String = text
        override suspend fun decrypt(text: String?): String? = text
    }

    private val tokenEndpoint = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    private val clientId = "9d4babd5-e7ba-4286-ba4b-17274495a901"

    private val appAuthCredential = """
        {
          "refreshToken": "the-refresh-token",
          "mLastTokenResponse": {
            "request": {
              "configuration": { "tokenEndpoint": "$tokenEndpoint" },
              "clientId": "$clientId"
            },
            "access_token": "the-access-token",
            "expires_at": 1762000000000
          }
        }
    """.trimIndent()

    @OptIn(ExperimentalEncodingApi::class)
    private val idToken = "header." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        .encode("""{"sub":"the-subject","email":"alice@outlook.com"}""".toByteArray()) +
            ".signature"

    private val appAuthCredentialWithIdToken = """
        {
          "refreshToken": "the-refresh-token",
          "mLastTokenResponse": {
            "request": {
              "configuration": { "tokenEndpoint": "$tokenEndpoint" },
              "clientId": "$clientId"
            },
            "access_token": "the-access-token",
            "id_token": "$idToken",
            "expires_at": 1762000000000
          }
        }
    """.trimIndent()

    private val warnings = mutableListOf<String>()

    @Before
    fun captureLogs() {
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                if (severity >= Severity.Warn && tag == "Upgrade_15_15") {
                    warnings.add(message)
                }
            }
        })
    }

    @After
    fun restoreLogs() {
        Logger.setLogWriters(platformLogWriter())
    }

    private fun dao(vararg accounts: CaldavAccount): CaldavDao = mock {
        onBlocking { getAccounts(TYPE_MICROSOFT) } doReturn accounts.toList()
        accounts.forEach { account ->
            account.username?.let { onBlocking { getAccount(TYPE_MICROSOFT, it) } doReturn account }
        }
    }

    @Test
    fun anAccountStoredUnderItsEmailMovesOntoTheSubject() = runBlocking {
        val dao = dao(
            CaldavAccount(id = 1, username = "alice@outlook.com", password = appAuthCredentialWithIdToken)
        )

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao) { setUsername(1, "the-subject") }
    }

    @Test
    fun anAccountWithoutAnIdTokenKeepsItsUsername() = runBlocking {
        val dao = dao(CaldavAccount(id = 1, username = "alice@outlook.com", password = appAuthCredential))

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao, never()) { setUsername(any(), any()) }
    }

    @Test
    fun anAccountDoesNotTakeTheSubjectOfAnotherAccount() = runBlocking {
        val dao = dao(
            CaldavAccount(id = 1, username = "alice@outlook.com", password = appAuthCredentialWithIdToken),
            CaldavAccount(
                id = 2,
                username = "the-subject",
                password = OAuthTokenData("a", "r", tokenEndpoint, clientId, 1).serialize(),
            ),
        )

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao, never()) { setUsername(any(), any()) }
    }

    @Test
    fun anAppAuthCredentialIsRewrittenForTheTokenStore() = runBlocking {
        val dao = dao(CaldavAccount(id = 1, password = appAuthCredential))

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao) {
            setPassword(
                1,
                OAuthTokenData(
                    accessToken = "the-access-token",
                    refreshToken = "the-refresh-token",
                    tokenEndpoint = tokenEndpoint,
                    clientId = clientId,
                    expiresAt = 1762000000000,
                ).serialize(),
            )
        }
    }

    @Test
    fun aCredentialAlreadyInTheNewFormatIsLeftAlone() = runBlocking {
        val stored = OAuthTokenData("a", "r", tokenEndpoint, clientId, 1).serialize()
        val dao = dao(CaldavAccount(id = 1, password = stored))

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao, never()) { setPassword(any(), anyOrNull()) }
    }

    @Test
    fun anUnusableCredentialIsLeftForTheUserToSignInAgain() = runBlocking {
        val dao = dao(CaldavAccount(id = 1, password = "not json"))

        Upgrade_15_15(dao, plaintext).run()

        verifyBlocking(dao, never()) { setPassword(any(), anyOrNull()) }
    }

    @Test
    fun oneAccountFailingDoesNotStopTheOthersOrTheUpgrade() = runBlocking {
        val failing = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? =
                if (text == "explodes") throw IllegalStateException(text) else text
        }
        val dao = dao(
            CaldavAccount(id = 1, password = "explodes"),
            CaldavAccount(id = 2, password = appAuthCredential),
        )

        Upgrade_15_15(dao, failing).run()

        verifyBlocking(dao) { setPassword(eq(2L), anyOrNull()) }
    }

    @Test
    fun anAccountWithoutAPasswordIsSkippedWithoutDecrypting() = runBlocking {
        val decrypted = mutableListOf<String?>()
        val recording = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = text.also { decrypted.add(it) }
        }
        val dao = dao(CaldavAccount(id = 1, password = null), CaldavAccount(id = 2, password = ""))

        Upgrade_15_15(dao, recording).run()

        assertEquals(emptyList<String?>(), decrypted)
        assertEquals(emptyList<String>(), warnings)
    }

    @Test
    fun aCredentialThatCannotBeDecryptedIsLogged() = runBlocking {
        val undecryptable = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = null
        }
        val dao = dao(CaldavAccount(id = 1, password = appAuthCredential))

        Upgrade_15_15(dao, undecryptable).run()

        verifyBlocking(dao, never()) { setPassword(any(), anyOrNull()) }
        assertTrue(warnings.single().contains("account 1"))
    }

    @Test
    fun aCredentialThatCannotBeReEncryptedIsLogged() = runBlocking {
        val unencryptable = object : Encryption {
            override suspend fun encrypt(text: String): String? = null
            override suspend fun decrypt(text: String?): String? = text
        }
        val dao = dao(CaldavAccount(id = 1, password = appAuthCredential))

        Upgrade_15_15(dao, unencryptable).run()

        verifyBlocking(dao, never()) { setPassword(any(), anyOrNull()) }
        assertTrue(warnings.single().contains("account 1"))
    }

    @Test
    fun anUnrecognizedCredentialIsLogged() = runBlocking {
        val dao = dao(CaldavAccount(id = 1, password = "not json"))

        Upgrade_15_15(dao, plaintext).run()

        assertTrue(warnings.single().contains("account 1"))
    }

    @Test
    fun aFailureIsLogged() = runBlocking {
        val failing = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = throw IllegalStateException()
        }
        val dao = dao(CaldavAccount(id = 1, password = appAuthCredential))

        Upgrade_15_15(dao, failing).run()

        assertTrue(warnings.single().contains("account 1"))
    }

    @Test(expected = CancellationException::class)
    fun cancellationStopsTheMigration(): Unit = runBlocking {
        val cancelled = object : Encryption {
            override suspend fun encrypt(text: String): String = text
            override suspend fun decrypt(text: String?): String? = throw CancellationException()
        }
        val dao = dao(
            CaldavAccount(id = 1, password = appAuthCredential),
            CaldavAccount(id = 2, password = appAuthCredential),
        )

        try {
            Upgrade_15_15(dao, cancelled).run()
        } finally {
            verifyBlocking(dao, never()) { setPassword(any(), anyOrNull()) }
        }
    }
}
