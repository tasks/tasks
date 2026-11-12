package org.tasks.filters

import org.tasks.billing.PurchaseState
import org.tasks.compose.components.iconOr
import org.tasks.data.entity.TagData
import org.tasks.themes.TasksIcons
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterIconTest {
    private val pro = object : PurchaseState {
        override val hasTasksAccount = false
        override val hasPro = true
        override val hasTasksSubscription = false
    }

    @Test
    fun aKnownIconIsUsed() {
        assertEquals("pets", TagFilter(TagData(icon = "pets")).getIcon(pro))
    }

    @Test
    fun aLegacyIconNumberFallsBackToTheTypeIcon() {
        assertEquals(TasksIcons.LABEL, TagFilter(TagData(icon = "3")).getIcon(pro))
    }

    @Test
    fun anUnknownIconNameFallsBackToTheTypeIcon() {
        assertEquals(TasksIcons.LABEL, TagFilter(TagData(icon = "not_an_icon")).getIcon(pro))
    }

    @Test
    fun iconOrFallsBackForUnknownNames() {
        assertEquals("pets", "pets".iconOr(TasksIcons.LIST))
        assertEquals(TasksIcons.LIST, "3".iconOr(TasksIcons.LIST))
        assertEquals(TasksIcons.LIST, null.iconOr(TasksIcons.LIST))
    }
}
