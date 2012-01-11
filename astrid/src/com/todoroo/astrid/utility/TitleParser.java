package com.todoroo.astrid.utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mdimension.jchronic.Chronic;
import com.todoroo.astrid.data.Task;


public class TitleParser {

    HashMap results;
    Task task;
    ArrayList<String> tags;

    public TitleParser(Task task, ArrayList<String> tags){
        this.task = task;
        this.tags = tags;
        runAllHelpers();
    }

    public Task getTask(){
        return task;
    }

    private void runAllHelpers(){
        priorityListHelper(task, results, tags);
        dayHelper(task, results, tags);
        dateHelper(task, results, tags);
        timeHelper(task, results, tags);
        repeatHelper(task, results, tags);
    }

    private static int str_to_priority(String priority_str) {
        int priority = Task.IMPORTANCE_DO_OR_DIE;
        if (priority_str.equals ("0") || priority_str.equals ("!0"))
            priority = Task.IMPORTANCE_NONE;
        if (priority_str.equals ("!") || priority_str.equals ("!1"))
            priority = Task.IMPORTANCE_SHOULD_DO;
        if (priority_str.equals("!!") || priority_str.equals ("!2"))
            priority = Task.IMPORTANCE_MUST_DO;
        return priority;
    }

    private static void priorityListHelper(Task task, HashMap map, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);

