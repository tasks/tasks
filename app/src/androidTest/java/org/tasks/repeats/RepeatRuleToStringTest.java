package org.tasks.repeats;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;
import com.google.ical.values.RRule;
import java.text.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.locale.Locale;

@RunWith(AndroidJUnit4.class)
public class RepeatRuleToStringTest {

  @Test
  public void weekly() {
    assertEquals("Repeats weekly", toString("RRULE:FREQ=WEEKLY;INTERVAL=1"));
  }

  @Test
  public void weeklyPlural() {
    assertEquals("Repeats every 2 weeks", toString("RRULE:FREQ=WEEKLY;INTERVAL=2"));
  }

  @Test
  public void weeklyByDay() {
    assertEquals(
        "Repeats weekly on Mon, Tue, Wed, Thu, Fri",
        toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR"));
  }

  @Test
  public void printDaysInRepeatRuleOrder() {
    assertEquals(
        "Repeats weekly on Fri, Thu, Wed, Tue, Mon",
        toString("RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=FR,TH,WE,TU,MO"));
  }

  @Test
  public void useLocaleForDays() {
    assertEquals(
        "Wiederhole wöchentlich am Sa., So.",
        toString("de", "RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=SA,SU"));
  }

  private String toString(String rrule) {
    return toString(null, rrule);
  }

  private String toString(String language, String rrule) {
    try {
      Locale locale = new Locale(java.util.Locale.getDefault(), language, -1);
      return new RepeatRuleToString(locale.createConfigurationContext(getTargetContext()), locale)
          .toString(new RRule(rrule));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
