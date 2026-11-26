package org.tasks.widget

import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.tasks.R
import org.tasks.ShortcutManager
import org.tasks.ShortcutManager.ShortcutResult
import org.tasks.analytics.Firebase

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutConfigViewModelTest {
    private val shortcut = mock<ShortcutInfoCompat>()
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
    fun aSecondSaveWhileTheRequestIsInFlightIsIgnored() {
        val pending = CompletableDeferred<ShortcutResult>()
        val shortcutManager = shortcutManager { pending.await() }
        val viewModel = ShortcutConfigViewModel(shortcutManager, firebase)

        viewModel.save(shortcut, launcherRequest = false)
        viewModel.save(shortcut, launcherRequest = false)

        verifyBlocking(shortcutManager, times(1)) { createShortcut(any(), any()) }
    }

    @Test
    fun theResultWaitsForACollector() = runBlocking {
        val result = ShortcutResult.Created(Intent())
        val viewModel = ShortcutConfigViewModel(shortcutManager { result }, firebase)

        viewModel.save(shortcut, launcherRequest = true)

        assertEquals(result, viewModel.results.first())
    }

    @Test
    fun aLauncherRequestIsLoggedWhenCreated() {
        val viewModel = ShortcutConfigViewModel(shortcutManager { ShortcutResult.Created(Intent()) }, firebase)

        viewModel.save(shortcut, launcherRequest = true)

        verify(firebase).logEvent(R.string.event_create_shortcut, R.string.param_type to ShortcutConfigViewModel.SOURCE)
    }

    @Test
    fun anInAppRequestIsLoggedWhenCreated() {
        val viewModel = ShortcutConfigViewModel(shortcutManager { ShortcutResult.Created(null) }, firebase)

        viewModel.save(shortcut, launcherRequest = false)

        verify(firebase).logEvent(R.string.event_create_shortcut, R.string.param_type to ShortcutConfigViewModel.SOURCE)
    }

    @Test
    fun aFailedLauncherRequestIsNotLogged() {
        val viewModel = ShortcutConfigViewModel(shortcutManager { ShortcutResult.Failed }, firebase)

        viewModel.save(shortcut, launcherRequest = true)

        verify(firebase, never()).logEvent(any<Int>(), any())
    }

    private fun shortcutManager(result: suspend () -> ShortcutResult) = mock<ShortcutManager> {
        onBlocking { createShortcut(any(), any()) } doSuspendableAnswer { result() }
    }
}
