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
import org.tasks.http.HttpException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val authorization = OAuthResult(accessToken = "token")
    private val account = CaldavAccount(name = "user@example.com")
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
    fun aCreatedAccountFinishesOk() {
        val viewModel = viewModel(signIn())

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.Finished(Outcome.Created), viewModel.state.value)
    }

    @Test
    fun dismissingTheBrowserFinishesAsCancelled() {
        val viewModel = viewModel(signIn(authorize = { throw CancellationException("Sign in cancelled") }))

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.Finished(Outcome.Cancelled), viewModel.state.value)
    }

    @Test
    fun aSupersededSignInFinishesWithoutReportingACancellation() {
        val viewModel = viewModel(signIn(authorize = { throw RedirectSupersededException() }))

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.Finished(Outcome.Superseded), viewModel.state.value)
    }

    @Test
    fun aSecondSignInWhileTheBrowserIsOpenIsIgnored() {
        val signIn = signIn(authorize = { awaitCancellation() })
        val viewModel = viewModel(signIn)

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)
        viewModel.signIn(OAuthProvider.GITHUB, loginHint = null)

        assertEquals(State.Authorizing, viewModel.state.value)
        verifyBlocking(signIn, times(1)) { authorize(any(), any(), anyOrNull()) }
    }

    @Test
    fun paymentRequiredKeepsTheAuthorizationForARetry() {
        val signIn = signIn(createAccount = { throw PaymentRequiredException() })
        val viewModel = viewModel(signIn)

        viewModel.signIn(OAuthProvider.GITHUB, loginHint = null)

        assertEquals(
            State.SubscriptionRequired(OAuthProvider.GITHUB, authorization),
            viewModel.state.value,
        )
    }

    @Test
    fun startingAPurchaseIsRememberedSoItIsNotStartedTwice() {
        val viewModel = viewModel(signIn(createAccount = { throw PaymentRequiredException() }))
        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

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
        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)
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

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.WrongAccount("u***@example.com"), viewModel.state.value)
    }

    @Test
    fun aFailureIsReportedAndFinishesWithItsMessage() {
        val error = IOException("boom")
        val viewModel = viewModel(signIn(authorize = { throw error }))

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.Finished(Outcome.Failed("boom")), viewModel.state.value)
        verify(firebase).reportException(error)
    }

    @Test
    fun aServerRejectionIsNotReportedAndFinishesWithItsStatus() {
        val viewModel = viewModel(signIn(createAccount = { throw HttpException(403, "forbidden") }))

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.Finished(Outcome.Failed("HTTP 403 - Forbidden")), viewModel.state.value)
        verify(firebase, never()).reportException(any(), any())
    }

    @Test
    fun signingInToADifferentAccountNamesTheExistingOneWithoutReporting() {
        val viewModel = viewModel(
            signIn(createAccount = { throw WrongAccountException("bob@example.com", OAuthProvider.GITHUB) })
        )

        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        assertEquals(State.DifferentAccount("bob@example.com", OAuthProvider.GITHUB), viewModel.state.value)
        verify(firebase, never()).reportException(any(), any())
    }

    @Test
    fun signingInAgainAfterADifferentAccountAuthorizesWithTheGivenProviderAndHint() {
        var created = 0
        val signIn = signIn(createAccount = {
            if (created++ == 0) throw WrongAccountException("bob@example.com", OAuthProvider.GITHUB)
            account
        })
        val viewModel = viewModel(signIn)
        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        viewModel.retry(OAuthProvider.GITHUB, "bob@example.com")

        verifyBlocking(signIn) { authorize(eq(OAuthProvider.GITHUB), any(), eq("bob@example.com")) }
        assertEquals(State.Finished(Outcome.Created), viewModel.state.value)
    }

    @Test
    fun signingInAgainWithoutADifferentAccountDoesNothing() {
        val signIn = signIn(createAccount = { throw PurchaseTokenInUseException("b**@e**.com") })
        val viewModel = viewModel(signIn)
        viewModel.signIn(OAuthProvider.GOOGLE, loginHint = null)

        viewModel.retry(OAuthProvider.GITHUB, "bob@example.com")

        verifyBlocking(signIn, times(1)) { authorize(any(), any(), anyOrNull()) }
        assertEquals(State.WrongAccount("b**@e**.com"), viewModel.state.value)
    }

    private fun signIn(
        authorize: suspend () -> OAuthResult = { authorization },
        createAccount: suspend () -> CaldavAccount = { account },
    ): TasksOAuthSignIn = mock {
        onBlocking { authorize(any(), any(), anyOrNull()) } doSuspendableAnswer { authorize() }
        onBlocking { createAccount(any(), any()) } doSuspendableAnswer { createAccount() }
    }

    private fun viewModel(signIn: TasksOAuthSignIn) =
        SignInViewModel(signIn, firebase)
}
