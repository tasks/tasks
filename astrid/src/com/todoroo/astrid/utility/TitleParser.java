/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.mdimension.jchronic.AstridChronic;
import com.mdimension.jchronic.Chronic;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService;

@SuppressWarnings("nls")
public class TitleParser {

    public static boolean parse(Task task, ArrayList<String> tags) {
        boolean markup = false;
        markup = repeatHelper(task) || markup;
        listHelper(task,tags); // Don't need to know if tags affected things since we don't show alerts for them
        markup = dayHelper(task) || markup;
        markup = priorityHelper(task) || markup;
        return markup;
    }

    public static String trimParenthesis(String pattern){
        if (pattern.charAt(0) == '#' || pattern.charAt(0) == '@') {
            pattern = pattern.substring(1);
        }
        if ('(' == pattern.charAt(0)) {
            String list = pattern.substring(1, pattern.length()-1);
            return list;
        }
        return pattern;
    }
    public static boolean listHelper(Task task, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        Pattern tagPattern = Pattern.compile("(\\s|^)#(\\(.*\\)|[^\\s]+)");
        Pattern contextPattern = Pattern.compile("(\\s|^)@(\\(.*\\)|[^\\s]+)");
        boolean result = false;

        Set<String> addedTags = new HashSet<String>();
        TagService tagService = TagService.getInstance();

        while(true) {
            Matcher m = tagPattern.matcher(inputText);
            if(m.find()) {
                result = true;
                String tag = TitleParser.trimParenthesis(m.group(2));
                String tagWithCase = tagService.getTagWithCase(tag);
                if (!addedTags.contains(tagWithCase))
                    tags.add(tagWithCase);
                addedTags.add(tagWithCase);
            } else {
                m = contextPattern.matcher(inputText);
                if(m.find()) {
                    result = true;
                    String tag = TitleParser.trimParenthesis(m.group(2));
                    String tagWithCase = tagService.getTagWithCase(tag);
                    if (!addedTags.contains(tagWithCase))
                        tags.add(tagWithCase);
                    addedTags.add(tagWithCase);
                } else{
                    break;
                }
            }
            inputText = inputText.substring(0, m.start()) + inputText.substring(m.end());
        }
        task.setValue(Task.TITLE, inputText.trim());
        return result;
    }

    //helper method for priorityHelper. converts the string to a Task Importance
    private static int strToPriority(String priorityStr) {
        if (priorityStr!=null)
            priorityStr.toLowerCase().trim();
        int priority = Task.IMPORTANCE_DO_OR_DIE;
        if ("0".equals(priorityStr) || "!0".equals(priorityStr) || "least".equals(priorityStr) ||  "lowest".equals(priorityStr))
            priority = Task.IMPORTANCE_NONE;
        if ("!".equals(priorityStr) || "!1".equals(priorityStr) || "bang".equals(priorityStr) || "1".equals(priorityStr) || "low".equals(priorityStr))
            priority = Task.IMPORTANCE_SHOULD_DO;
        if ("!!".equals(priorityStr) || "!2".equals(priorityStr) || "bang bang".equals(priorityStr) || "2".equals(priorityStr) || "high".equals(priorityStr))
            priority = Task.IMPORTANCE_MUST_DO;
        return priority;
    }

    //priorityHelper parses the string and sets the Task's importance
    private static boolean priorityHelper(Task task) {
        String inputText = task.getValue(Task.TITLE);
        String[] importanceStrings = {
                "()((^|[^\\w!])!+|(^|[^\\w!])!\\d)($|[^\\w!])",
                "()(?i)((\\s?bang){1,})$",
                "(?i)(\\spriority\\s?(\\d)$)",
                "(?i)(\\sbang\\s?(\\d)$)",
                "(?i)()(\\shigh(est)?|\\slow(est)?|\\stop|\\sleast) ?priority$"
        };
        boolean result = false;
        for (String importanceString:importanceStrings){
            Pattern importancePattern = Pattern.compile(importanceString);
            while (true){
                Matcher m = importancePattern.matcher(inputText);
                if(m.find()) {
                    result = true;
                    task.setValue(Task.IMPORTANCE, strToPriority(m.group(2).trim()));
                    int start = m.start() == 0 ? 0 : m.start() + 1;
                    inputText = inputText.substring(0, start) + inputText.substring(m.end());

                } else
                    break;
            }
        }
        task.setValue(Task.TITLE, inputText.trim());
        return result;
    }

