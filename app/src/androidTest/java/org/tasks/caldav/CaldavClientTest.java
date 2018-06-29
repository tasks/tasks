package org.tasks.caldav;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaldavClientTest {
  @Test
  public void dontCrashOnSpaceInUrl() {
    new CaldavClient("https://example.com/remote.php/a space/", "username", "password");
  }
}
