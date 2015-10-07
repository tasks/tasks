/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDateTime;

public class TitleParserTest extends DatabaseTestCase {

    @Inject TaskService taskService;
    @Inject TagService tagService;
    @Inject Preferences preferences;

    @Override
    protected void setUp() {
        super.setUp();
        preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
    }

  /** test that completing a task w/ no regular expressions creates a simple task with no date, no repeat, no lists*/
  public void testNoRegexes() throws Exception{
      Task task = new Task();
      Task nothing = new Task();
      task.setTitle("Jog");
      taskService.save(task);
      assertFalse(task.hasDueTime());
      assertFalse(task.hasDueDate());
      assertEquals(task.getRecurrence(), nothing.getRecurrence());
  }

  /** Tests correct date is parsed **/
  public void testMonthDate() {
      Task task = new Task();
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
          insertTitleAddTask(testTitle, task);
          DateTime date = newDateTime(task.getDueDate());
          assertEquals(date.getMonthOfYear(), i/2 + 1);
          assertEquals(date.getDayOfMonth(), 12);
      }
  }

  public void testMonthSlashDay() {
      Task task = new Task();
      for (int i = 1; i < 13; i++) {
          String testTitle = "Jog on " + i + "/12/13";
          insertTitleAddTask(testTitle, task);
          DateTime date = newDateTime(task.getDueDate());
          assertEquals(date.getMonthOfYear(), i);
          assertEquals(date.getDayOfMonth(), 12);
          assertEquals(date.getYear(), 2013);
      }
  }

  public void testArmyTime() {
      Task task = new Task();
      String testTitle = "Jog on 23:21.";
      insertTitleAddTask(testTitle, task);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 23);
      assertEquals(date.getMinuteOfHour(), 21);
  }

  public void test_AM_PM() {
      Task task = new Task();
      String testTitle = "Jog at 8:33 PM.";
      insertTitleAddTask(testTitle, task);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 20);
      assertEquals(date.getMinuteOfHour(), 33);
  }

  public void test_at_hour() {
      Task task = new Task();
      String testTitle = "Jog at 8 PM.";
      insertTitleAddTask(testTitle, task);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 20);
      assertEquals(date.getMinuteOfHour(), 0);
  }

  public void test_oclock_AM() {
      Task task = new Task();
      String testTitle = "Jog at 8 o'clock AM.";
      insertTitleAddTask(testTitle, task);
      DateTime date = newDateTime(task.getDueDate());
      assertEquals(date.getHourOfDay(), 8);
      assertEquals(date.getMinuteOfHour(), 0);
  }

  public void test_several_forms_of_eight() {
      Task task = new Task();
      String[] testTitles = {
              "Jog 8 AM",
              "Jog 8 o'clock AM",
              "at 8:00 AM"
      };
      for (String testTitle: testTitles) {
          insertTitleAddTask(testTitle, task);
          DateTime date = newDateTime(task.getDueDate());
          assertEquals(date.getHourOfDay(), 8);
          assertEquals(date.getMinuteOfHour(), 0);
      }
  }

  public void test_several_forms_of_1230PM() {
      Task task = new Task();
      String[] testTitles = {
              "Jog 12:30 PM",
              "at 12:30 PM",
              "Do something on 12:30 PM",
              "Jog at 12:30 PM Friday"

      };
      for (String testTitle: testTitles) {
          insertTitleAddTask(testTitle, task);
          DateTime date = newDateTime(task.getDueDate());
          assertEquals(date.getHourOfDay(), 12);
          assertEquals(date.getMinuteOfHour(), 30);
      }
  }

  private void insertTitleAddTask(String title, Task task) {
      task.clear();
      task.setTitle(title);
      taskService.createWithValues(task, null, title);
  }


   // ----------------Days begin----------------//
    public void disabled_testDays() {
        Calendar today = Calendar.getInstance();
        Task task = new Task();

        String title = "Jog today";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        DateTime date = newDateTime(task.getDueDate());
        assertEquals(date.getDayOfMonth(), today.get(Calendar.DAY_OF_WEEK));
        //Calendar starts 1-6, date.getDay() starts at 0

        task = new Task();
        title = "Jog tomorrow";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        date = newDateTime(task.getDueDate());
        assertEquals((date.getDayOfWeek()) % 7, (today.get(Calendar.DAY_OF_WEEK)+1) % 7);

        String[] days = {
                "sunday",
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
        };
        String[] abrevDays = {
                "sun.",
                "mon.",
                "tue.",
                "wed.",
                "thu.",
                "fri.",
                "sat."
        };

        for (int i = 1; i <= 6; i++){
            task = new Task();
            title = "Jog "+ days[i];
            task.setTitle(title);
            taskService.createWithValues(task, null, title);
            date = newDateTime(task.getDueDate());
            assertEquals(date.getDayOfWeek(), i);

            task = new Task();
            title = "Jog "+ abrevDays[i];
            task.setTitle(title);
            taskService.createWithValues(task, null, title);
            date = newDateTime(task.getDueDate());
            assertEquals(date.getDayOfWeek(), i);
        }

        }

    //----------------Days end----------------//


    //----------------Priority begin----------------//
    /** tests all words using priority 0 */
    public void testPriority0() throws Exception {
        String[] acceptedStrings = {
                "priority 0",
                "least priority",
                "lowest priority",
                "bang 0"
        };
        Task task;
        for (String acceptedString:acceptedStrings){
            task = new Task();
            String title = "Jog " + acceptedString;
            task.setTitle(title); //test at end of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_LEAST);
        }
        for (String acceptedString:acceptedStrings){
            task = new Task();
            String title = acceptedString + " jog";
            task.setTitle(title); //test at beginning of task. should not set importance.
            taskService.createWithValues(task, null, title);
            assertNotSame(task.getImportance(),Task.IMPORTANCE_LEAST);
        }
    }

    public void testPriority1() throws Exception {
        String[] acceptedStringsAtEnd = {
                "priority 1",
                "low priority",
                "bang",
                "bang 1"
        };
        String[] acceptedStringsAnywhere = {
                "!1",
                "!"
        };
        Task task;
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            task.setTitle("Jog " + acceptedStringAtEnd); //test at end of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_SHOULD_DO);
        }
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            task.setTitle(acceptedStringAtEnd + " jog"); //test at beginning of task. should not set importance.
            taskService.save(task);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_SHOULD_DO);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            task.setTitle("Jog " + acceptedStringAnywhere); //test at end of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_SHOULD_DO);

            task.setTitle(acceptedStringAnywhere + " jog"); //test at beginning of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_SHOULD_DO);
        }
    }

    public void testPriority2() throws Exception {
        String[] acceptedStringsAtEnd = {
                "priority 2",
                "high priority",
                "bang bang",
                "bang 2"
        };
        String[] acceptedStringsAnywhere = {
                "!2",
                "!!"
        };
        Task task;
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            String title = "Jog " + acceptedStringAtEnd;
            task.setTitle(title); //test at end of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_MUST_DO);

            task = new Task();
            title = acceptedStringAtEnd + " jog";
            task.setTitle(title); //test at beginning of task. should not set importance.
            taskService.createWithValues(task, null, title);
            assertNotSame(task.getImportance(), Task.IMPORTANCE_MUST_DO);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            String title = "Jog " + acceptedStringAnywhere;
            task.setTitle(title); //test at end of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_MUST_DO);

            title = acceptedStringAnywhere + " jog";
            task.setTitle(title); //test at beginning of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_MUST_DO);
        }
    }

    public void testPriority3() throws Exception {
        String[] acceptedStringsAtEnd = {
                "priority 3",
                "highest priority",
                "bang bang bang",
                "bang 3",
                "bang bang bang bang bang bang bang"
        };
        String[] acceptedStringsAnywhere = {
                "!3",
                "!!!",
                "!6",
                "!!!!!!!!!!!!!"
        };
        Task task;
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            String title = "Jog " + acceptedStringAtEnd;
            task.setTitle(title); //test at end of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_DO_OR_DIE);

            task = new Task();
            title = acceptedStringAtEnd + " jog";
            task.setTitle(title); //test at beginning of task. should not set importance.
            taskService.createWithValues(task, null, title);
            assertNotSame(task.getImportance(), Task.IMPORTANCE_DO_OR_DIE);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            String title = "Jog " + acceptedStringAnywhere;
            task.setTitle(title); //test at end of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_DO_OR_DIE);

            title = acceptedStringAnywhere + " jog";
            task.setTitle(title); //test at beginning of task. should set importance.
            taskService.createWithValues(task, null, title);
            assertEquals((int)task.getImportance(), Task.IMPORTANCE_DO_OR_DIE);
        }
    }



    //----------------Priority end----------------//


    //----------------Repeats begin----------------//
    /** test daily repeat from due date, but with no due date set */
    public void testDailyWithNoDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog daily";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.DAILY);
        rrule.setInterval(1);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every day";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " days.";
            task.setTitle(title);
            rrule.setInterval(i);
            taskService.createWithValues(task, null, title);
            assertEquals(task.getRecurrence(), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }

    }

    /** test weekly repeat from due date, with no due date & time set */
    public void testWeeklyWithNoDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog weekly";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.WEEKLY);
        rrule.setInterval(1);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every week";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " weeks";
            task.setTitle(title);
            rrule.setInterval(i);
            taskService.createWithValues(task, null, title);
            assertEquals(task.getRecurrence(), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }
    }

    /** test hourly repeat from due date, with no due date but no time */
    public void testMonthlyFromNoDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog monthly";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.MONTHLY);
        rrule.setInterval(1);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every month";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " months";
            task.setTitle(title);
            rrule.setInterval(i);
            taskService.createWithValues(task, null, title);
            assertEquals(task.getRecurrence(), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }
    }

    public void testDailyFromDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog daily starting from today";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.DAILY);
        rrule.setInterval(1);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertTrue(task.hasDueDate());

        task.clearValue(Task.ID);
        task.clearValue(Task.UUID);
        title = "Jog every day starting from today";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertTrue(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " days starting from today";
            task.setTitle(title);
            rrule.setInterval(i);
            taskService.createWithValues(task, null, title);
            assertEquals(task.getRecurrence(), rrule.toIcal());
            assertTrue(task.hasDueDate());
            task = new Task();
        }
    }

    public void testWeeklyFromDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog weekly starting from today";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.WEEKLY);
        rrule.setInterval(1);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertTrue(task.hasDueDate());

        task.clearValue(Task.ID);
        task.clearValue(Task.UUID);
        title = "Jog every week starting from today";
        task.setTitle(title);
        taskService.createWithValues(task, null, title);
        assertEquals(task.getRecurrence(), rrule.toIcal());
        assertTrue(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " weeks starting from today";
            task.setTitle(title);
            rrule.setInterval(i);
            taskService.createWithValues(task, null, title);
            assertEquals(task.getRecurrence(), rrule.toIcal());
            assertTrue(task.hasDueDate());
            task = new Task();
        }
    }