    //helper for dayHelper. Converts am/pm to an int 0/1.
    private static int ampmToNumber(String amPmString) {
        int time = Calendar.PM;
        if (amPmString == null){
            return time;
        }
        String text = amPmString.toLowerCase().trim();
        if (text.equals ("am") || text.equals ("a.m") || text.equals("a"))
            time = Calendar.AM;
        if (text.equals ("pm") || text.equals ("p.m") || text.equals("p"))
            time = Calendar.PM;
        return time;
    }

    private static String removeIfParenthetical(Matcher m, String inputText) {
        String s = m.group();
        if (s.startsWith("(") && s.endsWith(")")) {
            return inputText.substring(0, m.start()) + inputText.substring(m.end());
        }
        return inputText;
    }

    private static String stripParens(String s) {
        if (s.startsWith("("))
            s = s.substring(1);
        if (s.endsWith(")"))
            s = s.substring(0, s.length() - 1);
        return s;
    }

    //---------------------DATE--------------------------
    //Handles setting the task's date.
    //Day of week (e.g. Monday, Tuesday,..) is overridden by a set date (e.g. October 23 2013).
    //Vague times (e.g. breakfast, night) are overridden by a set time (9 am, at 10, 17:00)
    private static boolean dayHelper(Task task ) {
        if (task.containsNonNullValue(Task.DUE_DATE))
            return false;
        String inputText = task.getValue(Task.TITLE);
        Calendar cal = null;
        Boolean containsSpecificTime = false;
        String[] daysOfWeek = {
                "(?i)(\\(|\\b)today(\\)|\\b)",
                "(?i)(\\(|\\b)tomorrow(\\)|\\b)",
                "(?i)(\\(|\\b)mon(day(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)tue(sday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)wed(nesday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)thu(rsday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)fri(day(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)sat(urday(\\)|\\b)|(\\)|\\.))",
                "(?i)(\\(|\\b)sun(day(\\)|\\b)|(\\)|\\.))"
        };

        for (String date : daysOfWeek){
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);
            if (m.find()) {
                String toParse = stripParens(m.group(0));
                Calendar dayCal = AstridChronic.parse(toParse).getBeginCalendar();
                cal = dayCal;
                inputText = removeIfParenthetical(m, inputText);
                //then put it into task
            }
        }

        String[] dates = {
                "(?i)(\\(|\\b)(jan(\\.|uary))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(feb(\\.|ruary))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(mar(\\.|ch))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(apr(\\.|il))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(may())(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(jun(\\.|e))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(jul(\\.|y))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(aug(\\.|ust))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(sep(\\.|tember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(oct(\\.|ober))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(nov(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)",
                "(?i)(\\(|\\b)(dec(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),?( (\\d{4}|\\d{2}))?(\\)|\\b)"
        };

        // m.group(2) = "month"
        //m.group(5) = "day"
        for (String date: dates) {
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);

