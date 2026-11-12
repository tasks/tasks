package org.tasks.compose.accounts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.auth.OAuthProvider
import org.tasks.auth.PaymentRequiredException
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.data.dao.CaldavDao

@OptIn(ExperimentalCoroutinesApi::class)
class AddAccountSignInStateTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dismissingTheBrowserLeavesNoSpinnerBehind() = runTest {
        val viewModel = viewModel { throw CancellationException("Sign in cancelled") }

        viewModel.signIn(Platform.TASKS_ORG, OAuthProvider.GOOGLE, openUrl = {})

        assertEquals(AddAccountViewModel.SignInState.Idle, viewModel.signInState.value)
    }

    @Test
    fun aSignInThatFailsKeepsTheErrorOnScreen() = runTest {
        val viewModel = viewModel { throw PaymentRequiredException() }

        viewModel.signIn(Platform.TASKS_ORG, OAuthProvider.GOOGLE, openUrl = {})

        assertEquals(
            AddAccountViewModel.SignInState.Error.PaymentRequired,
            viewModel.signInState.value,
        )
    }

    private fun viewModel(signIn: suspend () -> Unit) = AddAccountViewModel(
        caldavDao = mock<CaldavDao> {
            onBlocking { getAccounts() } doReturn emptyList()
            on { watchAccounts() } doReturn MutableSharedFlow()
        },
        purchaseState = object : PurchaseState {
            override val hasTasksAccount = false
            override val hasPro = false
            override val hasTasksSubscription = false
        },
        signInHandler = object : SignInHandler {
            override suspend fun signIn(
                platform: Platform,
                provider: OAuthProvider?,
                openUrl: (String) -> Unit,
            ) = signIn()
        },
    )
}
