package org.tasks.ai

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.http.UnauthorizedException
import org.tasks.preferences.TasksPreferences
import org.tasks.preferences.fragments.AiTaskCreationViewModel
import org.tasks.security.KeyProvider
import org.tasks.security.KeyStoreEncryption
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_key_invalid
import tasks.kmp.generated.resources.ai_no_compatible_models
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@OptIn(ExperimentalCoroutinesApi::class)
class AiTaskCreationViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class FakeProvider(
        var models: List<ModelInfo> = emptyList(),
        var error: Throwable? = null,
    ) : OpenRouterClientProvider {
        var validateCalls = 0
        override suspend fun getService(): OpenRouterService? = null
        override suspend fun validateKey(rawKey: String): List<ModelInfo> {
            validateCalls++
            error?.let { throw it }
            return models
        }
    }

    private lateinit var preferences: TasksPreferences
    private lateinit var credentials: AiCredentialStore
    private lateinit var gate: AiGate
    private lateinit var provider: FakeProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        preferences = TasksPreferences(InMemoryDataStore())
        credentials = AiCredentialStore(
            preferences,
            KeyStoreEncryption(object : KeyProvider {
                private val key: SecretKey =
                    KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

                override fun getKey() = key
            })
        )
        gate = AiGate(preferences, credentials)
        provider = FakeProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        AiTaskCreationViewModel(gate, credentials, preferences, provider)

    /**
     * [KeyStoreEncryption] hops to [Dispatchers.Default], which the test scheduler does not
     * control, so draining the test dispatcher alone is not enough to observe a stored key.
     */
    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            dispatcher.scheduler.advanceUntilIdle()
            if (condition()) return
            Thread.sleep(10)
        }
        dispatcher.scheduler.advanceUntilIdle()
        if (!condition()) throw AssertionError("Timed out waiting for view model state")
    }

    private fun model(id: String, structured: Boolean) = ModelInfo(
        id = id,
        name = id,
        supportedParameters = if (structured) listOf("structured_outputs") else emptyList(),
    )

    @Test
    fun togglingOnWhileUnconsentedRaisesDialogAndDoesNotEnable() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnabledChange(true)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.showDisclosure.value)
        assertFalse(vm.state.value.enabled)
        assertFalse(preferences.get(TasksPreferences.aiTaskCreationEnabled, false))
    }

    @Test
    fun acceptingDisclosureSetsConsentAndEnables() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnabledChange(true)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onDisclosureAccepted()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.showDisclosure.value)
        assertTrue(vm.state.value.enabled)
        assertTrue(gate.hasConsent())
        assertTrue(preferences.get(TasksPreferences.aiTaskCreationEnabled, false))
    }

    @Test
    fun dismissingDisclosureLeavesFeatureOff() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnabledChange(true)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onDisclosureDismissed()

        assertFalse(vm.showDisclosure.value)
        assertFalse(vm.state.value.enabled)
        assertFalse(gate.hasConsent())
        assertFalse(preferences.get(TasksPreferences.aiTaskCreationEnabled, false))
    }

    @Test
    fun invalidKeySetsErrorAndStoresNothing() = runTest(dispatcher) {
        provider.error = UnauthorizedException("nope")
        val vm = viewModel()
        vm.onKeyChange("sk-bad")
        vm.onKeySubmit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Res.string.ai_key_invalid, vm.state.value.keyError)
        assertFalse(credentials.hasApiKey())
        assertTrue(vm.state.value.models.isEmpty())
    }

    @Test
    fun validKeyStoresKeyAndFiltersToStructuredOutputModels() = runTest(dispatcher) {
        provider.models = listOf(
            model("vendor/plain", structured = false),
            model("vendor/structured:free", structured = true),
        )
        val vm = viewModel()
        vm.onKeyChange("sk-good")
        vm.onKeySubmit()
        awaitUntil { vm.state.value.selectedModel != null }

        assertNull(vm.state.value.keyError)
        assertEquals("sk-good", credentials.getApiKey())
        assertEquals(
            listOf("vendor/structured:free"),
            vm.state.value.models.map { it.id },
        )
        assertEquals("vendor/structured:free", vm.state.value.selectedModel)
        assertEquals(
            "vendor/structured:free",
            preferences.get(TasksPreferences.aiModel, ""),
        )
        assertTrue(vm.state.value.hasStoredKey)
    }

    @Test
    fun noCompatibleModelsSurfacesMessageAndStoresNothing() = runTest(dispatcher) {
        provider.models = listOf(model("vendor/plain", structured = false))
        val vm = viewModel()
        vm.onKeyChange("sk-good")
        vm.onKeySubmit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Res.string.ai_no_compatible_models, vm.message.value)
        assertFalse(credentials.hasApiKey())
        assertFalse(preferences.get(TasksPreferences.aiTaskCreationEnabled, false))
    }

    @Test
    fun freeModelIsPreferredWhenAutoSelecting() = runTest(dispatcher) {
        provider.models = listOf(
            model("aaa/paid", structured = true),
            model("zzz/free:free", structured = true),
        )
        val vm = viewModel()
        vm.onKeyChange("sk-good")
        vm.onKeySubmit()
        awaitUntil { vm.state.value.selectedModel != null }

        assertEquals("zzz/free:free", vm.state.value.selectedModel)
    }

    @Test
    fun blankKeyIsNotSubmitted() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyChange("   ")
        vm.onKeySubmit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, provider.validateCalls)
    }

    @Test
    fun disablingClearsThePreferenceWithoutTouchingConsent() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onEnabledChange(true)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onDisclosureAccepted()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onEnabledChange(false)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.enabled)
        assertFalse(preferences.get(TasksPreferences.aiTaskCreationEnabled, false))
        assertTrue(gate.hasConsent())
    }

    @Test
    fun selectingAModelPersistsIt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onModelSelected("vendor/other:free")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("vendor/other:free", vm.state.value.selectedModel)
        assertEquals("vendor/other:free", preferences.get(TasksPreferences.aiModel, ""))
    }
}
