/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.TitleParser;

public class TitleParserTest extends DatabaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
    }



  /** test that completing a task w/ no regular expressions creates a simple task with no date, no repeat, no lists*/
  public void testNoRegexes() throws Exception{
      TaskService taskService = new TaskService();
      Task task = new Task();
      Task nothing = new Task();
      task.setValue(Task.TITLE, "Jog");
      taskService.save(task);
      assertFalse(task.hasDueTime());
      assertFalse(task.hasDueDate());
      assertEquals(task.getValue(Task.RECURRENCE), nothing.getValue(Task.RECURRENCE));
  }

  /** Tests correct date is parsed **/
  public void testMonthDate() {
      TaskService taskService = new TaskService();
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
          insertTitleAddTask(testTitle, task, taskService);
          Date date = new Date(task.getValue(Task.DUE_DATE));
          assertEquals(date.getMonth(), i/2);
          assertEquals(date.getDate(), 12);
      }
  }

  public void testMonthSlashDay() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      for (int i = 1; i < 13; i++) {
          String testTitle = "Jog on " + i + "/12/13";
          insertTitleAddTask(testTitle, task, taskService);
          Date date = new Date(task.getValue(Task.DUE_DATE));
          assertEquals(date.getMonth(), i-1);
          assertEquals(date.getDate(), 12);
          assertEquals(date.getYear(), 113);
      }
  }

  public void testArmyTime() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String testTitle = "Jog on 23:21.";
      insertTitleAddTask(testTitle, task, taskService);
      Date date = new Date(task.getValue(Task.DUE_DATE));
      assertEquals(date.getMinutes(), 21);
      assertEquals(date.getHours(), 23);
  }

  public void test_AM_PM() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String testTitle = "Jog at 8:33 PM.";
      insertTitleAddTask(testTitle, task, taskService);
      Date date = new Date(task.getValue(Task.DUE_DATE));
      assertEquals(date.getMinutes(), 33);
      assertEquals(date.getHours(), 20);
  }

  public void test_at_hour() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String testTitle = "Jog at 8 PM.";
      insertTitleAddTask(testTitle, task, taskService);
      Date date = new Date(task.getValue(Task.DUE_DATE));
      assertEquals(date.getMinutes(), 0);
      assertEquals(date.getHours(), 20);
  }

  public void test_oclock_AM() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String testTitle = "Jog at 8 o'clock AM.";
      insertTitleAddTask(testTitle, task, taskService);
      Date date = new Date(task.getValue(Task.DUE_DATE));
      assertEquals(date.getMinutes(), 0);
      assertEquals(date.getHours(), 8);
  }

  public void test_several_forms_of_eight() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String[] testTitles = {
              "Jog 8 AM",
              "Jog 8 o'clock AM",
              "at 8:00 AM"
      };
      for (String testTitle: testTitles) {
          insertTitleAddTask(testTitle, task, taskService);
          Date date = new Date(task.getValue(Task.DUE_DATE));
          assertEquals(date.getMinutes(), 0);
          assertEquals(date.getHours(), 8);
      }
  }

  public void test_several_forms_of_1230PM() {
      TaskService taskService = new TaskService();
      Task task = new Task();
      String[] testTitles = {
              "Jog 12:30 PM",
              "at 12:30 PM",
              "Do something on 12:30 PM",
              "Jog at 12:30 PM Friday"

      };
      for (String testTitle: testTitles) {
          insertTitleAddTask(testTitle, task, taskService);
          Date date = new Date(task.getValue(Task.DUE_DATE));
          assertEquals(date.getMinutes(), 30);
          assertEquals(date.getHours(), 12);
      }
  }

  private void insertTitleAddTask(String title, Task task, TaskService taskService) {
      task.clear();
      task.setValue(Task.TITLE, title);
      TaskService.createWithValues(task, null, title);
  }


   // ----------------Days begin----------------//
    public void testDays() throws Exception{
        Calendar today = Calendar.getInstance();
        TaskService taskService = new TaskService();
        Task task = new Task();

        String title = "Jog today";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        Date date = new Date(task.getValue(Task.DUE_DATE));
        assertEquals(date.getDay()+1, today.get(Calendar.DAY_OF_WEEK));
        //Calendar starts 1-6, date.getDay() starts at 0

        task = new Task();
        title = "Jog tomorrow";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        date = new Date(task.getValue(Task.DUE_DATE));
        assertEquals((date.getDay()+1) % 7, (today.get(Calendar.DAY_OF_WEEK)+1) % 7);

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
            task.setValue(Task.TITLE, title);
            TaskService.createWithValues(task, null, title);
            date = new Date(task.getValue(Task.DUE_DATE));
            assertEquals(date.getDay(), i);

            task = new Task();
            title = "Jog "+ abrevDays[i];
            task.setValue(Task.TITLE, title);
            TaskService.createWithValues(task, null, title);
            date = new Date(task.getValue(Task.DUE_DATE));
            assertEquals(date.getDay(), i);
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
        Task task = new Task();
        for (String acceptedString:acceptedStrings){
            task = new Task();
            String title = "Jog " + acceptedString;
            task.setValue(Task.TITLE, title); //test at end of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_LEAST);
        }
        for (String acceptedString:acceptedStrings){
            task = new Task();
            String title = acceptedString + " jog";
            task.setValue(Task.TITLE, title); //test at beginning of task. should not set importance.
            TaskService.createWithValues(task, null, title);
            assertNotSame((int)task.getValue(Task.IMPORTANCE),(int)Task.IMPORTANCE_LEAST);
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
        TaskService taskService = new TaskService();
        Task task = new Task();
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            task.setValue(Task.TITLE, "Jog " + acceptedStringAtEnd); //test at end of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_SHOULD_DO);
        }
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            task.setValue(Task.TITLE, acceptedStringAtEnd + " jog"); //test at beginning of task. should not set importance.
            taskService.save(task);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_SHOULD_DO);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            task.setValue(Task.TITLE, "Jog " + acceptedStringAnywhere); //test at end of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_SHOULD_DO);

            task.setValue(Task.TITLE, acceptedStringAnywhere + " jog"); //test at beginning of task. should set importance.
            taskService.save(task);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_SHOULD_DO);
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
        TaskService taskService = new TaskService();
        Task task = new Task();
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            String title = "Jog " + acceptedStringAtEnd;
            task.setValue(Task.TITLE, title); //test at end of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_MUST_DO);

            task = new Task();
            title = acceptedStringAtEnd + " jog";
            task.setValue(Task.TITLE, title); //test at beginning of task. should not set importance.
            TaskService.createWithValues(task, null, title);
            assertNotSame((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_MUST_DO);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            String title = "Jog " + acceptedStringAnywhere;
            task.setValue(Task.TITLE, title); //test at end of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_MUST_DO);

            title = acceptedStringAnywhere + " jog";
            task.setValue(Task.TITLE, title); //test at beginning of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_MUST_DO);
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
        TaskService taskService = new TaskService();
        Task task = new Task();
        for (String acceptedStringAtEnd:acceptedStringsAtEnd){
            task = new Task();
            String title = "Jog " + acceptedStringAtEnd;
            task.setValue(Task.TITLE, title); //test at end of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_DO_OR_DIE);

            task = new Task();
            title = acceptedStringAtEnd + " jog";
            task.setValue(Task.TITLE, title); //test at beginning of task. should not set importance.
            TaskService.createWithValues(task, null, title);
            assertNotSame((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_DO_OR_DIE);
        }
        for (String acceptedStringAnywhere:acceptedStringsAnywhere){
            task = new Task();
            String title = "Jog " + acceptedStringAnywhere;
            task.setValue(Task.TITLE, title); //test at end of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_DO_OR_DIE);

            title = acceptedStringAnywhere + " jog";
            task.setValue(Task.TITLE, title); //test at beginning of task. should set importance.
            TaskService.createWithValues(task, null, title);
            assertEquals((int)task.getValue(Task.IMPORTANCE), (int)Task.IMPORTANCE_DO_OR_DIE);
        }
    }



    //----------------Priority end----------------//


    //----------------Repeats begin----------------//
    /** test daily repeat from due date, but with no due date set */
    public void testDailyWithNoDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog daily";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.DAILY);
        rrule.setInterval(1);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every day";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " days.";
            task.setValue(Task.TITLE, title);
            rrule.setInterval(i);
            TaskService.createWithValues(task, null, title);
            assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }

    }

    /** test weekly repeat from due date, with no due date & time set */
    public void testWeeklyWithNoDueDate() throws Exception {
        TaskService taskService = new TaskService();
        Task task = new Task();
        String title = "Jog weekly";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.WEEKLY);
        rrule.setInterval(1);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every week";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " weeks";
            task.setValue(Task.TITLE, title);
            rrule.setInterval(i);
            TaskService.createWithValues(task, null, title);
            assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }
    }

    /** test hourly repeat from due date, with no due date but no time */
    public void testMonthlyFromNoDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog monthly";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.MONTHLY);
        rrule.setInterval(1);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        title = "Jog every month";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertFalse(task.hasDueTime());
        assertFalse(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " months";
            task.setValue(Task.TITLE, title);
            rrule.setInterval(i);
            TaskService.createWithValues(task, null, title);
            assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
            assertFalse(task.hasDueTime());
            assertFalse(task.hasDueDate());
            task = new Task();
        }
    }

    public void testDailyFromDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog daily starting from today";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.DAILY);
        rrule.setInterval(1);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertTrue(task.hasDueDate());

        task.clearValue(Task.ID);
        title = "Jog every day starting from today";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertTrue(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " days starting from today";
            task.setValue(Task.TITLE, title);
            rrule.setInterval(i);
            TaskService.createWithValues(task, null, title);
            assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
            assertTrue(task.hasDueDate());
            task = new Task();
        }
    }

    public void testWeeklyFromDueDate() throws Exception {
        Task task = new Task();
        String title = "Jog weekly starting from today";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.WEEKLY);
        rrule.setInterval(1);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertTrue(task.hasDueDate());

        task.clearValue(Task.ID);
        title = "Jog every week starting from today";
        task.setValue(Task.TITLE, title);
        TaskService.createWithValues(task, null, title);
        assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
        assertTrue(task.hasDueDate());

        for (int i = 1; i <= 12; i++){
            title = "Jog every " + i + " weeks starting from today";
            task.setValue(Task.TITLE, title);
            rrule.setInterval(i);
            TaskService.createWithValues(task, null, title);
            assertEquals(task.getValue(Task.RECURRENCE), rrule.toIcal());
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
        Task task = new Task();
        for (String acceptedString:acceptedStrings){
            task = new Task();
            task.setValue(Task.TITLE, "Jog " + acceptedString); //test at end of task. should set importance.
            ArrayList<String> tags = new ArrayList<String>();
            TitleParser.listHelper(task, tags);
            String[] splitTags = TitleParser.trimParenthesisAndSplit(acceptedString);
            for (String tag : splitTags) {
                assertTrue("test pound at failed for string: " + acceptedString + " for tags: " + tags.toString(),tags.contains(tag));
            }
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
        Task task = new Task();
        for (String acceptedString:acceptedStrings){
            task = new Task();
            task.setValue(Task.TITLE, "Jog " + acceptedString); //test at end of task. should set importance.
            ArrayList<String> tags = new ArrayList<String>();
            TitleParser.listHelper(task, tags);
            String[] splitTags = TitleParser.trimParenthesisAndSplit(acceptedString);
            for (String tag : splitTags) {
                assertTrue("testTagsAt failed for string: " + acceptedString+ " for tags: " + tags.toString(), tags.contains(tag));
            }
        }
    }



    //----------------Priority end----------------//


}
