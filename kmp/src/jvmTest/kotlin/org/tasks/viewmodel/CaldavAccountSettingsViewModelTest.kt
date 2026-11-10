package org.tasks.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.tasks.DatabaseTest
import org.tasks.MainDispatcherRule
import org.tasks.data.entity.CaldavAccount

class CaldavAccountSettingsViewModelTest : DatabaseTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val caldavDao = db.caldavDao()
    private val deletionDao = db.deletionDao()
    private val viewModels = mutableListOf<CaldavAccountSettingsViewModel>()

    @After
    fun cancelViewModels() = viewModels.forEach { it.viewModelScope.cancel() }

    @Test
    fun `editor reads the account from the database`() = runBlocking {
        val account = insertAccount()
        caldavDao.update(account.copy(name = "renamed", username = "renamed@example.com"))

        val viewModel = viewModel()
        viewModel.setAccountId(account.id)
        viewModel.awaitAccount()

        assertEquals("renamed", viewModel.state.value.displayName)
        assertEquals("renamed@example.com", viewModel.state.value.username)
    }

    @Test
    fun `a rename while editing does not discard what was typed`() = runBlocking {
        val account = insertAccount()
        val viewModel = viewModel()
        viewModel.setAccountId(account.id)
        viewModel.awaitAccount()

        viewModel.setDisplayName("typed")
        caldavDao.update(account.copy(name = "renamed"))
        withTimeout(TIMEOUT) { viewModel.state.first { it.account?.name == "renamed" } }

        assertEquals("typed", viewModel.state.value.displayName)
    }

    @Test
    fun `saving an account that was deleted does not add a new one`() = runBlocking {
        val account = insertAccount()
        val viewModel = viewModel()
        viewModel.setAccountId(account.id)
        viewModel.awaitAccount()

        deletionDao.delete(account) { }
        withTimeout(TIMEOUT) { viewModel.state.first { it.account == null } }

        val saved = CompletableDeferred<Unit>()
        viewModel.save { saved.complete(Unit) }
        withTimeout(TIMEOUT) { saved.await() }

        assertEquals(emptyList<CaldavAccount>(), caldavDao.getAccounts(CaldavAccount.TYPE_CALDAV))
    }

    private fun viewModel() = createViewModel().also { viewModels.add(it) }

    private fun createViewModel() = CaldavAccountSettingsViewModel(
        caldavDao = caldavDao,
        caldavClientProvider = mock(),
        encryption = mock(),
        taskDeleter = mock(),
        reporting = mock(),
        tagMetadataSync = mock(),
        tagMetadataActivation = mock(),
    )

    private suspend fun insertAccount(): CaldavAccount {
        val account = CaldavAccount(
            uuid = "account-uuid",
            name = "CalDAV",
            url = "https://example.com/dav/",
            username = "user@example.com",
            password = "encrypted",
            accountType = CaldavAccount.TYPE_CALDAV,
        )
        return caldavDao.getAccount(caldavDao.insert(account))!!
    }

    private suspend fun CaldavAccountSettingsViewModel.awaitAccount() {
        withTimeout(TIMEOUT) { state.first { it.account != null } }
    }

    companion object {
        private const val TIMEOUT = 5_000L
    }
}
