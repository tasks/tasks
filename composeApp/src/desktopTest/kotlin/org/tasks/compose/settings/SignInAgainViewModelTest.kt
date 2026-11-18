package org.tasks.compose.settings

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.tasks.auth.OAuthProvider
import org.tasks.auth.PaymentRequiredException
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.compose.accounts.Platform
import org.tasks.compose.accounts.SignInState
import org.tasks.http.HttpException
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import org.tasks.ui.DisplayableException
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.microsoft_tasks_permission_not_granted
import java.io.EOFException

@OptIn(ExperimentalCoroutinesApi::class)
class SignInAgainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val syncAdapters = mock<SyncAdapters>()
    private val calls = mutableListOf<Triple<Platform, OAuthProvider?, String?>>()
    private val cancelled = mutableListOf<Int>()
    private var result = CompletableDeferred<Unit>()
    private var setup: CompletableDeferred<Unit>? = null

    private val viewModel by lazy {
        SignInAgainViewModel(
            signInHandler = object : SignInHandler {
                override suspend fun signIn(
                    platform: Platform,
                    provider: OAuthProvider?,
                    loginHint: String?,
                    onAuthorized: () -> Unit,
                ) {
                    calls += Triple(platform, provider, loginHint)
                    val call = calls.size
                    try {
                        result.await()
                    } catch (e: CancellationException) {
                        cancelled += call
                        throw e
                    }
                    setup?.let {
                        onAuthorized()
                        withContext(NonCancellable) { it.await() }
                    }
                }
            },
            purchaseState = object : PurchaseState {
                override val hasTasksAccount = false
                override val hasPro = false
                override val hasTasksSubscription = false
            },
            syncAdapters = syncAdapters,
        )
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun tappingAgainAbandonsTheSignInStillWaitingOnTheBrowser() = runTest(dispatcher) {
        viewModel.signIn(Platform.MICROSOFT, loginHint = "user@example.com")
        runCurrent()
        result = CompletableDeferred()
        viewModel.signIn(Platform.MICROSOFT, loginHint = "user@example.com")
        runCurrent()

        assertEquals(2, calls.size)
        assertEquals(listOf(1), cancelled)
        assertEquals(SignInState.Loading, viewModel.signInState.value)
    }

    @Test
    fun cancellingTheProviderPromptSyncsNothing() = runTest(dispatcher) {
        viewModel.signIn(Platform.MICROSOFT)
        result.completeExceptionally(CancellationException("Sign in cancelled"))
        runCurrent()

        assertEquals(SignInState.Idle, viewModel.signInState.value)
        verify(syncAdapters, never()).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun cancellingTheSpinnerAbandonsTheSignIn() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG)
        runCurrent()

        viewModel.cancel()
        runCurrent()

        assertEquals(listOf(1), cancelled)
        assertEquals(SignInState.Idle, viewModel.signInState.value)
        verify(syncAdapters, never()).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun cancellingWhileTheAccountIsBeingSavedLetsItFinishAndSync() = runTest(dispatcher) {
        setup = CompletableDeferred()
        viewModel.signIn(Platform.TASKS_ORG)
        result.complete(Unit)
        runCurrent()
        assertEquals(SignInState.Finishing, viewModel.signInState.value)

        viewModel.cancel()
        runCurrent()
        assertEquals(SignInState.Finishing, viewModel.signInState.value)

        setup!!.complete(Unit)
        runCurrent()

        assertEquals(SignInState.Idle, viewModel.signInState.value)
        verify(syncAdapters).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun cancellingWhileTheAccountIsBeingSavedStillShowsItsFailure() = runTest(dispatcher) {
        setup = CompletableDeferred()
        viewModel.signIn(Platform.TASKS_ORG)
        result.complete(Unit)
        runCurrent()

        viewModel.cancel()
        setup!!.completeExceptionally(PaymentRequiredException())
        runCurrent()

        assertEquals(SignInState.Error.PaymentRequired, viewModel.signInState.value)
    }

    @Test
    fun tappingAgainWhileTheAccountIsBeingSavedIsIgnored() = runTest(dispatcher) {
        setup = CompletableDeferred()
        viewModel.signIn(Platform.TASKS_ORG)
        result.complete(Unit)
        runCurrent()

        viewModel.signIn(Platform.TASKS_ORG)
        runCurrent()

        assertEquals(1, calls.size)
        assertEquals(SignInState.Finishing, viewModel.signInState.value)
    }

    @Test
    fun aFailureIsLoggedWithoutTheAddress() = runTest(dispatcher) {
        val logged = mutableListOf<Pair<String, Throwable?>>()
        val writers = Logger.config.logWriterList
        Logger.setLogWriters(object : LogWriter() {
            override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                logged += message to throwable
            }
        })
        try {
            viewModel.signIn(Platform.MICROSOFT)
            result.completeExceptionally(
                IllegalStateException("No account", IllegalStateException("for user@example.com")),
            )
            runCurrent()
        } finally {
            Logger.setLogWriters(writers)
        }

        val (message, throwable) = logged.single()
        assertEquals(null, throwable)
        assertFalse(message.contains("user@example.com"))
        assertTrue(message.contains("for <email>"))
    }

    @Test
    fun cancellingAfterAFailureLeavesTheErrorOnScreen() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG)
        result.completeExceptionally(PaymentRequiredException())
        runCurrent()

        viewModel.cancel()

        assertEquals(SignInState.Error.PaymentRequired, viewModel.signInState.value)
    }

    @Test
    fun aFailureReportsItsMessageWithoutTheAddress() = runTest(dispatcher) {
        viewModel.signIn(Platform.MICROSOFT)
        result.completeExceptionally(IllegalStateException("No account for user@example.com"))
        runCurrent()

        assertEquals(
            SignInState.Error.Failed(
                message = "No account for user@example.com",
                report = "No account for <email>",
            ),
            viewModel.signInState.value,
        )
    }

    @Test
    fun anErrorWithoutAMessageSaysTheSignInFailedAndReportsItsType() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG)
        result.completeExceptionally(NullPointerException())

        assertEquals(
            SignInState.Error.Failed("Sign-in failed", report = "NullPointerException"),
            viewModel.signInState.first { it != SignInState.Loading },
        )
    }

    @Test
    fun aDroppedConnectionWithoutAMessageSaysItCouldNotConnectAndReportsItsType() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG)
        result.completeExceptionally(EOFException())

        assertEquals(
            SignInState.Error.Failed(
                "Could not connect",
                report = "EOFException",
            ),
            viewModel.signInState.first { it != SignInState.Loading },
        )
    }

    @Test
    fun aMissingGrantReportsTheStringKeyRatherThanTheTranslation() = runTest(dispatcher) {
        viewModel.signIn(Platform.MICROSOFT)
        result.completeExceptionally(
            DisplayableException(Res.string.microsoft_tasks_permission_not_granted),
        )

        assertEquals(
            SignInState.Error.Failed(
                "Microsoft To Do permission was not granted. Please sign in again and grant access to Microsoft To Do on the consent screen.",
                report = "microsoft_tasks_permission_not_granted",
            ),
            viewModel.signInState.first { it != SignInState.Loading },
        )
    }

    @Test
    fun signingInAgainSyncsTheAccount() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG, OAuthProvider.GITHUB)
        result.complete(Unit)
        runCurrent()

        assertEquals(SignInState.Idle, viewModel.signInState.value)
        verify(syncAdapters).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun aFailureIsShownAndNothingSyncs() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG, OAuthProvider.GOOGLE)
        result.completeExceptionally(PurchaseTokenInUseException("b**@e**.com"))
        runCurrent()

        assertEquals(SignInState.Error.AccountInUse("b**@e**.com"), viewModel.signInState.value)
        verify(syncAdapters, never()).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun aServerRejectionShowsItsStatusRatherThanTheRawResponse() = runTest(dispatcher) {
        viewModel.signIn(Platform.TASKS_ORG)
        result.completeExceptionally(HttpException(403, "<html>Forbidden</html>"))
        runCurrent()

        assertEquals(
            SignInState.Error.Failed("HTTP 403 - Forbidden", report = "http_403"),
            viewModel.signInState.value,
        )
    }

    @Test
    fun theButtonWorksAgainAfterAFailureIsDismissed() = runTest(dispatcher) {
        viewModel.signIn(Platform.GOOGLE_TASKS)
        result.completeExceptionally(IllegalStateException("access_denied"))
        runCurrent()
        assertEquals(
            SignInState.Error.Failed("access_denied", report = "access_denied"),
            viewModel.signInState.value,
        )

        viewModel.dismissError()
        result = CompletableDeferred()
        viewModel.signIn(Platform.GOOGLE_TASKS)
        runCurrent()

        assertEquals(2, calls.size)
    }
}
