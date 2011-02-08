package com.todoroo.astrid.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.data.Task;

public class QuickAddMarkupTest extends TodorooTestCase {

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

    public void testContexts() {
        whenTitleIs("eat @home");
        assertTitleBecomes("eat");
        assertTagsAre("@home");

        whenTitleIs("buy oatmeal @store @morning");
        assertTitleBecomes("buy oatmeal");
        assertTagsAre("@store", "@morning");

        whenTitleIs("look @ me");
        assertTitleBecomes("look @ me");
        assertTagsAre();
    }

    public void testImportances() {
        whenTitleIs("eat !1");
        assertTitleBecomes("eat");
        assertImportanceIs(Task.IMPORTANCE_NONE);

        whenTitleIs("super cool!");
        assertTitleBecomes("super cool!");

        whenTitleIs("stay alive !4");
        assertTitleBecomes("stay alive");
        assertImportanceIs(Task.IMPORTANCE_DO_OR_DIE);
    }

    public void testMixed() {
        whenTitleIs("eat #food !2");
        assertTitleBecomes("eat");
        assertTagsAre("food");
        assertImportanceIs(Task.IMPORTANCE_SHOULD_DO);
    }

    // --- helpers

    private Task task;
    private ArrayList<String> tags = new ArrayList<String>();

    private void assertTagsAre(String... tags) {
        List<String> expected = Arrays.asList(tags);
        assertEquals(expected.toString(), tags.toString());
    }

    private void assertTitleBecomes(String title) {
        assertEquals(title, task.getValue(Task.TITLE));
    }

    private void whenTitleIs(String title) {
        task = new Task();
        task.setValue(Task.TITLE, title);
        tags.clear();
        TaskService.parseQuickAddMarkup(task, tags);
    }

    private void assertImportanceIs(int importance) {
        assertEquals(importance, (int)task.getValue(Task.IMPORTANCE));
    }

}
