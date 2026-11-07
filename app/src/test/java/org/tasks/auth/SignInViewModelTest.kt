package org.tasks.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.tasks.analytics.Firebase
import org.tasks.auth.SignInViewModel.Outcome
import org.tasks.auth.SignInViewModel.State
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.data.entity.CaldavAccount
import org.tasks.fcm.PushTokenManager
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val authorization = OAuthResult(accessToken = "token")
    private val account = CaldavAccount(name = "user@example.com")
    private val pushTokenManager = mock<PushTokenManager>()
    private val firebase = mock<Firebase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aCreatedAccountRegistersForPushAndFinishesOk() {
        val viewModel = viewModel(signIn())

        viewModel.signIn(OAuthProvider.GOOGLE)

        assertEquals(State.Finished(Outcome.Created), viewModel.state.value)
        verify(pushTokenManager).registerTokenForAccount(account)
    }

    @Test
    fun dismissingTheBrowserFinishesAsCancelled() {
        val viewModel = viewModel(signIn(authorize = { throw CancellationException("Sign in cancelled") }))

        viewModel.signIn(OAuthProvider.GOOGLE)

        assertEquals(State.Finished(Outcome.Cancelled), viewModel.state.value)
    }

    @Test
    fun aSupersededSignInFinishesWithoutReportingACancellation() {
        val viewModel = viewModel(signIn(authorize = { throw RedirectSupersededException() }))

        viewModel.signIn(OAuthProvider.GOOGLE)

        assertEquals(State.Finished(Outcome.Superseded), viewModel.state.value)
    }

    @Test
    fun aSecondSignInWhileTheBrowserIsOpenIsIgnored() {
        val signIn = signIn(authorize = { awaitCancellation() })
        val viewModel = viewModel(signIn)

        viewModel.signIn(OAuthProvider.GOOGLE)
        viewModel.signIn(OAuthProvider.GITHUB)

        assertEquals(State.Authorizing, viewModel.state.value)
        verifyBlocking(signIn, times(1)) { authorize(any(), any(), anyOrNull()) }
    }

    @Test
    fun paymentRequiredKeepsTheAuthorizationForARetry() {
        val signIn = signIn(createAccount = { throw PaymentRequiredException() })
        val viewModel = viewModel(signIn)

        viewModel.signIn(OAuthProvider.GITHUB)

        assertEquals(
            State.SubscriptionRequired(OAuthProvider.GITHUB, authorization),
            viewModel.state.value,
        )
    }

    @Test
    fun startingAPurchaseIsRememberedSoItIsNotStartedTwice() {
        val viewModel = viewModel(signIn(createAccount = { throw PaymentRequiredException() }))
        viewModel.signIn(OAuthProvider.GOOGLE)

        viewModel.purchaseStarted()

        assertEquals(
            State.SubscriptionRequired(OAuthProvider.GOOGLE, authorization, purchasing = true),
            viewModel.state.value,
        )
    }

    @Test
    fun aRetryAfterPurchaseReusesTheAuthorizationWithoutOpeningTheBrowserAgain() {
        var attempts = 0
        val signIn = signIn(createAccount = {
            if (attempts++ == 0) throw PaymentRequiredException() else account
        })
        val viewModel = viewModel(signIn)
        viewModel.signIn(OAuthProvider.GOOGLE)
        viewModel.purchaseStarted()

        viewModel.retryAfterPurchase()

        assertEquals(State.Finished(Outcome.Created), viewModel.state.value)
        verifyBlocking(signIn, times(1)) { authorize(any(), any(), anyOrNull()) }
        verifyBlocking(signIn, times(2)) { createAccount(eq(OAuthProvider.GOOGLE), eq(authorization)) }
    }

    @Test
    fun aRetryWithoutAPendingSubscriptionDoesNothing() {
        val signIn = signIn()
        val viewModel = viewModel(signIn)

        viewModel.retryAfterPurchase()

        assertEquals(State.Idle, viewModel.state.value)
        verifyBlocking(signIn, never()) { createAccount(any(), any()) }
    }

    @Test
    fun aPurchaseTokenInUseShowsTheAccountItBelongsTo() {
        val viewModel = viewModel(
            signIn(createAccount = { throw PurchaseTokenInUseException("u***@example.com") })
        )

        viewModel.signIn(OAuthProvider.GOOGLE)

        assertEquals(State.WrongAccount("u***@example.com"), viewModel.state.value)
    }

    @Test
    fun aFailureIsReportedAndFinishesWithItsMessage() {
        val error = IOException("boom")
        val viewModel = viewModel(signIn(authorize = { throw error }))

        viewModel.signIn(OAuthProvider.GOOGLE)

        assertEquals(State.Finished(Outcome.Failed("boom")), viewModel.state.value)
        verify(firebase).reportException(error)
    }

    private fun signIn(
        authorize: suspend () -> OAuthResult = { authorization },
        createAccount: suspend () -> CaldavAccount = { account },
    ): TasksOAuthSignIn = mock {
        onBlocking { authorize(any(), any(), anyOrNull()) } doSuspendableAnswer { authorize() }
        onBlocking { createAccount(any(), any()) } doSuspendableAnswer { createAccount() }
    }

    private fun viewModel(signIn: TasksOAuthSignIn) =
        SignInViewModel(signIn, pushTokenManager, firebase)
}
