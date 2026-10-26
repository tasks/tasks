package org.tasks.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.tasks.InMemoryDataStore
import org.tasks.preferences.TasksPreferences
import org.tasks.viewmodel.TaskEditPreferencesViewModel

/**
 * Smoke test: the production detail composable (with its internal
 * koinViewModel lookup, exactly as wired in App.kt) must compose and
 * render the top bar and the back-saves row. Catches a pane that fails
 * to compose or whose loaded-gate never opens.
 *
 * Note: this renders at root, so it does NOT reproduce the detail
 * pane-slot layout collapse that once made the body blank (commit
 * c349d91de5). That regression is guarded structurally by applying the
 * modifier in the same fillMaxSize+verticalScroll Column as the task
 * list options pane, and verified live.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
class TaskEditOptionsDetailSmokeTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `renders the top bar and the back button saves row`() {
        startKoin {
            modules(
                module {
                    single { TasksPreferences(InMemoryDataStore()) }
                    viewModel { TaskEditPreferencesViewModel(tasksPreferences = get()) }
                },
            )
        }
        runComposeUiTest {
            setContent {
                TaskEditOptionsDetail(onNavigateBack = {})
            }
            onNodeWithText("Edit screen options").assertIsDisplayed()
            onNodeWithText("Escape key saves task").assertIsDisplayed()
        }
    }
}
