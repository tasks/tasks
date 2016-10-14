/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.TestComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class QuickAddMarkupTest extends DatabaseTestCase {

    @Inject TaskCreator taskCreator;

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }

    @Test
    public void testTags() {
        whenTitleIs("this #cool");
        assertTitleBecomes("this");
        assertTagsAre("cool");

        whenTitleIs("#cool task");
        assertTitleBecomes("task");
        assertTagsAre("cool");

        whenTitleIs("doggie #nice #cute");
        assertTitleBecomes("doggie");
        assertTagsAre("nice", "cute");
    }

    @Test
    public void testContexts() {
        whenTitleIs("eat @home");
        assertTitleBecomes("eat");
        assertTagsAre("home");

        whenTitleIs("buy oatmeal @store @morning");
        assertTitleBecomes("buy oatmeal");
        assertTagsAre("store", "morning");

        whenTitleIs("look @ me");
        assertTitleBecomes("look @ me");
        assertTagsAre();
    }

    @Test
    public void testImportances() {
        whenTitleIs("eat !1");
        assertTitleBecomes("eat");
        assertImportanceIs(Task.IMPORTANCE_SHOULD_DO);

        whenTitleIs("super cool!");
        assertTitleBecomes("super cool!");

        whenTitleIs("stay alive !4");
        assertTitleBecomes("stay alive");
        assertImportanceIs(Task.IMPORTANCE_DO_OR_DIE);
    }

    @Test
    public void testMixed() {
        whenTitleIs("eat #food !2");
        assertTitleBecomes("eat");
        assertTagsAre("food");
        assertImportanceIs(Task.IMPORTANCE_MUST_DO);
    }

    // --- helpers

    private Task task;
    private final ArrayList<String> tags = new ArrayList<>();

    private void assertTagsAre(String... expectedTags) {
        List<String> expected = Arrays.asList(expectedTags);
        assertEquals(expected.toString(), tags.toString());
    }

    private void assertTitleBecomes(String title) {
        assertEquals(title, task.getTitle());
    }

    private void whenTitleIs(String title) {
        task = new Task();
        task.setTitle(title);
        tags.clear();
        taskCreator.parseQuickAddMarkup(task, tags);
    }

    private void assertImportanceIs(int importance) {
        assertEquals(importance, (int)task.getImportance());
    }
}
