package com.todoroo.astrid.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.mdimension.jchronic.Chronic;
import com.todoroo.astrid.data.Task;


public class TitleParser {
    Task task;
    ArrayList<String> tags;

    public TitleParser(Task task, ArrayList<String> tags){
        this.task = task;
        this.tags = tags;
        runAllHelpers();
    }

    private void runAllHelpers(){
        repeatHelper(task);
        dayHelper(task);
        listHelper(task,tags);
        priorityHelper(task);
    }

    private static void listHelper(Task task, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        Pattern tagPattern = Pattern.compile("(\\s|^)#([^\\s]+)");
        Pattern contextPattern = Pattern.compile("(\\s|^)(@[^\\s]+)");

        while(true) {
            Matcher m = tagPattern.matcher(inputText);
            if(m.find()) {
                tags.add(m.group(2));
            } else {
                m = contextPattern.matcher(inputText);
                if(m.find()) {
                    tags.add(m.group(2));
                }else{
                    break;
                }
            }
            inputText = inputText.substring(0, m.start()) + inputText.substring(m.end());
        }
        task.setValue(Task.TITLE, inputText.trim());
    }

    //helper method for priorityHelper. converts the string to a Task Importance
    private static int str_to_priority(String priority_str) {
        if (priority_str!=null)
            priority_str.toLowerCase().trim();
        int priority = Task.IMPORTANCE_DO_OR_DIE;
        if (priority_str.equals ("0") || priority_str.equals ("!0") || priority_str.equals("least") ||  priority_str.equals("lowest"))
            priority = Task.IMPORTANCE_NONE;
        if (priority_str.equals ("!") || priority_str.equals ("!1") || priority_str.equals("bang") || priority_str.equals("1") || priority_str.equals("low"))
            priority = Task.IMPORTANCE_SHOULD_DO;
        if (priority_str.equals("!!") || priority_str.equals ("!2") || priority_str.equals("bang bang") || priority_str.equals("2") || priority_str.equals("high"))
            priority = Task.IMPORTANCE_MUST_DO;
        return priority;
    }

    //priorityHelper parses the string and sets the Task's importance
    private static void priorityHelper(Task task) {
        String inputText = task.getValue(Task.TITLE);
        String[] importanceStrings = {
                "()((^|[^\\w!])!+|(^|[^\\w!])0|(^|[^\\w!])!\\d)($|[^\\w!])",
                "()(?i)((\\s?bang){1,})$",
                "(?i)(\\spriority\\s?(\\d)$)",
                "(?i)(\\sbang\\s?(\\d)$)",
                "(?i)()(\\shigh(est)?|\\slow(est)?|\\stop|\\sleast) ?priority$"
        };
        for (String importanceString:importanceStrings){
            Pattern importancePattern= Pattern.compile(importanceString);
            while (true){
                Matcher m = importancePattern.matcher(inputText);
                if(m.find()) {
                    task.setValue(Task.IMPORTANCE, str_to_priority(m.group(2).trim()));
                    inputText = inputText.substring(0, m.start()+1) + inputText.substring(m.end());

                } else
                    break;
            }
        }
        task.setValue(Task.TITLE, inputText.trim());
    }

    //helper for dayHelper. Converts am/pm to an int 0/1.
    private static int ampm_to_number(String am_pm_string) {
        int time = 0;
        if (am_pm_string == null){
            return time;
        }
        String text = am_pm_string.toLowerCase().trim();
        if (text.equals ("am") || text.equals ("a.m.") || text.equals("a"))
            time = Calendar.AM;
        if (text.equals ("pm") || text.equals ("p.m") || text.equals("p"))
            time = Calendar.PM;
        return time;
    }

    //---------------------DATE--------------------------
    //Handles setting the task's date.
    //Day of week (e.g. Monday, Tuesday,..) is overridden by a set date (e.g. October 23 2013).
    //Vague times (e.g. breakfast, night) are overridden by a set time (9 am, at 10, 17:00)
    private static void dayHelper(Task task ) {
        String inputText = task.getValue(Task.TITLE);
        Calendar cal = null;
        Boolean containsSpecificTime = false;
        String[] days_of_week = {
                "(?i)\\b(today)\\b",
                "(?i)\\b(tomorrow)\\b",
                "(?i)\\b(mon(day\\b|\\.))",
                "(?i)\\b(tues(day\\b|\\.))",
                "(?i)\\b(wed(nesday\\b|\\.))",
                "(?i)\\b(thu(rsday\\b|\\.))",
                "(?i)\\b(fri(day\\b|\\.))",
                "(?i)\\b(sat(urday\\b|\\.))",
                "(?i)\\b(sun(day\\b|\\.))",
                "(?i)\\b(next (month|week|year))\\b" };

        for (String date : days_of_week){
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);
            if (m.find()) {
                Calendar dayCal = Chronic.parse(m.group(0)).getBeginCalendar();
                cal= dayCal;
                //then put it into task
            }
        }