            if (m.find()){
                Calendar dateCal = Chronic.parse(m.group(2)).getBeginCalendar();
                if (m.group(5) != null) {
                    dateCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(5)));
                }
                Calendar today = Calendar.getInstance();
                if (m.group(6) != null) {
                    dateCal.set(Calendar.YEAR, Integer.parseInt(m.group(6).trim()));
                } else if (today.get(Calendar.MONTH) - dateCal.get(Calendar.MONTH) > 1) { //if more than a month in the past
                    dateCal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR) + 1);
                }
                if (cal == null) {
                    cal = dateCal;
                } else{
                    cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.MONTH,dateCal.get(Calendar.MONTH) );
                    cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
                }
                inputText = removeIfParenthetical(m, inputText);
            }
        }

        // for dates in the format MM/DD
        Pattern p = Pattern.compile("(?i)(\\(|\\b)(1[0-2]|0?[1-9])(\\/|-)(3[0-1]|[0-2]?[0-9])(\\/|-)?(\\d{4}|\\d{2})?(\\)|\\b)");
        Matcher match = p.matcher(inputText);
        if (match.find()){
            Calendar dCal = Calendar.getInstance();
            setCalendarToDefaultTime(dCal);
            dCal.set(Calendar.MONTH, Integer.parseInt(match.group(2).trim()) - 1);
            dCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(match.group(4)));
            if (match.group(6) != null && !(match.group(6).trim()).equals("")) {
                String yearString = match.group(6);
                if(match.group(6).length() == 2)
                    yearString = "20" + match.group(6);
                dCal.set(Calendar.YEAR, Integer.parseInt(yearString));
            }

            if (cal == null) {
                cal = dCal;
            } else{
                cal.set(Calendar.DAY_OF_MONTH, dCal.get(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.MONTH,dCal.get(Calendar.MONTH));
                cal.set(Calendar.YEAR, dCal.get(Calendar.YEAR));
            }
            inputText = removeIfParenthetical(match, inputText);
        }

        HashMap<String, Integer> dayTimes = new HashMap<String, Integer>();
        dayTimes.put("(?i)\\bbreakfast\\b", 8);
        dayTimes.put("(?i)\\blunch\\b", 12);
        dayTimes.put("(?i)\\bsupper\\b", 18);
        dayTimes.put("(?i)\\bdinner\\b", 18);
        dayTimes.put("(?i)\\bbrunch\\b", 10);
        dayTimes.put("(?i)\\bmorning\\b", 8);
        dayTimes.put("(?i)\\bafternoon\\b", 15);
        dayTimes.put("(?i)\\bevening\\b", 19);
        dayTimes.put("(?i)\\bnight\\b", 19);
        dayTimes.put("(?i)\\bmidnight\\b", 0);
        dayTimes.put("(?i)\\bnoon\\b", 12);

        Set<String> keys = dayTimes.keySet();
        for (String dayTime : keys) {
            Pattern pattern = Pattern.compile(dayTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find()) {
                containsSpecificTime = true;
                int timeHour = dayTimes.get(dayTime);
                Calendar dayTimesCal = Calendar.getInstance();
                setCalendarToDefaultTime(dayTimesCal);
                dayTimesCal.set(Calendar.HOUR, timeHour);
                if (cal == null) {
                    cal = dayTimesCal;
                } else {
                    setCalendarToDefaultTime(cal);
                    cal.set(Calendar.HOUR, timeHour);
                }
            }
        }

        String[] times = {
                //[time] am/pm
                "(?i)(\\b)([01]?\\d):?([0-5]\\d)? ?([ap]\\.?m?\\.?)\\b",
                //army time
                "(?i)\\b(([0-2]?[0-9]):([0-5][0-9]))(\\b)",
                //[int] o'clock
                "(?i)\\b(([01]?\\d)() ?o'? ?clock) ?([ap]\\.?m\\.?)?\\b",
                //at [int]
                "(?i)(\\bat) ([01]?\\d)()($|\\D($|\\D))"

                //m.group(2) holds the hour
                //m.group(3) holds the minutes
                //m.group(4) holds am/pm
        };

        for (String time : times){
            Pattern pattern = Pattern.compile(time);
            Matcher m = pattern.matcher(inputText);
            if (m.find()) {
                containsSpecificTime = true;
                Calendar today = Calendar.getInstance();
                Calendar timeCal = Calendar.getInstance();
                setCalendarToDefaultTime(timeCal);
                timeCal.set(Calendar.HOUR, Integer.parseInt(m.group(2)));

                if (m.group(3) != null && !m.group(3).trim().equals(""))
                    timeCal.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
                else
                    timeCal.set(Calendar.MINUTE, 0);
                if (Integer.parseInt(m.group(2)) <= 12)
                    timeCal.set(Calendar.AM_PM, ampmToNumber(m.group(4)));

                //sets it to the next occurrence of that hour if no am/pm is provided. doesn't include military time
                if (Integer.parseInt(m.group(2))<= 12 && (m.group(4)==null || (m.group(4).trim()).equals(""))) {
                    while (timeCal.getTime().getTime() < today.getTime().getTime()){
                        timeCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY)+12);
                    }
                } else { //if am/pm is provided and the time is in the past, set it to the next day. Military time included.
                    if (timeCal.get(Calendar.HOUR) !=0 && (timeCal.getTime().getTime() < today.getTime().getTime())) {
                        timeCal.set(Calendar.DAY_OF_MONTH, timeCal.get(Calendar.DAY_OF_MONTH) + 1);
                    }
                    if (timeCal.get(Calendar.HOUR) == 0){
                        timeCal.set(Calendar.HOUR, 12);
                    }
                }

                if (cal == null){
                    cal = timeCal;
                } else {
                    cal.set(Calendar.HOUR, timeCal.get(Calendar.HOUR));
                    cal.set(Calendar.MINUTE,timeCal.get(Calendar.MINUTE) );
                    cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
                    cal.set(Calendar.AM_PM, timeCal.get(Calendar.AM_PM));
                }
                break;
            }
        }

        if(cal != null) { //if at least one of the above has been called, write to task. else do nothing.
            if (!TextUtils.isEmpty(inputText))
                task.setValue(Task.TITLE, inputText);
            if (containsSpecificTime) {
                task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, cal.getTime().getTime()));
            } else {
                task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, cal.getTime().getTime()));
            }
            return true;
        }
        return false;
    }
    //---------------------DATE--------------------------

    //Parses through the text and sets the frequency of the task.
    private static boolean repeatHelper(Task task) {
        if (task.containsNonNullValue(Task.RECURRENCE))
            return false;
        String inputText = task.getValue(Task.TITLE);
        HashMap<String, Frequency> repeatTimes = new HashMap<String, Frequency>();
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} days?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} ?nights?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} ?mornings?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} ?evenings?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} ?afternoons?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?weeks?\\b", Frequency.WEEKLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?(mon|tues|wednes|thurs|fri|satur|sun)days?\\b", Frequency.WEEKLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?months?\\b", Frequency.MONTHLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?years?\\b", Frequency.YEARLY);

        HashMap<String, Frequency> repeatTimesIntervalOne = new HashMap<String, Frequency>();
        //pre-determined intervals of 1
        repeatTimesIntervalOne.put( "(?i)\\bdaily\\b" , Frequency.DAILY);
        repeatTimesIntervalOne.put( "(?i)\\beveryday\\b" , Frequency.DAILY);
        repeatTimesIntervalOne.put( "(?i)\\bweekly\\b" , Frequency.WEEKLY);
        repeatTimesIntervalOne.put( "(?i)\\bmonthly\\b" ,Frequency.MONTHLY);
        repeatTimesIntervalOne.put( "(?i)\\byearly\\b" , Frequency.YEARLY);

        Set<String> keys = repeatTimes.keySet();
        for (String repeatTime : keys){
            Pattern pattern = Pattern.compile(repeatTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find()){
                Frequency rtime = repeatTimes.get(repeatTime);
                RRule rrule = new RRule();
                rrule.setFreq(rtime);
                rrule.setInterval(findInterval(inputText));
                task.setValue(Task.RECURRENCE, rrule.toIcal());
                return true;
            }
        }

        for (String repeatTimeIntervalOne:repeatTimesIntervalOne.keySet()){
            Pattern pattern = Pattern.compile(repeatTimeIntervalOne);
            Matcher m = pattern.matcher(inputText);
            if (m.find()) {
                Frequency rtime = repeatTimesIntervalOne.get(repeatTimeIntervalOne);
                RRule rrule = new RRule();
                rrule.setFreq(rtime);
                rrule.setInterval(1);
                String thing = rrule.toIcal();
                task.setValue(Task.RECURRENCE, thing);
                return true;
            }
        }
        return false;
    }

    //helper method for repeatHelper.
    private static int findInterval(String inputText) {
        HashMap<String,Integer> wordsToNum = new HashMap<String, Integer>();
        String[] words = new String[] {
                "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine", "ten", "eleven", "twelve"
        };
        for(int i = 0; i < words.length; i++) {
            wordsToNum.put(words[i], i+1);
            wordsToNum.put(Integer.toString(i + 1), i + 1);
        }
        wordsToNum.put("other" , 2);

        Pattern pattern = Pattern.compile("(?i)\\bevery (\\w*)\\b");
        int interval = 1;
        Matcher m = pattern.matcher(inputText);
        if (m.find() && m.group(1)!=null){
            String intervalStr = m.group(1);
            if (wordsToNum.containsKey(intervalStr))
                interval = wordsToNum.get(intervalStr);
            else {
                try {
                    interval = Integer.parseInt(intervalStr);
                } catch (NumberFormatException e) {
                    // Ah well
                }
            }
        }
        return interval;
    }

    //helper method for DayHelper. Resets the time on the calendar to 00:00:00 am
    private static void setCalendarToDefaultTime(Calendar cal){
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.AM_PM, Calendar.AM);
    }

}

