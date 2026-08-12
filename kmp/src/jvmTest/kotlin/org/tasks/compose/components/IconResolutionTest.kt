package org.tasks.compose.components

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.themes.TasksIcons

class IconResolutionTest {
    @Test
    fun theWarningIconResolves() {
        assertNotNull(imageVectorByName(TasksIcons.WARNING))
    }

    @Test
    fun theSubtaskIconResolves() {
        assertNotNull(imageVectorByName(TasksIcons.SUBTASK))
    }

    @Test
    fun anIconThatDoesNotExistIsNull() {
        assertNull(imageVectorByName("no_such_icon_anywhere"))
    }
}
