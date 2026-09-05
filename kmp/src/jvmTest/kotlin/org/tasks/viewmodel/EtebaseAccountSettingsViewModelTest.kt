package org.tasks.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.EtebaseService
import org.tasks.etebase.EtebaseClient
import org.tasks.etebase.EtebaseClientProvider
import org.tasks.security.KeyStoreEncryption

@OptIn(ExperimentalCoroutinesApi::class)
class EtebaseAccountSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val dao = mock<CaldavDao>()
    private val provider = mock<EtebaseClientProvider>()
    private val encryption = mock<KeyStoreEncryption>()
    private val client = mock<EtebaseClient>()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel() = EtebaseAccountSettingsViewModel(dao, provider, encryption, mock(), mock())

    @Test fun hiddenDefaultAndPersistentIdentityAreSavedTogether() = runTest(dispatcher) {
        whenever(provider.forUrl(any(), any(), anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(client)
        whenever(client.getSession()).thenReturn("session")
        whenever(encryption.encrypt("session")).thenReturn("encrypted")
        val vm = viewModel()
        vm.setService(EtebaseService.SILENTSUITE)
        vm.setUsername("same@example.com")
        vm.setPassword("password")
        vm.setService(EtebaseService.SILENTSUITE)
        assertEquals("password", vm.state.value.password)
        var completed = false
        vm.save { completed = true }
        advanceUntilIdle()
        assertTrue(completed)
        val saved = argumentCaptor<CaldavAccount>()
        verify(dao).insert(saved.capture())
        assertEquals("https://server.silentsuite.io", saved.firstValue.url)
        assertTrue(saved.firstValue.isSilentSuite)
        assertEquals(CaldavAccount.TYPE_ETEBASE, saved.firstValue.accountType)
        assertTrue(saved.firstValue.needsPro)
        verify(provider).forUrl("https://server.silentsuite.io", "same@example.com", "password", null, true, saved.firstValue.uuid)
    }

    @Test fun selfHostedReloginPreservesProviderAccountAndScope() = runTest(dispatcher) {
        val account = CaldavAccount(id = 42, uuid = "existing", accountType = CaldavAccount.TYPE_ETEBASE,
            serverType = EtebaseService.SILENTSUITE.serverType, username = "same@example.com",
            url = "https://self.example/etebase/", name = "Existing", password = "encrypted")
        whenever(dao.watchAccount(42)).thenReturn(emptyFlow())
        whenever(encryption.decrypt("encrypted")).thenReturn("old-session")
        whenever(provider.forUrl(any(), any(), anyOrNull(), anyOrNull(), any(), anyOrNull())).thenReturn(client)
        whenever(client.getSession()).thenReturn("new-session")
        whenever(encryption.encrypt("new-session")).thenReturn("new-encrypted")
        val vm = viewModel()
        vm.setAccount(account)
        vm.setPassword("new-password")
        vm.setService(EtebaseService.ETESYNC) // A route default cannot rebrand an existing account.
        assertEquals(EtebaseService.SILENTSUITE, vm.state.value.service)
        assertTrue(vm.state.value.hasCustomUrl)
        vm.save {}
        advanceUntilIdle()
        verify(provider).forUrl(account.url!!, account.username!!, "new-password", null, true, account.uuid)
        val updated = argumentCaptor<CaldavAccount>()
        verify(dao).update(updated.capture())
        assertEquals(account.uuid, updated.firstValue.uuid)
        assertEquals(account.serverType, updated.firstValue.serverType)
        assertEquals(account.url, updated.firstValue.url)
    }

    @Test fun legacyAccountIsNotRebrandedByItsHostname() = runTest(dispatcher) {
        whenever(dao.watchAccount(1)).thenReturn(emptyFlow())
        val vm = viewModel()
        vm.setAccount(CaldavAccount(id = 1, accountType = CaldavAccount.TYPE_ETEBASE,
            url = "https://server.silentsuite.io"))
        assertEquals(EtebaseService.ETESYNC, vm.state.value.service)
        assertTrue(vm.state.value.hasCustomUrl)
    }
}
