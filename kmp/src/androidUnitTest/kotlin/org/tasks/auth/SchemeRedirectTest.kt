package org.tasks.auth

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.io.IOException
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SchemeRedirectTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val redirect = SchemeRedirect(context, googleScheme = GOOGLE_SCHEME)

    private class Authorization(
        val result: Deferred<RedirectResult>,
        val redirectUri: String?,
    )

    @Test
    fun googleComesBackToTheSchemeRegisteredForThisBuild() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)

        assertEquals("$GOOGLE_SCHEME:/oauth2redirect", authorization.redirectUri)
        assertEquals(
            "363426363175-test.apps.googleusercontent.com",
            redirect.client(OAuthProvider.GOOGLE).clientId,
        )

        authorization.result.cancelAndJoin()
    }

    @Test
    fun gitHubUsesTheClientRegisteredForItsOwnScheme() = runTest {
        val authorization = authorize(OAuthProvider.GITHUB)

        assertEquals(
            "org.tasks.github.a50fdbf3e289a7fb2fc6://oauth2redirect",
            authorization.redirectUri,
        )
        assertEquals("a50fdbf3e289a7fb2fc6", redirect.client(OAuthProvider.GITHUB).clientId)
        assertEquals(
            "/oauth/github-configuration",
            redirect.client(OAuthProvider.GITHUB).discoveryPath,
        )

        authorization.result.cancelAndJoin()
    }

    @Test
    fun appleComesBackToTheAppOnceTheServerHasTheFormPost() = runTest {
        val authorization = authorize(OAuthProvider.APPLE)

        assertEquals("org.tasks.apple://oauth2redirect", authorization.redirectUri)

        authorization.result.cancelAndJoin()
    }

    @Test
    fun googleTasksHasNoRegisteredRedirect() = runTest {
        assertFailsWith<IOException> {
            redirect.await(
                OAuthProvider.GOOGLE_TASKS,
                openUrl = {},
                authUrl = { "https://example.com" },
                expectedState = { STATE },
            )
        }
    }

    @Test
    fun theBrowserOpensInACustomTabOnTheAuthorizationUrl() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)

        val activity = launchRedirectActivity()

        assertEquals(
            AUTH_URL,
            shadowOf(activity.get()).peekNextStartedActivity().data.toString(),
        )

        authorization.result.cancelAndJoin()
    }

    @Test
    fun theCodeTheBrowserRedirectsBackWithFinishesTheAuthorization() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=c0de&state=st%2Fate")

        val result = authorization.result.await()
        assertEquals("c0de", result.code)
        assertEquals("st/ate", result.state)
        assertEquals("$GOOGLE_SCHEME:/oauth2redirect", result.redirectUri)
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun anErrorTheBrowserRedirectsBackWithFinishesTheAuthorization() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?error=access_denied")

        val result = authorization.result.await()
        assertNull(result.code)
        assertEquals("access_denied", result.error)
    }

    @Test
    fun aRedirectFromAnotherSignInReopensTheBrowserOnThisOne() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()
        shadowOf(activity.get()).nextStartedActivity

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=old&state=stale")

        assertTrue(authorization.result.isActive)
        assertFalse(activity.get().isFinishing)
        assertEquals(AUTH_URL, shadowOf(activity.get()).nextStartedActivity.data.toString())

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=c0de&state=st%2Fate")

        assertEquals("c0de", authorization.result.await().code)
    }

    @Test
    fun aRedirectToAnotherSchemeDoesNotEndTheAuthorization() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()

        activity.redirectBack("org.tasks.github.sponsor://oauth2redirect?code=c0de&state=st%2Fate")

        assertTrue(authorization.result.isActive)

        authorization.result.cancelAndJoin()
    }

    @Test
    fun aCodeWithoutStateDoesNotEndTheAuthorization() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=c0de")

        assertTrue(authorization.result.isActive)

        authorization.result.cancelAndJoin()
    }

    @Test
    fun dismissingTheBrowserCancelsTheAuthorization() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()

        activity.pause().resume()

        assertTrue(authorization.result.isCancelled)
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun startingAgainOpensTheBrowserOnTheNewAuthorization() = runTest {
        val first = authorize(OAuthProvider.GOOGLE)
        val activity = launchRedirectActivity()
        assertEquals(AUTH_URL, shadowOf(activity.get()).nextStartedActivity.data.toString())

        val second = authorize(OAuthProvider.GOOGLE, authUrl = SECOND_AUTH_URL)
        activity.pause()
        activity.newIntent(shadowOf(context).nextStartedActivity)
        activity.resume()

        assertEquals(
            SECOND_AUTH_URL,
            shadowOf(activity.get()).nextStartedActivity.data.toString(),
        )
        assertTrue(first.result.isCancelled)
        assertTrue(second.result.isActive)

        second.result.cancelAndJoin()
    }

    @Test
    fun aRedirectWithNothingWaitingDoesNotCrash() = runTest {
        val activity = launchRedirectActivity()

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=c0de&state=st")

        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun theReceiverWithNoPendingSignInOpensNoBrowser() = runTest {
        val activity = launchRedirectActivity()

        assertNull(shadowOf(activity.get()).peekNextStartedActivity()?.data)
        assertTrue(activity.get().isFinishing)
    }

    private fun TestScope.authorize(
        provider: OAuthProvider,
        authUrl: String = AUTH_URL,
    ): Authorization {
        var redirectUri: String? = null
        val result = async(UnconfinedTestDispatcher(testScheduler)) {
            redirect.await(
                provider,
                openUrl = { },
                authUrl = { uri ->
                    redirectUri = uri
                    authUrl
                },
                expectedState = { STATE },
            )
        }
        return Authorization(result, redirectUri)
    }

    private fun launchRedirectActivity(): ActivityController<OAuthRedirectActivity> {
        val intent = shadowOf(context).nextStartedActivity
            ?: Intent(context, OAuthRedirectActivity::class.java)
        return Robolectric.buildActivity(OAuthRedirectActivity::class.java, intent).setup()
    }

    private fun ActivityController<OAuthRedirectActivity>.redirectBack(uri: String) {
        pause()
        newIntent(Intent(Intent.ACTION_VIEW, uri.toUri()))
        resume()
    }

    companion object {
        private const val STATE = "st/ate"
        private const val GOOGLE_SCHEME = "com.googleusercontent.apps.363426363175-test"
        private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth?client_id=x"
        private const val SECOND_AUTH_URL =
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=x&try=2"
    }
}
