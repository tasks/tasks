/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.support.test.runner.AndroidJUnit4;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.TitleParser;
import java.util.ArrayList;
import java.util.Calendar;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class TitleParserTest extends InjectingTestCase {

  @Inject TagService tagService;
  @Inject Preferences preferences;
  @Inject TaskCreator taskCreator;

  @Override
  public void setUp() {
    super.setUp();
    preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  /**
   * test that completing a task w/ no regular expressions creates a simple task with no date, no
   * repeat, no lists
   */
  @Test
  public void testNoRegexes() {
    Task task = taskCreator.basicQuickAddTask("Jog");
    Task nothing = new Task();
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());
    assertEquals(task.getRecurrence(), nothing.getRecurrence());
  }

  /** Tests correct date is parsed */
  @Test
  public void testMonthDate() {
    String[] titleMonthStrings = {
      "Jan.", "January",
      "Feb.", "February",
      "Mar.", "March",
      "Apr.", "April",
      "May", "May",
      "Jun.", "June",
      "Jul.", "July",
      "Aug.", "August",
      "Sep.", "September",
      "Oct.", "October",
      "Nov.", "November",
      "Dec.", "December"
    };
    for (int i = 0; i < 23; i++) {
      String testTitle = "Jog on " + titleMonthStrings[i] + " 12.";
      Task task = insertTitleAddTask(testTitle);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getMonthOfYear(), i / 2 + 1);
      assertEquals(date.getDayOfMonth(), 12);
    }
  }

  @Test
  public void testMonthSlashDay() {
    for (int i = 1; i < 13; i++) {
      String testTitle = "Jog on " + i + "/12/13";
      Task task = insertTitleAddTask(testTitle);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getMonthOfYear(), i);
      assertEquals(date.getDayOfMonth(), 12);
      assertEquals(date.getYear(), 2013);
    }
  }

  @Test
  public void testArmyTime() {
    String testTitle = "Jog on 23:21.";
    Task task = insertTitleAddTask(testTitle);
    DateTime date = newDateTime(task.getDueDate());
    assertEquals(date.getHourOfDay(), 23);
    assertEquals(date.getMinuteOfHour(), 21);
  }

  @Test
  public void test_AM_PM() {
    String testTitle = "Jog at 8:33 PM.";
    Task task = insertTitleAddTask(testTitle);
    DateTime date = newDateTime(task.getDueDate());
    assertEquals(date.getHourOfDay(), 20);
    assertEquals(date.getMinuteOfHour(), 33);
  }

  @Test
  public void test_at_hour() {
    String testTitle = "Jog at 8 PM.";
    Task task = insertTitleAddTask(testTitle);
    DateTime date = newDateTime(task.getDueDate());
    assertEquals(date.getHourOfDay(), 20);
    assertEquals(date.getMinuteOfHour(), 0);
  }

  @Test
  public void test_oclock_AM() {
    String testTitle = "Jog at 8 o'clock AM.";
    Task task = insertTitleAddTask(testTitle);
    DateTime date = newDateTime(task.getDueDate());
    assertEquals(date.getHourOfDay(), 8);
    assertEquals(date.getMinuteOfHour(), 0);
  }

  @Test
  public void test_several_forms_of_eight() {
    String[] testTitles = {"Jog 8 AM", "Jog 8 o'clock AM", "at 8:00 AM"};
    for (String testTitle : testTitles) {
      Task task = insertTitleAddTask(testTitle);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 8);
      assertEquals(date.getMinuteOfHour(), 0);
    }
  }

  @Test
  public void test_several_forms_of_1230PM() {
    String[] testTitles = {
      "Jog 12:30 PM", "at 12:30 PM", "Do something on 12:30 PM", "Jog at 12:30 PM Friday"
    };
    for (String testTitle : testTitles) {
      Task task = insertTitleAddTask(testTitle);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 12);
      assertEquals(date.getMinuteOfHour(), 30);
    }
  }

  private Task insertTitleAddTask(String title) {
    return taskCreator.createWithValues(null, title);
  }

  // ----------------Days begin----------------//
  @Test
  public void testDays() {
    Calendar today = Calendar.getInstance();

    String title = "Jog today";
    Task task = taskCreator.createWithValues(null, title);
    DateTime date = newDateTime(task.getDueDate());
    assertEquals(date.getDayOfWeek(), today.get(Calendar.DAY_OF_WEEK));
    // Calendar starts 1-6, date.getDay() starts at 0

    title = "Jog tomorrow";
    task = taskCreator.createWithValues(null, title);
    date = newDateTime(task.getDueDate());
    assertEquals((date.getDayOfWeek()) % 7, (today.get(Calendar.DAY_OF_WEEK) + 1) % 7);

    String[] days = {
      "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday",
    };
    String[] abrevDays = {"sun.", "mon.", "tue.", "wed.", "thu.", "fri.", "sat."};

    for (int i = 1; i <= 6; i++) {
      title = "Jog " + days[i];
      task = taskCreator.createWithValues(null, title);
      date = newDateTime(task.getDueDate());
      assertEquals(date.getDayOfWeek(), i + 1);

      title = "Jog " + abrevDays[i];
      task = taskCreator.createWithValues(null, title);
      date = newDateTime(task.getDueDate());
      assertEquals(date.getDayOfWeek(), i + 1);
    }
  }

  // ----------------Days end----------------//

  // ----------------Priority begin----------------//

  /** tests all words using priority 0 */
  @Test
  public void testPriority0() {
    String[] acceptedStrings = {"priority 0", "least priority", "lowest priority", "bang 0"};
    for (String acceptedString : acceptedStrings) {
      String title = "Jog " + acceptedString;
      Task task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.NONE);
    }
    for (String acceptedString : acceptedStrings) {
      String title = acceptedString + " jog";
      Task task = taskCreator.createWithValues(null, title);
      assertNotSame(task.getPriority(), Priority.NONE);
    }
  }

  @Test
  public void testPriority1() {
    String[] acceptedStringsAtEnd = {"priority 1", "low priority", "bang", "bang 1"};
    String[] acceptedStringsAnywhere = {"!1", "!"};
    Task task;
    for (String acceptedStringAtEnd : acceptedStringsAtEnd) {
      task =
          taskCreator.basicQuickAddTask(
              "Jog " + acceptedStringAtEnd); // test at end of task. should set importance.
      assertEquals((int) task.getPriority(), Priority.LOW);
    }
    for (String acceptedStringAtEnd : acceptedStringsAtEnd) {
      task =
          taskCreator.basicQuickAddTask(
              acceptedStringAtEnd
                  + " jog"); // test at beginning of task. should not set importance.
      assertEquals((int) task.getPriority(), Priority.LOW);
    }
    for (String acceptedStringAnywhere : acceptedStringsAnywhere) {
      task =
          taskCreator.basicQuickAddTask(
              "Jog " + acceptedStringAnywhere); // test at end of task. should set importance.
      assertEquals((int) task.getPriority(), Priority.LOW);

      task =
          taskCreator.basicQuickAddTask(
              acceptedStringAnywhere + " jog"); // test at beginning of task. should set importance.
      assertEquals((int) task.getPriority(), Priority.LOW);
    }
  }

  @Test
  public void testPriority2() {
    String[] acceptedStringsAtEnd = {"priority 2", "high priority", "bang bang", "bang 2"};
    String[] acceptedStringsAnywhere = {"!2", "!!"};
    for (String acceptedStringAtEnd : acceptedStringsAtEnd) {
      String title = "Jog " + acceptedStringAtEnd;
      Task task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.MEDIUM);

      title = acceptedStringAtEnd + " jog";
      task = taskCreator.createWithValues(null, title);
      assertNotSame(task.getPriority(), Priority.MEDIUM);
    }
    for (String acceptedStringAnywhere : acceptedStringsAnywhere) {
      String title = "Jog " + acceptedStringAnywhere;
      Task task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.MEDIUM);

      title = acceptedStringAnywhere + " jog";
      task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.MEDIUM);
    }
  }

  @Test
  public void testPriority3() {
    String[] acceptedStringsAtEnd = {
      "priority 3",
      "highest priority",
      "bang bang bang",
      "bang 3",
      "bang bang bang bang bang bang bang"
    };
    String[] acceptedStringsAnywhere = {"!3", "!!!", "!6", "!!!!!!!!!!!!!"};
    for (String acceptedStringAtEnd : acceptedStringsAtEnd) {
      String title = "Jog " + acceptedStringAtEnd;
      Task task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.HIGH);

      title = acceptedStringAtEnd + " jog";
      task = taskCreator.createWithValues(null, title);
      assertNotSame(task.getPriority(), Priority.HIGH);
    }
    for (String acceptedStringAnywhere : acceptedStringsAnywhere) {
      String title = "Jog " + acceptedStringAnywhere;
      Task task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.HIGH);

      title = acceptedStringAnywhere + " jog";
      task = taskCreator.createWithValues(null, title);
      assertEquals((int) task.getPriority(), Priority.HIGH);
    }
  }

  // ----------------Priority end----------------//

  // ----------------Repeats begin----------------//

  /** test daily repeat from due date, but with no due date set */
  @Test
  public void testDailyWithNoDueDate() {
    String title = "Jog daily";
    Task task = taskCreator.createWithValues(null, title);
    RRule rrule = new RRule();
    rrule.setFreq(Frequency.DAILY);
    rrule.setInterval(1);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    title = "Jog every day";
    task = taskCreator.createWithValues(null, title);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    for (int i = 1; i <= 12; i++) {
      title = "Jog every " + i + " days.";
      rrule.setInterval(i);
      task = taskCreator.createWithValues(null, title);
      assertEquals(task.getRecurrence(), rrule.toIcal());
      assertFalse(task.hasDueTime());
      assertFalse(task.hasDueDate());
    }
  }

  /** test weekly repeat from due date, with no due date & time set */
  @Test
  public void testWeeklyWithNoDueDate() {
    String title = "Jog weekly";
    Task task = taskCreator.createWithValues(null, title);
    RRule rrule = new RRule();
    rrule.setFreq(Frequency.WEEKLY);
    rrule.setInterval(1);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    title = "Jog every week";
    task = taskCreator.createWithValues(null, title);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    for (int i = 1; i <= 12; i++) {
      title = "Jog every " + i + " weeks";
      rrule.setInterval(i);
      task = taskCreator.createWithValues(null, title);
      assertEquals(task.getRecurrence(), rrule.toIcal());
      assertFalse(task.hasDueTime());
      assertFalse(task.hasDueDate());
    }
  }

  /** test hourly repeat from due date, with no due date but no time */
  @Test
  public void testMonthlyFromNoDueDate() {
    String title = "Jog monthly";
    Task task = taskCreator.createWithValues(null, title);
    RRule rrule = new RRule();
    rrule.setFreq(Frequency.MONTHLY);
    rrule.setInterval(1);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    title = "Jog every month";
    task = taskCreator.createWithValues(null, title);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertFalse(task.hasDueTime());
    assertFalse(task.hasDueDate());

    for (int i = 1; i <= 12; i++) {
      title = "Jog every " + i + " months";
      rrule.setInterval(i);
      task = taskCreator.createWithValues(null, title);
      assertEquals(task.getRecurrence(), rrule.toIcal());
      assertFalse(task.hasDueTime());
      assertFalse(task.hasDueDate());
    }
  }

  @Test
  public void testDailyFromDueDate() {
    String title = "Jog daily starting from today";
    Task task = taskCreator.createWithValues(null, title);
    RRule rrule = new RRule();
    rrule.setFreq(Frequency.DAILY);
    rrule.setInterval(1);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertTrue(task.hasDueDate());

    title = "Jog every day starting from today";
    task = taskCreator.createWithValues(null, title);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertTrue(task.hasDueDate());

    for (int i = 1; i <= 12; i++) {
      title = "Jog every " + i + " days starting from today";
      rrule.setInterval(i);
      task = taskCreator.createWithValues(null, title);
      assertEquals(task.getRecurrence(), rrule.toIcal());
      assertTrue(task.hasDueDate());
    }
  }

  @Test
  public void testWeeklyFromDueDate() {
    String title = "Jog weekly starting from today";
    Task task = taskCreator.createWithValues(null, title);
    RRule rrule = new RRule();
    rrule.setFreq(Frequency.WEEKLY);
    rrule.setInterval(1);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertTrue(task.hasDueDate());

    title = "Jog every week starting from today";
    task = taskCreator.createWithValues(null, title);
    assertEquals(task.getRecurrence(), rrule.toIcal());
    assertTrue(task.hasDueDate());

    for (int i = 1; i <= 12; i++) {
      title = "Jog every " + i + " weeks starting from today";
      rrule.setInterval(i);
      task = taskCreator.createWithValues(null, title);
      assertEquals(task.getRecurrence(), rrule.toIcal());
      assertTrue(task.hasDueDate());
    }
  }

  // ----------------Repeats end----------------//

  // ----------------Tags begin----------------//

  /** tests all words using priority 0 */
  @Test
  public void testTagsPound() {
    String[] acceptedStrings = {"#tag", "#a", "#(a cool tag)", "#(cool)"};
    Task task;
    for (String acceptedString : acceptedStrings) {
      task = new Task();
      task.setTitle("Jog " + acceptedString); // test at end of task. should set importance.
      ArrayList<String> tags = new ArrayList<>();
      TitleParser.listHelper(tagService, task, tags);
      String tag = TitleParser.trimParenthesis(acceptedString);
      assertTrue(
          "test pound at failed for string: " + acceptedString + " for tags: " + tags.toString(),
          tags.contains(tag));
    }
  }

  /** tests all words using priority 0 */
  @Test
  public void testTagsAt() {
    String[] acceptedStrings = {"@tag", "@a", "@(a cool tag)", "@(cool)"};
    Task task;
    for (String acceptedString : acceptedStrings) {
      task = new Task();
      task.setTitle("Jog " + acceptedString); // test at end of task. should set importance.
      ArrayList<String> tags = new ArrayList<>();
      TitleParser.listHelper(tagService, task, tags);
      String tag = TitleParser.trimParenthesis(acceptedString);
      assertTrue(
          "testTagsAt failed for string: " + acceptedString + " for tags: " + tags.toString(),
          tags.contains(tag));
    }
  }
}