        String ds = "3[0-1]|[0-2]?[0-9]"; //day string for date regexes
        String[] dates = {
                "(?i)\\b(jan(\\.|uary))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(feb(\\.|ruary))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(mar(\\.|ch))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(apr(\\.|il))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(may)(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(jun(\\.|e))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(jul(\\.|y))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(aug(\\.|ust))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(sep(\\.|tember))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(oct(\\.|ober))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(nov(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?",
                "(?i)\\b(dec(\\.|ember))(\\s(3[0-1]|[0-2]?[0-9])),? (\\d{4}|\\d{2})?"};

        // m.group(1) = "month"
        //m.group(4) = "day"
        for (String date: dates) {
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);

            if (m.find()){
                Calendar dateCal = Chronic.parse(m.group(1)).getBeginCalendar();
                if (m.group(4)!= null){
                    dateCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(m.group(4)));
                }
                Calendar today = Calendar.getInstance();
                if (m.group(5)!=null){
                    dateCal.set(Calendar.YEAR, Integer.parseInt(m.group(5)));
                }
                else if (today.get(Calendar.MONTH) - dateCal.get(Calendar.MONTH) > 1){ //if more than a month in the past
                    dateCal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR)+1);
                }
                if (cal==null){
                    cal = dateCal;
                }
                else{
                    cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.MONTH,dateCal.get(Calendar.MONTH) );
                    cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
                }
            }
        }

        // for dates in the format MM/DD
        Pattern p = Pattern.compile("(?i)\\b(1[0-2]|[1-9])(\\/|-)(3[0-1]|[0-2]?[0-9])(\\/|-)?(\\d{4}|\\d{2})?");
        Matcher match = p.matcher(inputText);
        if (match.find()){
            Calendar dCal = Calendar.getInstance();
            setCalendarToDefaultTime(dCal);
            dCal.set(Calendar.MONTH, Integer.parseInt(match.group(1).trim())-1);
            dCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(match.group(3)));
            if (match.group(5) != null && !(match.group(5).trim()).equals(""))
            {
                String yearString = match.group(5);
                if(match.group(5).length()==2)
                    yearString = "20" + match.group(5);
                dCal.set(Calendar.YEAR, Integer.parseInt(yearString));
            }

            if (cal==null){
                cal = dCal;
            }
            else{
                cal.set(Calendar.DAY_OF_MONTH, dCal.get(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.MONTH,dCal.get(Calendar.MONTH));
                cal.set(Calendar.YEAR, dCal.get(Calendar.YEAR));
            }
        }

        HashMap<String, Integer> dayTimes = new HashMap();
        dayTimes.put("(?i)\\bbreakfast\\b", 8);
        dayTimes.put("(?i)\\blunch\\b", 12);
        dayTimes.put("(?i)\\bsupper\\b" ,18);
        dayTimes.put("(?i)\\bdinner\\b",18);
        dayTimes.put("(?i)\\bbrunch\\b", 10);
        dayTimes.put("(?i)\\bafternoon\\b", 15);
        dayTimes.put("(?i)\\bevening\\b" , 19);
        dayTimes.put("(?i)\\bnight\\b" , 19);
        dayTimes.put("(?i)\\bmidnight\\b" , 24);
        dayTimes.put("(?i)\\bnoon\\b" , 12);


        for (String dayTime: dayTimes.keySet()){
            Pattern pattern = Pattern.compile(dayTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find()){
                containsSpecificTime=true;
                int timeHour = dayTimes.get(dayTime);
                Calendar dayTimesCal = Calendar.getInstance();
                setCalendarToDefaultTime(dayTimesCal);
                dayTimesCal.set(Calendar.HOUR, timeHour);
                if (cal == null) {
                    cal = dayTimesCal;
                }
                else {
                    setCalendarToDefaultTime(cal);
                    cal.set(Calendar.HOUR, timeHour);
                }
            }
        }

        String[] times = {
                //army time
                "(?i)\\b(([0-2]?[0-9]):([0-5][0-9]))(\\b)",
                //[time] am/pm
                "(\\b)([01]?\\d)(:[0-5]\\d)? ?([ap]\\.?m\\.?)\\b",
                //[int] o'clock
                "(?i)\\b(([01]?\\d)() ?o'? ?clock) ?([ap]\\.?m\\.?)?\\b",
                //at [int]
                "(?i)(\\bat) ([01]?\\d)()($|\\D($|\\D))"

                //m.group(2) holds the hour
                //m.group(3) holds the minutes
                //m.group(4) holds am/pm
        };

        for (String time:times){
            Pattern pattern = Pattern.compile(time);
            Matcher m = pattern.matcher(inputText);
            if (m.find()){
                containsSpecificTime = true;
                Calendar timeCal = Calendar.getInstance();
                setCalendarToDefaultTime(timeCal);
                timeCal.set(Calendar.HOUR, Integer.parseInt(m.group(2)));
                if (m.group(3) != null && !m.group(3).trim().equals(""))
                    timeCal.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
                else
                    timeCal.set(Calendar.MINUTE, 0);
                System.err.println(m.group(4));
                System.err.println(ampm_to_number(m.group(4)));
                timeCal.set(Calendar.AM_PM, ampm_to_number(m.group(4)));

                if (cal == null){
                    cal = timeCal;
                }
                else{
                    cal.set(Calendar.HOUR, timeCal.get(Calendar.HOUR));
                    cal.set(Calendar.MINUTE,timeCal.get(Calendar.MINUTE) );
                    cal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
                    cal.set(Calendar.AM_PM, timeCal.get(Calendar.AM_PM));
                }

                break;
            }
        }

        if( cal!= null ){ //if at least one of the above has been called, write to task. else do nothing.
            if (containsSpecificTime) {
                Long dueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, cal.getTime().getTime());
                task.setValue(Task.DUE_DATE, dueDate);
            }
            else {
                Long dueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, cal.getTime().getTime());
                task.setValue(Task.DUE_DATE, dueDate);
            }
        }

    }
    //---------------------DATE--------------------------

    //Parses through the text and sets the frequency of the task.
    private static void repeatHelper(Task task) {
        String inputText = task.getValue(Task.TITLE);
        HashMap<String, Frequency> repeatTimes = new HashMap();
        repeatTimes.put("(?i)\\bevery ?\\w{0,6} ?days?\\b" , Frequency.DAILY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?weeks?\\b", Frequency.WEEKLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?(mon|tues|wednes|thurs|fri|satur|sun)days?\\b", Frequency.WEEKLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?months?\\b", Frequency.MONTHLY);
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?years?\\b", Frequency.YEARLY);

        HashMap<String, Frequency> repeatTimesIntervalOne = new HashMap();
        //pre-determined intervals of 1
        repeatTimesIntervalOne.put( "(?i)\\bdaily\\b" , Frequency.DAILY);
        repeatTimesIntervalOne.put( "(?i)\\bweekly\\b" , Frequency.WEEKLY);
        repeatTimesIntervalOne.put( "(?i)\\bmonthly\\b" ,Frequency.MONTHLY);
        repeatTimesIntervalOne.put( "(?i)\\byearly\\b" , Frequency.YEARLY);

        for (String repeatTime:repeatTimes.keySet()){
            Pattern pattern = Pattern.compile(repeatTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find()){
                Frequency rtime = repeatTimes.get(repeatTime);
                RRule rrule = new RRule();
                rrule.setFreq(rtime);
                rrule.setInterval(findInterval(inputText));
                task.setValue(Task.RECURRENCE, rrule.toIcal());
            }
        }

        for (String repeatTimeIntervalOne:repeatTimesIntervalOne.keySet()){
            Pattern pattern = Pattern.compile(repeatTimeIntervalOne);
            Matcher m = pattern.matcher(inputText);
            //if (m.find() && map.get("time")==null){
            if (m.find()) {
                Frequency rtime = repeatTimesIntervalOne.get(repeatTimeIntervalOne);
                RRule rrule = new RRule();
                rrule.setFreq(rtime);
                rrule.setInterval(1);
                String thing = rrule.toIcal();
                task.setValue(Task.RECURRENCE, thing);
            }
        }
    }

    //helper method for repeatHelper.
    private static int findInterval(String inputText) {
        HashMap<String,Integer> words_to_num = new HashMap<String, Integer>();
        String[] words = new String[] {
                "one", "two", "three", "four", "five", "six",
                "seven", "eight", "nine", "ten", "eleven", "twelve"
        };
        for(int i = 0; i < words.length; i++) {
            words_to_num.put(words[i], i+1);
            words_to_num.put(Integer.toString(i + 1), i + 1);
        }
        words_to_num.put("other" , 2);

        Pattern pattern = Pattern.compile("(?i)\\bevery (\\w*)\\b");
        int interval = 1;
        Matcher m = pattern.matcher(inputText);
        if (m.find() && m.group(1)!=null){
            String interval_str = m.group(1);
            if (words_to_num.containsKey(interval_str))
                interval = words_to_num.get(interval_str);
        }
        return interval;
    }

    //helper method for DayHelper. Resets the time on the calendar to 00:00:00 am
    private static void setCalendarToDefaultTime(Calendar cal){
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.AM_PM, Calendar.AM);
    }

}

