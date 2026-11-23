package org.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.viewmodel.TaskEditViewModel

class MultilevelSubtaskLimitTest {
    private fun list(type: Int, uuid: String = "cal-1") = CaldavFilter(
        calendar = CaldavCalendar(uuid = uuid, name = "List"),
        account = CaldavAccount(uuid = "acct-1", accountType = type),
    )

    private fun state(
        parent: Long = 0,
        list: CaldavFilter? = null,
        originalList: CaldavFilter? = list,
        isDraft: Boolean = false,
        nested: Boolean = false,
    ) = TaskEditViewModel.State(
        isLoading = false,
        task = Task(id = 1, parent = parent),
        list = list,
        originalList = originalList,
        isDraft = isDraft,
        subtasksNested = nested,
    )

    @Test
    fun aTaskWithNoListYetNestsNothingAndSaysNothing() {
        assertNull(multilevelSubtaskLimit(state(parent = 3, list = null)))
    }

    @Test
    fun aTopLevelTaskCanAlwaysHaveSubtasks() {
        assertNull(multilevelSubtaskLimit(state(parent = 0, list = list(TYPE_GOOGLE_TASKS))))
    }

    @Test
    fun aCaldavSubtaskCanHaveSubtasksOfItsOwn() {
        assertNull(multilevelSubtaskLimit(state(parent = 3, list = list(TYPE_CALDAV))))
    }

    @Test
    fun aGoogleSubtaskIsAsDeepAsItGoes() {
        assertEquals(
            MultilevelSubtaskLimit.GoogleTasks,
            multilevelSubtaskLimit(state(parent = 3, list = list(TYPE_GOOGLE_TASKS))),
        )
    }

    @Test
    fun aMicrosoftSubtaskIsAsDeepAsItGoes() {
        assertEquals(
            MultilevelSubtaskLimit.Microsoft,
            multilevelSubtaskLimit(state(parent = 3, list = list(TYPE_MICROSOFT))),
        )
    }

    @Test
    fun anUnsavedSubtaskIsInTheSamePositionAsASavedOne() {
        assertEquals(
            MultilevelSubtaskLimit.GoogleTasks,
            multilevelSubtaskLimit(
                state(parent = 0, list = list(TYPE_GOOGLE_TASKS), isDraft = true)
            ),
        )
    }

    @Test
    fun movingTheTaskToAnotherListGivesItSubtasksBack() {
        assertNull(
            multilevelSubtaskLimit(
                state(
                    parent = 3,
                    list = list(TYPE_GOOGLE_TASKS, uuid = "cal-2"),
                    originalList = list(TYPE_GOOGLE_TASKS, uuid = "cal-1"),
                )
            )
        )
        assertNull(
            multilevelSubtaskLimit(
                state(
                    parent = 3,
                    list = list(TYPE_MICROSOFT),
                    originalList = list(TYPE_CALDAV),
                )
            )
        )
    }

    @Test
    fun stayingOnTheSameListStillSaysWhyThereIsNoAddRow() {
        assertEquals(
            MultilevelSubtaskLimit.Microsoft,
            multilevelSubtaskLimit(
                state(
                    parent = 3,
                    list = list(TYPE_MICROSOFT),
                    originalList = list(TYPE_MICROSOFT),
                )
            ),
        )
    }

    @Test
    fun aSingleLevelListWithNothingNestedHasNothingToFlatten() {
        assertFalse(flattensOnSave(state(list = list(TYPE_GOOGLE_TASKS))))
    }

    @Test
    fun aSingleLevelListFlattensTheNestingItIsGiven() {
        assertTrue(flattensOnSave(state(list = list(TYPE_GOOGLE_TASKS), nested = true)))
        assertTrue(flattensOnSave(state(list = list(TYPE_MICROSOFT), nested = true)))
    }

    @Test
    fun aListThatKeepsItsNestingFlattensNothing() {
        assertFalse(flattensOnSave(state(list = list(TYPE_CALDAV), nested = true)))
    }
}
