package org.tasks.themes

import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.compose.components.iconExists
import java.lang.reflect.Modifier

class TasksIconsTest {
    @Test
    fun everyConstantNamesAnIconInTheFont() {
        val names = TasksIcons::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.name to it.get(null) as String }
        assertTrue(names.size > 100)
        names.forEach { (constant, name) -> assertTrue("$constant = $name", iconExists(name)) }
    }
}