//----------------Repeats end----------------//


    //----------------Tags begin----------------//
    /** tests all words using priority 0 */
    public void testTagsPound() throws Exception {
        String[] acceptedStrings = {
                "#tag",
                "#a",
                "#(a cool tag)",
                "#(cool)"
        };
        Task task;
        for (String acceptedString : acceptedStrings) {
            task = new Task();
            task.setTitle("Jog " + acceptedString); //test at end of task. should set importance.
            ArrayList<String> tags = new ArrayList<>();
            TitleParser.listHelper(tagService, task, tags);
            String tag = TitleParser.trimParenthesis(acceptedString);
            assertTrue("test pound at failed for string: " + acceptedString + " for tags: " + tags.toString(), tags.contains(tag));
        }
    }

    /** tests all words using priority 0 */
    public void testTagsAt() throws Exception {
        String[] acceptedStrings = {
                "@tag",
                "@a",
                "@(a cool tag)",
                "@(cool)"
        };
        Task task;
        for (String acceptedString : acceptedStrings) {
            task = new Task();
            task.setTitle("Jog " + acceptedString); //test at end of task. should set importance.
            ArrayList<String> tags = new ArrayList<>();
            TitleParser.listHelper(tagService, task, tags);
            String tag = TitleParser.trimParenthesis(acceptedString);
            assertTrue("testTagsAt failed for string: " + acceptedString+ " for tags: " + tags.toString(), tags.contains(tag));
        }
    }



    //----------------Priority end----------------//


}
