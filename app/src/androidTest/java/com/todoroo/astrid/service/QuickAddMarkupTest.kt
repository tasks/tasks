/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service

import org.tasks.data.entity.Task
import com.todoroo.astrid.utility.TitleParser
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.dao.TagDataDao
import org.tasks.injection.InjectingTestCase
import org.tasks.injection.ProductionModule
import java.util.*
import javax.inject.Inject

@UninstallModules(ProductionModule::class)
@HiltAndroidTest
class QuickAddMarkupTest : InjectingTestCase() {
    private val tags = ArrayList<String>()
    @Inject lateinit var tagDataDao: TagDataDao
    
    private var task: Task? = null

    @Test
    fun testTags() {
        whenTitleIs("this #cool")
        assertTitleBecomes("this")
        assertTagsAre("cool")
        whenTitleIs("#cool task")
        assertTitleBecomes("task")
        assertTagsAre("cool")
        whenTitleIs("doggie #nice #cute")
        assertTitleBecomes("doggie")
        assertTagsAre("nice", "cute")
    }

    @Test
    fun testContexts() {
        whenTitleIs("eat @home")
        assertTitleBecomes("eat")
        assertTagsAre("home")
        whenTitleIs("buy oatmeal @store @morning")
        assertTitleBecomes("buy oatmeal")
        assertTagsAre("store", "morning")
        whenTitleIs("look @ me")
        assertTitleBecomes("look @ me")
        assertTagsAre()
    }

    // --- helpers
    @Test
    fun testPriorities() {
        whenTitleIs("eat !1")
        assertTitleBecomes("eat")
        assertPriority(Task.Priority.LOW)
        whenTitleIs("super cool!")
        assertTitleBecomes("super cool!")
        whenTitleIs("stay alive !4")
        assertTitleBecomes("stay alive")
        assertPriority(Task.Priority.HIGH)
    }

    @Test
    fun testMixed() {
        whenTitleIs("eat #food !2")
        assertTitleBecomes("eat")
        assertTagsAre("food")
        assertPriority(Task.Priority.MEDIUM)
    }

    private fun assertTagsAre(vararg expectedTags: String) {
        val expected = listOf(*expectedTags)
        assertEquals(expected.toString(), tags.toString())
    }

    private fun assertTitleBecomes(title: String) {
        assertEquals(title, task!!.title)
    }

    private fun whenTitleIs(title: String) = runBlocking {
        task = Task()
        task!!.title = title
        tags.clear()
        TitleParser.parse(tagDataDao, task!!, tags)
    }

    private fun assertPriority(priority: Int) {
        assertEquals(priority, task!!.priority)
    }
}