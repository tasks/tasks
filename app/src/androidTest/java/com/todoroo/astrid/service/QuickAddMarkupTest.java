/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import static junit.framework.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class QuickAddMarkupTest extends InjectingTestCase {

  private final ArrayList<String> tags = new ArrayList<>();
  @Inject TagService tagService;
  private Task task;

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

  // --- helpers

  @Test
  public void testPriorities() {
    whenTitleIs("eat !1");
    assertTitleBecomes("eat");
    assertPriority(Priority.LOW);

    whenTitleIs("super cool!");
    assertTitleBecomes("super cool!");

    whenTitleIs("stay alive !4");
    assertTitleBecomes("stay alive");
    assertPriority(Priority.HIGH);
  }

  @Test
  public void testMixed() {
    whenTitleIs("eat #food !2");
    assertTitleBecomes("eat");
    assertTagsAre("food");
    assertPriority(Priority.MEDIUM);
  }

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
    TitleParser.parse(tagService, task, tags);
  }

  private void assertPriority(int priority) {
    assertEquals(priority, (int) task.getPriority());
  }
}