        Pattern importancePattern = Pattern.compile("(.*)(^|[^\\w!])(!+|0|!\\d)($|[^\\w!])(.*)");
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
                    m = importancePattern.matcher(inputText);
                    if(m.find()) {
                        map.put("title", m.group(1)); //make task list without !!

                        map.put("priority", str_to_priority(m.group(3)));
                        task.setValue(Task.IMPORTANCE, str_to_priority(m.group(3)));

                    } else
                        break;
                }
            }
            inputText = inputText.substring(0, m.start()) + inputText.substring(m.end());
        }
        task.setValue(Task.TITLE, inputText.trim());
    }

    private static void dayHelper(Task task, HashMap map, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        String[] dates = {
                "(?i)\\b(today)\\b",
                "(?i)\\b(tomorrow)\\b",
                "(?i)\\b(mon(day\\b|\\.))",
                "(?i)\\b(tues(day\\b|\\.))",
                "(?i)\\b(wed(nesday\\b|\\.))",
                "(?i)\\b(thurs(day\\b|\\.))",
                "(?i)\\b(fri(day\\b|\\.))",
                "(?i)\\b(sat(urday\\b|\\.))",
                "(?i)\\b(sun(day\\b|\\.))",
                "(?i)\\b(next (month|week|year))\\b" };

        for (String date : dates){
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);

            if (m.find()) {
                Calendar cal = Chronic.parse(m.group(0)).getBeginCalendar();
                map.put("date", cal);
                long time = cal.getTime().getTime();
                task.setValue(Task.DUE_DATE, time);

            }
        }
    }

    private static void dateHelper(Task task, HashMap map, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        String ds = "3[0-1]|[0-2]?[0-9]";
        String[] dates = {
                "\\b(1?[1-9](\\/|-)(3[0-1]|[0-2]?[0-9])($|\\s|\\/|-))",
                "\\b(jan(uary)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(feb(ruary)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(mar(ch)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(apr(il)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(may)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(june?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(july?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(aug(ust)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(sep(tember)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(oct(ober)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(nov(ember)?)(\\s(3[0-1]|[0-2]?[0-9]))?",
                "\\b(dec(ember)?)(\\s(3[0-1]|[0-2]?[0-9]))?"
        };
        for (String date:dates) {
            Pattern pattern = Pattern.compile(date);
            Matcher m = pattern.matcher(inputText);
            //if (m.find()) {
            if (m.find() && map.get("day")== null){
                Calendar cal = Chronic.parse(m.group(0)).getBeginCalendar();
                Calendar today = Calendar.getInstance();
                if (today.get(Calendar.MONTH) - cal.get(Calendar.MONTH) > 1){ //if more than a month in the past
                    cal.set(cal.get(Calendar.YEAR)+1, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                }
                map.put("day", cal);
            }
        }
    }
    private static void timeHelper(Task task, HashMap map, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        String[] times = {
                //[time] am/pm
                "(?i)\\b((i?)\\d.* ?[ap]\\.?m\\.?)\\b",
                //army time
                "(?i)\\b([0-2]?[0-9]:[0-5][0-9])\\b",
                //at [int]
                "(?i)\\bat ([01]?\\d)($|\\D($|\\D))",
                //[int] o'clock
                "(?i)\\b([01]?\\d ?o'? ?clock)\\b"
        };
        for (String time:times){
            Pattern pattern = Pattern.compile(time);
            Matcher m = pattern.matcher(inputText);
            if (m.find()){
                map.put("time", Chronic.parse(m.group(1)).getBeginCalendar());
            }
        }
        HashMap<String, String> dayTimes = new HashMap();
        dayTimes.put("(?i)\\bafternoon\\b", "15:00");
        dayTimes.put("(?i)\\bevening\b" , "19:00");
        dayTimes.put("(?i)\\bnight\\b" , "19:00");
        dayTimes.put("(?i)\\bmidnight\\b" , "0:00");
        dayTimes.put("(?i)\\bnoon\\b" , "12:00");

        for (String dayTime: dayTimes.keySet()){
            Pattern pattern = Pattern.compile(dayTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find() && map.get("time")==null){
                String dtime = dayTimes.get(dayTime);
                map.put("time", Chronic.parse(dtime).getBeginCalendar());

            }
        }

        HashMap<String,String> mealTimes = new HashMap();
        mealTimes.put("(?i)\\bbreakfast\\b", "8:00");
        mealTimes.put("(?i)\\blunch\\b", "12:00");
        mealTimes.put("(?i)\\bsupper\\b" ,"18:00");
        mealTimes.put("(?i)\\bdinner\b","18:00");
        mealTimes.put("(?i)\\bbrunch\b", "10:00");

        for (String mealTime:mealTimes.keySet()){
            Pattern pattern = Pattern.compile(mealTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find() && map.get("time")==null){
                String mtime = mealTimes.get(mealTime);
                map.put("time", Chronic.parse(mtime).getBeginCalendar());
            }
        }
    }


    private static void repeatHelper(Task task, HashMap map, ArrayList<String> tags) {
        String inputText = task.getValue(Task.TITLE);
        HashMap<String, String> repeatTimes = new HashMap();
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?days?\\b" , "day");
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?weeks?\\b", "week");
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?(mon|tues|wednes|thurs|fri|satur|sun)days?\b/i","week");
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?months?\\b", "month");
        repeatTimes.put("(?i)\\bevery \\w{0,6} ?years?\\b", "year");

        HashMap<String, String> repeatTimesIntervalOne = new HashMap();
        //pre-determined intervals of 1
        repeatTimesIntervalOne.put( "(?i)\\bdaily\\b" , "day");
        repeatTimesIntervalOne.put( "(?i)\\bweekly\\b" , "week");
        repeatTimesIntervalOne.put( "(?i)\\bmonthly\\b" ,"month");
        repeatTimesIntervalOne.put( "(?i)\\byearly\\b" , "year");

        for (String repeatTime:repeatTimes.keySet()){
            Pattern pattern = Pattern.compile(repeatTime);
            Matcher m = pattern.matcher(inputText);
            if (m.find() && map.get("time")==null){
                String rtime = repeatTimes.get(repeatTime);
                map.put("freq", rtime);
                map.put("interval",findInterval(inputText));
                map.put("repeats", true);
            }
        }

        for (String repeatTimeIntervalOne:repeatTimesIntervalOne.keySet()){
            Pattern pattern = Pattern.compile(repeatTimeIntervalOne);
            Matcher m = pattern.matcher(inputText);
            if (m.find() && map.get("time")==null){
                String rtime = repeatTimesIntervalOne.get(repeatTimeIntervalOne);
                map.put("freq", rtime);
                map.put("interval", 1);
                map.put("repeats", true);
            }
        }
    }

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

        Pattern pattern = Pattern.compile("\\bevery (\\w*)\\b");
        int interval = 1;
        Matcher m = pattern.matcher(inputText);
        if (m.find() && m.group(1)!=null){
            String interval_str = m.group(1);
            if (words_to_num.containsKey(interval_str))
                interval = words_to_num.get(interval_str);
        }
        return interval;
    }

}

