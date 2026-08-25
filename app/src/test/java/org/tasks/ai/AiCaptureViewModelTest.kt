package org.tasks.ai

import com.todoroo.astrid.service.TaskCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.tasks.data.entity.Task
import org.tasks.ui.AiCaptureState
import org.tasks.ui.AiCaptureViewModel
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.ai_error_bad_response
import tasks.kmp.generated.resources.ai_error_network
import tasks.kmp.generated.resources.ai_error_no_tasks
import tasks.kmp.generated.resources.ai_error_rate_limited
import tasks.kmp.generated.resources.ai_error_unauthorized
import tasks.kmp.generated.resources.ai_error_unavailable
import tasks.kmp.generated.resources.ai_not_configured

@OptIn(ExperimentalCoroutinesApi::class)
class AiCaptureViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun parsed(title: String, priority: String? = null) = ParsedTask(
        title = title,
        priority = priority,
    )

    private fun taskCreator(): TaskCreator = mock {
        onBlocking { createWithValues(anyFilter(), any()) } doAnswer { invocation ->
            Task(title = invocation.getArgument<String?>(1))
        }
        onBlocking { persistNewTask(any()) } doAnswer { invocation ->
            invocation.getArgument(0)
        }
    }

    private fun anyFilter() = org.mockito.kotlin.anyOrNull<org.tasks.filters.Filter>()

    private fun parser(result: AiParseResult): AiTaskParser = mock {
        onBlocking { parse(any()) } doReturn result
        onBlocking { writableLists() } doReturn emptyList()
        onBlocking { knownTags() } doReturn emptyList()
    }

    private fun viewModel(
        result: AiParseResult,
        creator: TaskCreator = taskCreator(),
    ) = AiCaptureViewModel(parser(result), creator)

    @Test
    fun startsInInputState() = runTest(dispatcher) {
        assertEquals(AiCaptureState.Input, viewModel(AiParseResult.NotConfigured).state.value)
    }

    @Test
    fun multiTaskParseProducesOneCheckedItemPerTask() = runTest(dispatcher) {
        val vm = viewModel(
            AiParseResult.Success(listOf(parsed("Call dentist"), parsed("Buy milk")))
        )
        vm.submit("dentist and milk")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is AiCaptureState.Review)
        val items = (state as AiCaptureState.Review).items
        assertEquals(2, items.size)
        assertEquals(listOf("Call dentist", "Buy milk"), items.map { it.task.title })
        assertTrue(items.all { it.checked })
    }

    @Test
    fun confirmPersistsOnlyCheckedItems() = runTest(dispatcher) {
        val creator = taskCreator()
        val vm = viewModel(
            AiParseResult.Success(listOf(parsed("Call dentist"), parsed("Buy milk"))),
            creator,
        )
        vm.submit("dentist and milk")
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggle(1)
        val created = vm.confirm()

        assertEquals(listOf("Call dentist"), created.map { it.title })
        verify(creator).persistNewTask(any())
    }

    @Test
    fun confirmPersistsNothingWhenAllUnchecked() = runTest(dispatcher) {
        val creator = taskCreator()
        val vm = viewModel(AiParseResult.Success(listOf(parsed("Call dentist"))), creator)
        vm.submit("dentist")
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggle(0)
        val created = vm.confirm()

        assertTrue(created.isEmpty())
        verify(creator, never()).persistNewTask(any())
    }

    @Test
    fun confirmOutsideReviewPersistsNothing() = runTest(dispatcher) {
        val creator = taskCreator()
        val vm = viewModel(AiParseResult.NotConfigured, creator)

        assertTrue(vm.confirm().isEmpty())
        verify(creator, never()).persistNewTask(any())
    }

    @Test
    fun notConfiguredYieldsSettingsPointer() = runTest(dispatcher) {
        val vm = viewModel(AiParseResult.NotConfigured)
        vm.submit("anything")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            AiCaptureState.Error(Res.string.ai_not_configured),
            vm.state.value,
        )
    }

    @Test
    fun everyFailureMapsToItsOwnMessage() = runTest(dispatcher) {
        val expected = mapOf(
            AiFailure.UNAUTHORIZED to Res.string.ai_error_unauthorized,
            AiFailure.RATE_LIMITED to Res.string.ai_error_rate_limited,
            AiFailure.UNAVAILABLE to Res.string.ai_error_unavailable,
            AiFailure.BAD_RESPONSE to Res.string.ai_error_bad_response,
            AiFailure.NO_TASKS to Res.string.ai_error_no_tasks,
            AiFailure.NETWORK to Res.string.ai_error_network,
        )
        for ((failure, resource) in expected) {
            val vm = viewModel(AiParseResult.Failure(failure))
            vm.submit("anything")
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(failure.name, AiCaptureState.Error(resource), vm.state.value)
        }
    }

    @Test
    fun parsingNeverPersistsAnything() = runTest(dispatcher) {
        val creator = taskCreator()
        val vm = viewModel(AiParseResult.Success(listOf(parsed("Call dentist"))), creator)
        vm.submit("dentist")
        dispatcher.scheduler.advanceUntilIdle()

        verify(creator, never()).persistNewTask(any())
    }

    @Test
    fun openInEditorReturnsTheUnsavedTask() = runTest(dispatcher) {
        val creator = taskCreator()
        val vm = viewModel(AiParseResult.Success(listOf(parsed("Call dentist"))), creator)
        vm.submit("dentist")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Call dentist", vm.openInEditor(0)?.title)
        assertNull(vm.openInEditor(5))
        verify(creator, never()).persistNewTask(any())
    }

    @Test
    fun resetReturnsToInputAndClearsText() = runTest(dispatcher) {
        val vm = viewModel(AiParseResult.Success(listOf(parsed("Call dentist"))))
        vm.onInputChange("dentist")
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        vm.reset()

        assertEquals(AiCaptureState.Input, vm.state.value)
        assertEquals("", vm.input.value)
    }

    @Test
    fun toggleOutsideReviewIsANoop() = runTest(dispatcher) {
        val vm = viewModel(AiParseResult.NotConfigured)
        vm.toggle(0)

        assertEquals(AiCaptureState.Input, vm.state.value)
    }
}
