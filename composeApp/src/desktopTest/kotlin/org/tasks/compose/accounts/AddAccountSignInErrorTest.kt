package org.tasks.compose.accounts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.tasks.auth.OAuthProvider
import org.tasks.auth.PaymentRequiredException
import org.tasks.auth.SignInHandler
import org.tasks.billing.PurchaseState
import org.tasks.caldav.PurchaseTokenInUseException
import org.tasks.compose.accounts.AddAccountViewModel.SignInState
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount

@OptIn(ExperimentalCoroutinesApi::class)
class AddAccountSignInErrorTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = MutableSharedFlow<List<CaldavAccount>>()
    private var failure: Exception = PaymentRequiredException()
    private var pro = false
    private var cloudTier = false

    private val caldavDao = mock<CaldavDao> {
        on { watchAccounts() } doReturn accounts
        onBlocking { getAccounts() } doReturn emptyList()
    }

    private val purchaseState = object : PurchaseState {
        override val hasTasksAccount: Boolean get() = false
        override val hasPro: Boolean get() = pro
        override val hasTasksSubscription: Boolean get() = cloudTier
    }

    private val signInHandler = object : SignInHandler {
        override suspend fun signIn(
            platform: Platform,
            provider: OAuthProvider?,
            openUrl: (String) -> Unit,
        ): Unit = throw failure
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aSupporterTierIsAskedToUpgradeRatherThanToldNothingWasFound() = runTest(dispatcher) {
        pro = true
        cloudTier = false

        assertEquals(SignInState.Error.UpgradeRequired, signIn())
    }

    @Test
    fun aCloudSubscriptionThatTheServerCannotFindIsAPaymentPrompt() = runTest(dispatcher) {
        pro = true
        cloudTier = true

        assertEquals(SignInState.Error.PaymentRequired, signIn())
    }

    @Test
    fun holdingNothingIsAPaymentPrompt() = runTest(dispatcher) {
        assertEquals(SignInState.Error.PaymentRequired, signIn())
    }

    @Test
    fun aConflictNamesTheAccountAndReportsNoAddress() = runTest(dispatcher) {
        failure = PurchaseTokenInUseException("b**@e**.com")

        val state = signIn()

        assertEquals(SignInState.Error.AccountInUse("b**@e**.com"), state)
        assertEquals("purchase_token_in_use", (state as SignInState.Error).report)
    }

    private fun kotlinx.coroutines.test.TestScope.signIn(): SignInState {
        val viewModel = AddAccountViewModel(caldavDao, purchaseState, signInHandler)
        viewModel.signIn(Platform.TASKS_ORG)
        runCurrent()
        return viewModel.signInState.value
    }
}
