package org.tasks.auth

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.robolectric.shadows.ShadowAppTask
import org.tasks.billing.GitHubSponsorClient
import org.tasks.billing.GitHubSponsorOAuthClient
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
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
    fun aSponsorRestoreComesBackToItsOwnScheme() = runTest {
        val client = sponsorClient()
        val result = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val activity = launchRedirectActivity()

        val authUrl = shadowOf(activity.get()).nextStartedActivity.data!!

        assertEquals(
            "org.tasks.github.sponsor://oauth2redirect",
            authUrl.getQueryParameter("redirect_uri"),
        )
        assertEquals("Ov23limM74bOgBgCBrJ5", authUrl.getQueryParameter("client_id"))

        result.cancelAndJoin()
    }

    @Test
    fun reopeningAfterTheTabIsGoneOpensItAgainInACustomTab() = runTest {
        val client = sponsorClient()
        val callerOpened = mutableListOf<String>()
        val first = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val activity = launchRedirectActivity()
        val authUrl = shadowOf(activity.get()).nextStartedActivity.data.toString()
        activity.pause()

        val second = async(UnconfinedTestDispatcher(testScheduler)) {
            client.signIn { callerOpened.add(it) }
        }
        val reopen = shadowOf(context).nextStartedActivity
        activity.newIntent(reopen)
        activity.resume()

        assertEquals(ComponentName(context, OAuthRedirectActivity::class.java), reopen.component)
        assertEquals(authUrl, shadowOf(activity.get()).nextStartedActivity.data.toString())
        assertFalse(activity.get().isFinishing)
        assertTrue(first.isActive)
        assertTrue(second.isActive)
        assertTrue(callerOpened.isEmpty())

        first.cancelAndJoin()
        second.cancelAndJoin()
    }

    @Test
    fun reopeningBringsBackTheTabWhereTheUserLeftIt() = runTest {
        val browserTask = task(base = OAuthRedirectActivity::class.java.name, top = CUSTOM_TAB)
        val appTask = task(base = APP_ACTIVITY)
        appTasks(appTask, browserTask)
        val client = sponsorClient()
        val first = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val activity = launchRedirectActivity()
        shadowOf(activity.get()).nextStartedActivity
        activity.pause()

        val second = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }

        assertTrue(shadowOf(browserTask).hasMovedToFront())
        assertFalse(shadowOf(appTask).hasMovedToFront())
        assertNull(shadowOf(context).nextStartedActivity)
        assertTrue(second.isActive)

        first.cancelAndJoin()
        second.cancelAndJoin()
    }

    @Test
    fun aRedirectNobodyIsWaitingForReturnsToTheAppWithoutRestartingIt() = runTest {
        val appTask = task(base = APP_ACTIVITY)
        appTasks(task(base = OAuthRedirectActivity::class.java.name), appTask)
        val activity = launchRedirectActivity()

        activity.redirectBack("$GOOGLE_SCHEME:/oauth2redirect?code=c0de&state=st")

        assertTrue(shadowOf(appTask).hasMovedToFront())
        assertNull(shadowOf(activity.get()).nextStartedActivity)
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun reopeningWhenTheOAuthTaskContentsAreUnknownOpensTheTabAgain() = runTest {
        val browserTask = task(base = OAuthRedirectActivity::class.java.name, top = null)
        appTasks(task(base = APP_ACTIVITY), browserTask)
        val client = sponsorClient()
        val first = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val activity = launchRedirectActivity()
        val authUrl = shadowOf(activity.get()).nextStartedActivity.data.toString()
        activity.pause()

        val second = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val reopen = shadowOf(context).nextStartedActivity
        activity.newIntent(reopen)
        activity.resume()

        assertFalse(shadowOf(browserTask).hasMovedToFront())
        assertEquals(ComponentName(context, OAuthRedirectActivity::class.java), reopen.component)
        assertEquals(authUrl, shadowOf(activity.get()).nextStartedActivity.data.toString())
        assertTrue(first.isActive)
        assertTrue(second.isActive)

        first.cancelAndJoin()
        second.cancelAndJoin()
    }

    @Test
    fun aLateRedirectReturnsToTheTaskTheSignInStartedFrom() = runTest {
        val origin = task(base = APP_ACTIVITY)
        appTasks(origin)
        val signIn = authorize(OAuthProvider.GOOGLE)
        signIn.result.cancelAndJoin()
        val other = task(base = WIDGET_CONFIG)
        appTasks(task(base = OAuthRedirectActivity::class.java.name), other, origin)

        val activity = launchRedirectActivity()

        assertTrue(shadowOf(origin).hasMovedToFront())
        assertFalse(shadowOf(other).hasMovedToFront())
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun withoutAKnownOriginALateRedirectPrefersTheLauncherTask() = runTest {
        appTasks()
        authorize(OAuthProvider.GOOGLE).result.cancelAndJoin()
        shadowOf(context).nextStartedActivity
        registerLauncher(APP_ACTIVITY)
        val main = task(base = APP_ACTIVITY)
        val other = task(base = WIDGET_CONFIG)
        appTasks(task(base = OAuthRedirectActivity::class.java.name), other, main)

        val activity = launchRedirectActivity()

        assertTrue(shadowOf(main).hasMovedToFront())
        assertFalse(shadowOf(other).hasMovedToFront())
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun aTaskThatDisappearsBeforeItCanBeBroughtForwardFallsBackToLaunchingTheApp() = runTest {
        val launcher = registerLauncher(APP_ACTIVITY)
        val vanished = mock<ActivityManager.AppTask> {
            on { taskInfo } doReturn taskInfo(base = APP_ACTIVITY)
            on { moveToFront() } doThrow IllegalArgumentException("Unable to find task ID")
        }
        appTasks(task(base = OAuthRedirectActivity::class.java.name), vanished)
        val activity = Robolectric
            .buildActivity(OAuthRedirectActivity::class.java, Intent(context, OAuthRedirectActivity::class.java))
            .setup()

        assertEquals(launcher, shadowOf(activity.get()).nextStartedActivity?.component
            ?: shadowOf(context).nextStartedActivity?.component)
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun aReopenThatLosesTheRaceWithTheRedirectReturnsToTheApp() = runTest {
        val appTask = task(base = APP_ACTIVITY)
        appTasks(task(base = OAuthRedirectActivity::class.java.name), appTask)

        val activity = launchRedirectActivity()

        assertTrue(shadowOf(appTask).hasMovedToFront())
        assertNull(shadowOf(activity.get()).nextStartedActivity)
        assertTrue(activity.get().isFinishing)
    }

    @Test
    fun reopeningWithNothingWaitingOpensNothing() = runTest {
        redirect.reopen(openUrl = { error("should not open a browser") }, authUrl = AUTH_URL)

        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun closingTheBrowserEndsASponsorRestoreAsCancelled() = runTest {
        val client = sponsorClient()
        val result = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }
        val activity = launchRedirectActivity()
        shadowOf(activity.get()).nextStartedActivity

        activity.pause().resume()

        assertEquals(GitHubSponsorClient.VerifyResult.Cancelled, result.await())
    }

    @Test
    fun aSignInStartedDuringASponsorRestoreEndsItAsCancelled() = runTest {
        val client = sponsorClient()
        val result = async(UnconfinedTestDispatcher(testScheduler)) { client.signIn { } }

        val signIn = authorize(OAuthProvider.GOOGLE)

        assertEquals(GitHubSponsorClient.VerifyResult.Cancelled, result.await())
        assertTrue(signIn.result.isActive)
        signIn.result.cancelAndJoin()
    }

    @Test
    fun aSignInKeepsWaitingForTheBrowserHoweverLongItTakes() = runTest {
        val authorization = authorize(OAuthProvider.GOOGLE)

        advanceTimeBy(24 * 60 * 60 * 1000L)

        assertTrue(authorization.result.isActive)
        assertEquals(AUTH_URL, PendingRedirect.pendingAuthUrl())

        authorization.result.cancelAndJoin()
    }

    private fun TestScope.sponsorClient(redirect: OAuthRedirect = this@SchemeRedirectTest.redirect) =
        GitHubSponsorOAuthClient(
            verify = { _, _ -> error("should not reach verification") },
            redirect = redirect,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )

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

    @Test
    fun theBrowserOpensInItsOwnTaskSoFinishingReturnsToTheCaller() {
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, OAuthRedirectActivity::class.java),
            0,
        )

        assertEquals("${context.packageName}:oauth", info.taskAffinity)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, info.launchMode)
        assertTrue(info.flags and ActivityInfo.FLAG_AUTO_REMOVE_FROM_RECENTS != 0)
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

    private fun task(base: String, top: String? = base): ActivityManager.AppTask =
        ShadowAppTask.newInstance().also { task ->
            shadowOf(task).setTaskInfo(taskInfo(base, top))
        }

    @Suppress("DEPRECATION")
    private fun taskInfo(base: String, top: String? = base) =
        ActivityManager.RecentTaskInfo().apply {
            val id = nextTaskId.incrementAndGet()
            taskId = id
            persistentId = id
            baseActivity = ComponentName(context.packageName, base)
            baseIntent = Intent().setComponent(baseActivity)
            topActivity = top?.let { ComponentName(context.packageName, it) }
        }

    private fun registerLauncher(activity: String): ComponentName {
        val component = ComponentName(context.packageName, activity)
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
        return component
    }

    private fun appTasks(vararg tasks: ActivityManager.AppTask) {
        shadowOf(context.getSystemService(ActivityManager::class.java)).setAppTasks(tasks.toList())
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
        private val nextTaskId = AtomicInteger(1000)
        private const val WIDGET_CONFIG = "org.tasks.widget.WidgetConfigActivity"
        private const val STATE = "st/ate"
        private const val APP_ACTIVITY = "org.tasks.MainActivity"
        private const val CUSTOM_TAB = "org.chromium.chrome.browser.customtabs.CustomTabActivity"
        private const val GOOGLE_SCHEME = "com.googleusercontent.apps.363426363175-test"
        private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth?client_id=x"
        private const val SECOND_AUTH_URL =
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=x&try=2"
    }
}
