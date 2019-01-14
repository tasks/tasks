package org.tasks.notifications;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.tasks.Freeze.freezeAt;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.notifications.Throttle.Sleeper;

@RunWith(AndroidJUnit4.class)
public class ThrottleTest {

  private Sleeper sleeper;
  private Throttle throttle;

  @Before
  public void setUp() {
    sleeper = mock(Sleeper.class);
    throttle = new Throttle(3, sleeper);
  }

  @After
  public void tearDown() {
    verifyNoMoreInteractions(sleeper);
  }

  @Test
  public void dontThrottle() {
    long now = currentTimeMillis();

    runAt(now);
    runAt(now);
    runAt(now);
    runAt(now + 1000);
  }

  @Test
  public void throttleForOneMillisecond() {
    long now = currentTimeMillis();

    runAt(now);
    runAt(now);
    runAt(now);
    runAt(now + 999);

    verify(sleeper).sleep(1);
  }

  @Test
  public void throttleForOneSecond() {
    long now = currentTimeMillis();

    runAt(now);
    runAt(now);
    runAt(now);
    runAt(now);

    verify(sleeper).sleep(1000);
  }

  @Test
  public void throttleMultiple() {
    long now = currentTimeMillis();

    runAt(now);
    runAt(now + 200);
    runAt(now + 600);
    runAt(now + 700);

    verify(sleeper).sleep(300);

    runAt(now + 750);

    verify(sleeper).sleep(450);
  }

  private void runAt(long millis) {
    freezeAt(millis).thawAfter(() -> throttle.run(() -> {}));
  }
}
